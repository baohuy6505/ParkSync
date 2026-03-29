package com.example.parking_cloud;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ParkingReceiver {

    @Autowired
    private ParkingSlotRepository repository; // Để lưu vào MySQL

    @Autowired
    private FirebaseService firebaseService;
    private int availableSpots = 50; // Giả lập bãi xe có 50 chỗ ban đầu

    // Lắng nghe xem có tin nhắn nào mới ở "parking_queue" không
    @RabbitListener(queues = "parking_queue")
    public void receiveMessage(String message) {
        System.out.println("------------------------------------");
        System.out.println("📢 NHẬN TIN TỪ RABBITMQ: " + message);

        try {
            // 1. Cập nhật MySQL (Aiven)
            // Tìm chỗ số 1, nếu chưa có thì tạo mới
            ParkingSlot slot = repository.findById(1L).orElse(new ParkingSlot());
            slot.setSlotName("Slot 01");
            slot.setOccupied(true);
            repository.save(slot); // Lệnh này sẽ ghi xuống MySQL Cloud
            System.out.println("✅ Đã lưu trạng thái Slot 01 vào MySQL Aiven.");

            // 2. Tính toán lại chỗ trống
            if (availableSpots > 0) {
                availableSpots--;
            }

            // 3. Bắn lên Firebase Realtime Database
            firebaseService.updateSpotsOnWeb(availableSpots);
            System.out.println("🚀 Đã bắn số chỗ trống mới (" + availableSpots + ") lên Firebase!");

        } catch (Exception e) {
            System.err.println("❌ Lỗi xử lý: " + e.getMessage());
        }
        
        System.out.println("------------------------------------");
    }
}