package com.example.parking_cloud;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ParkingReceiver {

    @Autowired
    private ParkingSlotRepository repository; 

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private RabbitTemplate rabbitTemplate; // Dùng để hét lên loa

    @Value("${server.gate.name:DEFAULT_GATE}")
    private String myGateName; // Tên cổng của máy này (VD: GATE_HUY)

    private int availableSpots = 50;  // Giả sử bãi có 50 chỗ trống ban đầu

    // ====================================================================
    // LUỒNG 1: TRANH VIỆC (Chỉ 1 máy giật được phiếu từ queue_xin_vao)
    // ====================================================================
    @RabbitListener(queues = RabbitMQConfig.WORK_QUEUE)
    public void processEntryRequest(String thongTinXe) {
        System.out.println(" [" + myGateName + "] DA LAY DUOC PHIEU CUA XE: " + thongTinXe);

        // Giả sử kiểm tra DB cục bộ thấy còn chỗ
        if (availableSpots > 0) {
            System.out.println(" [" + myGateName + "] BAI CON CHO. CHUAN BI THONG BAO CHO LOA!");
            
            // Đóng gói tin nhắn đồng bộ. Đóng dấu tên người duyệt vào (VD: BMW_THEM_VAO_BOI_GATE_HUY)
            String syncMessage = thongTinXe + "_THEM_VAO_BOI_" + myGateName;
            
            // HÉT LÊN LOA ĐỂ BÁO CHO 4 ANH EM CÒN LẠI!
            rabbitTemplate.convertAndSend(RabbitMQConfig.SYNC_EXCHANGE, "", syncMessage);
        } else {
            System.out.println(" [" + myGateName + "] HET CHO, TU CHOI XE!");
        }
    }

    // ====================================================================
    // LUỒNG 2: NGHE ĐỒNG BỘ (Cả 5 máy cùng nghe loa để update MySQL)
    // ====================================================================
    // Cú pháp #{syncQueue.name} giúp Spring tự động lấy đúng tên queue riêng đã tạo ở Config
   @RabbitListener(queues = "#{syncQueue.name}")
    public void syncDatabase(String syncMessage) {
        System.out.println(" 📢 [" + myGateName + "] NGHE TU LOA TONG: " + syncMessage);

        try {
            // 1. TẠO DÒNG MỚI HOÀN TOÀN
            ParkingSlot slot = new ParkingSlot(); 

            // Tách lấy biển số từ tin nhắn (VD: "43A123_THEM_VAO..." -> lấy "43A123")
            String bienSo = syncMessage.split("_")[0];
            slot.setSlotName("Xe: " + bienSo); 
            slot.setOccupied(true);

            // Lưu và ép ghi xuống MySQL
            repository.saveAndFlush(slot); 
            System.out.println(" DA LUU DONG MOI VAO MySQL cho xe: " + bienSo);

            // 2. Cập nhật số chỗ trong bộ nhớ máy
            if (availableSpots > 0) {
                availableSpots--;
            }

            // 3. Bắn lên Firebase
            firebaseService.updateSpotsOnWeb(availableSpots);
            System.out.println(" DA BAO CAO Firebase!");

        } catch (Exception e) {
            System.err.println(" LOI XU LI DONG BO: " + e.getMessage());
            e.printStackTrace(); 
        }
    }
}