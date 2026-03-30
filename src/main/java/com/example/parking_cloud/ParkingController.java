package com.example.parking_cloud;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parking")
public class ParkingController {

    // Đây là "Cánh tay" giúp bạn ném dữ liệu lên RabbitMQ
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/dashboard")
    public String showDashboard() {
        // Nó sẽ tự tìm file index.html trong thư mục static hoặc templates
        return "index";
    }
    
    // Giả lập API khi có xe quẹt thẻ ở cổng
    @PostMapping("/quet-the")
    public String quetTheXe(@RequestParam String bienSo) {

        // 1. Đóng gói thông báo (Thực tế bạn sẽ dùng JSON, ở đây mình dùng String cho dễ hiểu)
        String thongTinXe = "XE BIEN SO: " + bienSo;

        // 2. NÉM LÊN RABBITMQ! 
        // Lệnh này ném thẳng câu chữ kia vào cái khay chung "queue_xin_vao"
        rabbitTemplate.convertAndSend(RabbitMQConfig.WORK_QUEUE, thongTinXe);

        System.out.println("DA GUI LEN HANG DOI: " + thongTinXe);

        return "Da gui yeu cau xin vao bai cho xe  [" + bienSo + "] len RabbitMQ. " +
               "Hay kiem tra Terminal va Firebase de xem ket qua!";
    }


    // Link test: http://localhost:8080/vao-bai?bienSo=43A12345
    @GetMapping("/vao-bai")
    public String vaoBai(@RequestParam String bienSo) {
        
        // NÉM PHIẾU VÀO HÀNG ĐỢI CHUNG CỦA RABBITMQ
        // Luồng này sẽ kích hoạt ParkingReceiver tranh nhau xử lý
        rabbitTemplate.convertAndSend(RabbitMQConfig.WORK_QUEUE, bienSo);

        return "Da gui yeu cau cho xe  [" + bienSo + "] len RabbitMQ. " +
               "Hay kiem tra Terminal va Firebase de xem ket qua!";
    }
}