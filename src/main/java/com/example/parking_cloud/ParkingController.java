package com.example.parking_cloud;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.parking_cloud.config.RabbitMQConfig;
import com.example.parking_cloud.model.ParkingLog;
import com.example.parking_cloud.model.ParkingLogRepository;
import java.util.List;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/parking")
@CrossOrigin(origins = "*")
public class ParkingController {

    @Value("${server.gate.name:DEFAULT_GATE}")
    private String gateName;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ParkingLogRepository parkingLogRepository;

    @GetMapping("/history")
    public List<ParkingLog> getHistoryLogs() {
        // Lấy toàn bộ Log từ MongoDB Atlas
        List<ParkingLog> logs = parkingLogRepository.findAll();
        // Đảo ngược danh sách để xe nào vừa quẹt thẻ sẽ hiện lên đầu bảng
        Collections.reverse(logs);
        // Trả về tối đa 50 dòng mới nhất cho Web đỡ lag
        return logs.stream().limit(50).toList();
    }

    // Link: http://localhost:8081/api/parking/dashboard
    @GetMapping("/dashboard")
    public void showDashboard(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        // Lệnh này bảo trình duyệt: "Này, đừng in chữ index.html nữa, hãy mở file /index.html ở thư mục static ra!"
        response.sendRedirect("/index.html");
    }

    // API dành cho máy quét thẻ (POST)
    @PostMapping("/quet-the")
    public String quetTheXe(@RequestParam String bienSo) {
        String thongTinXe = "VAO|" + bienSo;
        rabbitTemplate.convertAndSend(RabbitMQConfig.WORK_QUEUE, thongTinXe);
        System.out.println("DA GUI LEN HANG DOI: " + thongTinXe);
        return "Da gui yeu cau xin vao bai cho xe [" + bienSo + "] len RabbitMQ.";
    }

    // API Test trên trình duyệt (GET)
    // Link ĐÚNG: http://localhost:8081/api/parking/vao-bai?bienSo=43A-99999
    //   @GetMapping("/vao-bai")
    //     public String xeVaoBai(@RequestParam String bienSo) {
    //         String thongTinXe = "VAO|" + bienSo;

    //         // NÉM VÀO ĐÚNG HÀNG ĐỢI CỦA CỔNG MÌNH
    //         String myQueueName = "queue_xin_vao_" + gateName;
    //         rabbitTemplate.convertAndSend(myQueueName, thongTinXe);

    //         return "Đã đưa xe " + bienSo + " vào hàng đợi của " + gateName;
    //     }
@GetMapping("/vao-bai")
public String vaoBai(@RequestParam String bienSo) {
    String myQueue = "queue_vao_" + gateName;
    // PHẢI CÓ DẤU | Ở GIỮA
    String message = "VAO|" + bienSo; 
    
    rabbitTemplate.convertAndSend(myQueue, message);
    return "Gửi lệnh VÀO cho xe " + bienSo + " thành công!";
}
    // Link test: http://localhost:8081/api/parking/ra-bai?bienSo=43A-99999
    @GetMapping("/ra-bai")
    public String raBai(@RequestParam String bienSo) {
        String myQueue = "queue_vao_" + gateName; 
    
    // 2. Đóng gói lệnh RA (phải có dấu | )
    String message = "RA|" + bienSo; 
    
    // 3. Ném vào túi riêng
    rabbitTemplate.convertAndSend(myQueue, message);
    
    return "Cổng " + gateName + " đang xử lý cho xe [" + bienSo + "] RA bãi.";
    }
}