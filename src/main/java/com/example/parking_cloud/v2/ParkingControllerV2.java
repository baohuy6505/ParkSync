package com.example.parking_cloud.v2;

import com.example.parking_cloud.model.ParkingLog;
import com.example.parking_cloud.model.ParkingLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking/v2")
@CrossOrigin(origins = "*")
public class ParkingControllerV2 {
@Autowired
    private LamportClock lamportClock; 
    @Autowired
    private ParkingLogRepository parkingLogRepository;

    @Autowired
    private DistributedGateService p2pService;

    @GetMapping("/history")
    public List<ParkingLog> getHistoryLogs() {
        List<ParkingLog> logs = parkingLogRepository.findAll();
        Collections.reverse(logs);
        return logs.stream().limit(50).toList();
    }

    @GetMapping("/dashboard")
    public void showDashboard(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.sendRedirect("/ring-demo.html");
    }

    // Khởi tạo quy trình VÀO BÃI
    @GetMapping("/vao-bai")
    public String vaoBai(@RequestParam String bienSo) {
        p2pService.batDauGiaoDich("VAO", bienSo);
        return "Bắt đầu chu trình đồng thuận VÀO cho xe: " + bienSo;
    }

    // Khởi tạo quy trình RA BÃI
    @GetMapping("/ra-bai")
    public String raBai(@RequestParam String bienSo) {
        p2pService.batDauGiaoDich("RA", bienSo);
        return "Bắt đầu chu trình đồng thuận RA cho xe: " + bienSo;
    }

    // API nội bộ dùng để các Server trong vòng tròn liên lạc với nhau
    @PostMapping("/p2p-sync")
    public String nhanThongDiepP2P(@RequestBody P2PMessage msg) {
        new Thread(() -> {
            p2pService.xuLyThongDiep(msg);
        }).start();
        return "OK";
    }
@GetMapping("/lamport-clock")
    public int getClock() {
        return lamportClock.get();
    }

    // 2. Lấy danh sách xe đang đợi trong RAM (Bảng tạm)
    @GetMapping("/temp-queue")
    public List<P2PMessage> getTempQueue() {
        return p2pService.getHangDoiTam();
    }

    // 3. Lấy lịch sử các pha (SEND, TEMP, SYNC, UPD) của cổng này
    @GetMapping("/local-events")
    public List<Map<String, Object>> getLocalEvents() {
        return p2pService.getLocalEvents();
    }
    
}