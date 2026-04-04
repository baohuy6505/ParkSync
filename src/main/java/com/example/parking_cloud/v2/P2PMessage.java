package com.example.parking_cloud.v2;

public class P2PMessage {
    private int vong;           // Pha giao dịch: 1, 2, 3, 4
    private String hanhDong;    // Hành động: SEND, TEMP, SYNC, UPD
    private String tenServer;   // Server đang xử lý thông điệp
    private int dongHo;         // Số đồng hồ logic
    private String noiDung;     // Nội dung giao dịch (VD: VAO|43A-99999)
    private String serverGoc;   // Server phát sinh giao dịch

    public P2PMessage() {}

    public P2PMessage(int vong, String hanhDong, String tenServer, int dongHo, String noiDung, String serverGoc) {
        this.vong = vong;
        this.hanhDong = hanhDong;
        this.tenServer = tenServer;
        this.dongHo = dongHo;
        this.noiDung = noiDung;
        this.serverGoc = serverGoc;
    }

    // ... (Thêm Getters và Setters cho các thuộc tính trên) ...
    public int getVong() { return vong; }
    public void setVong(int vong) { this.vong = vong; }
    public String getHanhDong() { return hanhDong; }
    public void setHanhDong(String hanhDong) { this.hanhDong = hanhDong; }
    public String getTenServer() { return tenServer; }
    public void setTenServer(String tenServer) { this.tenServer = tenServer; }
    public int getDongHo() { return dongHo; }
    public void setDongHo(int dongHo) { this.dongHo = dongHo; }
    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String noiDung) { this.noiDung = noiDung; }
    public String getServerGoc() { return serverGoc; }
    public void setServerGoc(String serverGoc) { this.serverGoc = serverGoc; }

    // Chuẩn hóa theo format cấu trúc thông điệp
    public String toFormatString() {
        return String.format("@$|%d|%s|%s|%d|%s|%s|$$", vong, hanhDong, tenServer, dongHo, noiDung, serverGoc);
    }
}