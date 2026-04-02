package com.example.parking_cloud;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

//import com.example.parking_cloud.config.FirebaseService;
import com.example.parking_cloud.config.RabbitMQConfig;
import com.example.parking_cloud.model.ParkingLog;
import com.example.parking_cloud.model.ParkingLogRepository;
import com.example.parking_cloud.model.ParkingSlot;
import com.example.parking_cloud.model.ParkingSlotRepository;

@Component
public class ParkingReceiver {

    @Autowired
    private ParkingSlotRepository repository; // Dùng để lưu trạng thái chỗ đậu xe TRÊN MySQL
    @Autowired
    private ParkingLogRepository parkingLogRepository; // Dùng để lưu nhật ký TRÊN MONGODB ATLAS

    // @Autowired
    // private FirebaseService firebaseService;

    @Autowired
    private RabbitTemplate rabbitTemplate; // Dùng để hét lên loa

    @Value("${server.gate.name:DEFAULT_GATE}")
    private String myGateName; // Tên cổng của máy này (VD: GATE_HUY)

    private int availableSpots = 100;  // Giả sử bãi có 100 chỗ trống ban đầu
    // ====================================================================
    // LUỒNG 1: TRANH VIỆC (Chỉ 1 máy giật được phiếu từ queue_xin_vao)
    // ====================================================================
    @RabbitListener(queues = "queue_vao_${server.gate.name}")
    public void processEntryRequest(String thongTinXe) {
        System.out.println("[" + myGateName + "] DA CHOP DUOC PHIEU CUA XE: " + thongTinXe);

        // Tách chuỗi để lấy hành động (VAO/RA)
        String[] parts = thongTinXe.split("\\|");
        
        // BẢO VỆ 1: Lọc tin nhắn rác hoặc định dạng sai
        if (parts.length < 2) {
            System.err.println("BO QUA TIN NHAN RAC: " + thongTinXe);
            return;
        }

        String action = parts[0];

        // Nếu xe muốn VÀO nhưng bãi đã hết chỗ -> Từ chối ngay
        if (action.equals("VAO") && availableSpots <= 0) {
            System.out.println("[" + myGateName + "] BAI DA DAY, TU CHOI XE VAO!");
            return;
        }

        // Đóng gói tin nhắn đồng bộ. Nối thêm tên Cổng bằng dấu "|"
        // Kết quả sẽ có dạng: "VAO|43A123|GATE_HUY" hoặc "RA|43A123|GATE_HUY"
        String syncMessage = thongTinXe + "|" + myGateName;
        
        // HÉT LÊN LOA ĐỂ BÁO CHO 4 ANH EM CÒN LẠI!
        System.out.println("[" + myGateName + "] HOP LE! BAN LEN LOA DONG BO: " + syncMessage);
        rabbitTemplate.convertAndSend(RabbitMQConfig.SYNC_EXCHANGE, "", syncMessage);
    }

    @RabbitListener(queues = "#{syncQueue.name}")
    public void syncDatabase(String syncMessage) {
        System.out.println("[" + myGateName + "] NGHE TU LOA TONG: " + syncMessage);
        try {
            String[] parts = syncMessage.split("\\|");
            if (parts.length < 3) return;

            String action = parts[0];
            String bienSoXe = parts[1];
            String nguoiDuyet = parts[2];

            ParkingSlot xeTrongBai = repository.findFirstBySlotNameContaining(bienSoXe);

            if (action.equals("VAO")) {
                if (xeTrongBai != null) return;
                
                ParkingSlot slotMoi = new ParkingSlot(); 
                slotMoi.setSlotName("XE CUA: " + bienSoXe); 
                slotMoi.setOccupied(true);
                repository.saveAndFlush(slotMoi);

                availableSpots--; // Cập nhật biến local để máy mình biết

                // CHỈ MÁY CHỦ DUYỆT MỚI GỌI FIREBASE ĐỂ TRỪ 1
                // if (nguoiDuyet.equals(myGateName)) {
                //     firebaseService.updateSpotsOnWeb(-1); // TRUYỀN -1 LÀ ĐÚNG
                // }

            } else if (action.equals("RA")) {
                if (xeTrongBai == null) return;
                
                repository.delete(xeTrongBai);
                repository.flush();

                availableSpots++; // Cập nhật biến local

                // if (nguoiDuyet.equals(myGateName)) {
                //     firebaseService.updateSpotsOnWeb(1); // TRUYỀN 1 LÀ ĐÚNG
                // }
            }

            ParkingLog logEntry = new ParkingLog();
            logEntry.setBienSo(bienSoXe);
            logEntry.setHanhDong(action);
            logEntry.setCongXuly(nguoiDuyet); 
            logEntry.setThoiGian(new java.util.Date().toString()); 
            parkingLogRepository.save(logEntry);

        } catch (Exception e) {
            System.err.println("LOI XU LY DONG BO: " + e.getMessage());
        }
    }
}