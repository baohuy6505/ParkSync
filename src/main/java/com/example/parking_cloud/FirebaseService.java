package com.example.parking_cloud;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;

@Service
public class FirebaseService {

    private DatabaseReference dbReference;

    @PostConstruct
    public void initialize() {
        try {
            // 1. Huy nhớ để file firebase-key.json ở thư mục gốc của project nhé!
            FileInputStream serviceAccount = new FileInputStream("firebase-key.json");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    // 2. Thay link này bằng Database URL trong Firebase Console của Huy
                    .setDatabaseUrl("https://TEN-DU-AN-CUA-HUY.firebaseio.com") 
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            
            dbReference = FirebaseDatabase.getInstance().getReference("parking_status");
            System.out.println("🔥 Firebase đã sẵn sàng kết nối Cloud!");
        } catch (Exception e) {
            System.err.println("❌ Lỗi cấu hình Firebase: " + e.getMessage());
        }
    }

    // Hàm để Coordinator gọi bắn số chỗ trống
    public void updateSpotsOnWeb(int availableSpots) {
        if (dbReference != null) {
            dbReference.child("available_spots").setValueAsync(availableSpots);
            System.out.println("🚀 Cloud Firebase: Đã cập nhật còn " + availableSpots + " chỗ.");
        }
    }
}