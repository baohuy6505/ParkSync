package com.example.parking_cloud.v2;

import com.example.parking_cloud.model.ParkingLog;
import com.example.parking_cloud.model.ParkingLogRepository;
import com.example.parking_cloud.model.ParkingSlot;
import com.example.parking_cloud.model.ParkingSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class DistributedGateService {

    @Autowired
    private LamportClock lamportClock;

    @Autowired
    private ParkingSlotRepository slotRepository;

    @Autowired
    private ParkingLogRepository logRepository;

    @Value("${server.gate.name:DEFAULT_GATE}")
    private String myGateName;

    @Value("${my.public.url:http://localhost:8080}")
    private String myPublicUrl;

    // Bản đồ vòng tròn ảo khai báo trong application.properties
    @Value("${gate.ring.urls:http://localhost:8080,http://localhost:8081}")
    private String ringUrlsConfig;

    private List<String> ringUrls;
    private RestTemplate restTemplate = new RestTemplate();
    
    private List<Map<String, Object>> localEvents = Collections.synchronizedList(new ArrayList<>());

    // THÊM: Getter cho Controller gọi
    public List<P2PMessage> getHangDoiTam() { return hangDoiTam; }

    public List<Map<String, Object>> getLocalEvents() {
        return localEvents;
    }
    
    // Hàng đợi động 2: Cập nhật bảng tạm
    private List<P2PMessage> hangDoiTam = Collections.synchronizedList(new ArrayList<>());
    
    private int availableSpots = 100;

    @PostConstruct
    public void init() {
        ringUrls = Arrays.asList(ringUrlsConfig.split(","));
        ringUrls.replaceAll(url -> url.trim().replaceAll("/+$", ""));
        myPublicUrl = myPublicUrl.trim().replaceAll("/+$", "");
    }

    // =======================================================
    // BƯỚC 1: KHỞI TẠO GIAO DỊCH KHI QUẸT THẺ
    // =======================================================
    public void batDauGiaoDich(String loaiGiaoDich, String bienSo) {
        if (loaiGiaoDich.equals("VAO") && availableSpots <= 0) {
            System.out.println("[" + myGateName + "] BAI DA DAY, KHONG THE NHAN THEM XE VAO: " + bienSo);
            return;
        }

        String noiDungGiaoDich = loaiGiaoDich + "|" + bienSo;
        int currentClock = lamportClock.tick(); 
        
        P2PMessage msg = new P2PMessage(1, "SEND", myGateName, currentClock, noiDungGiaoDich, myGateName);
        System.out.println("KHOI TAO GIAO DICH: " + msg.toFormatString());
        
        // THÊM DÒNG NÀY: Tự thực hiện Pha 1 (Khóa dữ liệu) cho chính máy mình trước
        thucHienHanhDongCucBo(msg); 
        
        chuyenThongDiepDi(msg);
    }

    // =======================================================
    // BƯỚC 2: XỬ LÝ THÔNG ĐIỆP ĐẾN (Từ vòng tròn ảo)
    // =======================================================
    public void xuLyThongDiep(P2PMessage msg) {
        lamportClock.update(msg.getDongHo());
        msg.setTenServer(myGateName);

        if (msg.getServerGoc().equals(myGateName)) {
            // Thông điệp đã chạy đủ 1 vòng, tiến hành thăng cấp pha giao dịch
            thangCapVongGiaoDich(msg);
        } else {
            // Thực hiện lệnh trên máy cục bộ và truyền tiếp
            thucHienHanhDongCucBo(msg);
            chuyenThongDiepDi(msg);
        }
    }

    // =======================================================
    // QUẢN LÝ 4 PHA GIAO DỊCH
    // =======================================================
    private void thangCapVongGiaoDich(P2PMessage msg) {
        int vongHienTai = msg.getVong();
        msg.setDongHo(lamportClock.tick()); // Tăng đồng hồ trước mỗi pha mới

        if (vongHienTai == 1) {
            msg.setVong(2); msg.setHanhDong("TEMP");
            thucHienHanhDongCucBo(msg); // THÊM: Tự lưu vào Bảng tạm của mình
            chuyenThongDiepDi(msg);
        } else if (vongHienTai == 2) {
            msg.setVong(3); msg.setHanhDong("SYNC");
            thucHienHanhDongCucBo(msg); // THÊM: Tự sắp xếp Bảng tạm của mình
            chuyenThongDiepDi(msg);
        } else if (vongHienTai == 3) {
            msg.setVong(4); msg.setHanhDong("UPD");
            thucHienHanhDongCucBo(msg); // THÊM: Cập nhật CSDL của mình
            chuyenThongDiepDi(msg);
        } else if (vongHienTai == 4) {
            // Đã xong 4 vòng, CHỈ thực hiện cục bộ, KHÔNG truyền đi nữa
            thucHienHanhDongCucBo(msg); 
        }
    }
private void recordEvent(P2PMessage msg) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", System.currentTimeMillis());
        event.put("vong", msg.getVong());
        event.put("hanhDong", msg.getHanhDong());
        event.put("bienSo", msg.getNoiDung().contains("|") ? msg.getNoiDung().split("\\|")[1] : "N/A");
        event.put("lamportClock", msg.getDongHo());
        event.put("noiDung", msg.getNoiDung());
        localEvents.add(event);
        // Giới hạn 50 sự kiện gần nhất cho nhẹ máy
        if (localEvents.size() > 50) localEvents.remove(0);
    }
   private void thucHienHanhDongCucBo(P2PMessage msg) {
        recordEvent(msg); // GHI LẠI SỰ KIỆN VÀO DANH SÁCH CHO GIAO DIỆN
        switch (msg.getVong()) {
            case 1:
                System.out.println("    [Pha 1] KHOA TRUONG DU LIEU: " + msg.getNoiDung());
                break;
            case 2:
                System.out.println("    [Pha 2] CAP NHAT BANG TAM");
                hangDoiTam.add(msg);
                break;
            case 3:
                System.out.println("    [Pha 3] SAP XEP TRAN BANG TAM");
                hangDoiTam.sort((m1, m2) -> Integer.compare(m1.getDongHo(), m2.getDongHo()));
                break;
            case 4:
                System.out.println("    [Pha 4] THUC HIEN CAP NHAT CSDL");
                luuVaoDatabaseChinh(msg);
                break;
        }
    }

    private synchronized void luuVaoDatabaseChinh(P2PMessage msg) {
        // Gỡ khỏi bảng tạm
        hangDoiTam.removeIf(m -> m.getNoiDung().equals(msg.getNoiDung()));

        String[] parts = msg.getNoiDung().split("\\|");
        String action = parts[0];
        String bienSoXe = parts[1];

        try {
            ParkingSlot xeTrongBai = slotRepository.findFirstBySlotNameContaining(bienSoXe);

            if (action.equals("VAO") && xeTrongBai == null) {
                ParkingSlot slotMoi = new ParkingSlot(); 
                slotMoi.setSlotName("XE CUA: " + bienSoXe); 
                slotMoi.setOccupied(true);
                slotRepository.saveAndFlush(slotMoi);
                availableSpots--; 

                ParkingLog logEntry = new ParkingLog();
                logEntry.setBienSo(bienSoXe);
                logEntry.setHanhDong("VAO");
                logEntry.setCongXuly(msg.getServerGoc()); 
                logEntry.setThoiGian(new java.util.Date().toString()); 
                logRepository.save(logEntry);
                
                System.out.println("   DA CAP NHAT XE VAO CSDL THÀNH CÔNG: " + bienSoXe);
            } 
            else if (action.equals("RA") && xeTrongBai != null) {
                slotRepository.delete(xeTrongBai);
                slotRepository.flush();
                availableSpots++; 

                ParkingLog logEntry = new ParkingLog();
                logEntry.setBienSo(bienSoXe);
                logEntry.setHanhDong("RA");
                logEntry.setCongXuly(msg.getServerGoc()); 
                logEntry.setThoiGian(new java.util.Date().toString()); 
                logRepository.save(logEntry);
                
                System.out.println("  DA CAP NHAT XE RA CSDL THÀNH CÔNG: " + bienSoXe);
            }
        } catch (Exception e) {
            System.err.println(" LOI CSDL: " + e.getMessage());
        }
    }

    // =======================================================
    // TỰ ĐỘNG CHUYỂN LỖI (FAILOVER)
    // =======================================================
    private void chuyenThongDiepDi(P2PMessage msg) {
        int myIndex = ringUrls.indexOf(myPublicUrl);
        if (myIndex == -1) myIndex = 0; 

        boolean guiThanhCong = false;
        int nextIndex = (myIndex + 1) % ringUrls.size();
        int soLanThu = 0;

        while (!guiThanhCong && soLanThu < ringUrls.size() - 1) {
            String targetUrl = ringUrls.get(nextIndex) + "/api/parking/v2/p2p-sync";
            try {
                restTemplate.postForEntity(targetUrl, msg, String.class);
                guiThanhCong = true;
            } catch (Exception e) {
                System.err.println(" NHAY COC QUA SERVER SAP: " + targetUrl);
                nextIndex = (nextIndex + 1) % ringUrls.size();
                soLanThu++;
            }
        }
    }
}