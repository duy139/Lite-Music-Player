# Lite Music Player - Đồ án của Nhóm 12 🎵
Ứng dụng nghe nhạc nhẹ nhàng cho Android, phát triển như một dự án học tập của Nhóm 12.

![GitHub repo size](https://img.shields.io/github/repo-size/duy139/Lite-Music-Player)
![License](https://img.shields.io/badge/license-MIT-green)

## Giới thiệu
Lite Music Player là đồ án học tập của Nhóm 12, nhằm mục đích xây dựng một ứng dụng nghe nhạc đa tính năng trên Android. Dự án tích hợp các công nghệ hiện đại như Firebase và Google Cloud, đồng thời cung cấp trải nghiệm âm thanh và giao diện tối ưu cho người dùng.


## Tính năng chính ✨
| Tính năng                  | Mô tả                                      |
|----------------------------|--------------------------------------------|
| **Giao diện**              | Cử chỉ vuốt mượt mà, tối ưu dọc/ngang, sử dụng chủ đề Material You |
| **Firebase Authentication**| Đăng nhập/quản lý người dùng qua Firebase |
| **Realtime Database**      | Đồng bộ dữ liệu playlist theo thời gian thực |
| **Google Cloud**           | Sao lưu và khôi phục dữ liệu nhạc qua Google Drive |
| **Chỉnh sửa Metadata**     | Cập nhật thông tin bài hát (tên, nghệ sĩ, album, ảnh bìa) |
| **Thêm bài hát mới**       | Thêm thông tin bài hát (tên, nghệ sĩ, album, chọn file, ảnh bìa) |
| **Thêm Playlist**          | Thêm tên, ảnh bìa và ghi chú cho danh sách phát |
| **Yêu thích bài hát**      | Lưu những bài hát mà mình yêu thích nhất |
| **Phát nhạc**              | Hỗ trợ danh sách bài hát, phát ngẫu nhiên, lặp lại, hẹn giờ, đổi màu nền theo ảnh bìa |
| **Bộ chỉnh âm**            | Tùy chỉnh Bass, Mid, Treble, âm lượng, âm vang. Có các mẫu âm thanh sẵn như: Flat, Pop, Rock,... |
| **Tìm kiếm**               | Tìm kiếm nhạc và album |
| **Chế độ tối**             | Làm dịu mắt hơn vào ban đêm |


## Công nghệ sử dụng 🛠️
- **Ngôn ngữ**: Java
- **Nền tảng**: Android SDK
- **Firebase**: Authentication, Realtime Database
- **Google Cloud**: Google Drive API
- **Thư viện**: Jaudiotagger cho chỉnh sửa metadata

## Hướng dẫn cài đặt 📲
Để chạy Lite Music Player - Đồ án của Nhóm 12 trên máy của bạn, làm theo các bước sau:

1. **Chuẩn bị môi trường**:
   - Cài đặt [Android Studio](https://developer.android.com/studio) (phiên bản mới nhất khuyến nghị).
   - Đảm bảo máy tính có JDK 11 hoặc cao hơn.

2. **Clone repository**:
   ```bash
   git clone https://github.com/duy139/Lite-Music-Player.git

3. **Mở terminal hoặc PowerShell, chạy lệnh trên để tải source code về máy**:
- Mở project:
 - Khởi động Android Studio.
 - Chọn Open an existing project, trỏ tới thư mục Lite-Music-Player vừa clone.
 
- Cấu hình và build & run:
 - Chờ Android Studio sync Gradle (tự động tải dependencies).
 - Nếu có lỗi Gradle, vào File > Sync Project with Gradle Files.
 - Chạy ứng dụng (Yêu cầu tối thiểu API 31+)
 - Vì đây là ứng dụng nghe nhạc offline, vui lòng cấp quyền bộ nhớ để ứng dụng hoạt động bình thường.
 - Kết nối Firebase riêng nếu bạn muốn dùng tính năng đăng nhập và sync playlist riêng.
 - Kết nối Google Cloud riêng nếu bạn muốn dùng tính năng sao lưu và khôi phục riêng.

Dưới đây là giao diện của ứng dụng:

<img src="https://github.com/duy139/Lite-Music-Player/raw/main/screenshots/main_screen.jpg" width="150"/> <img src="https://github.com/duy139/Lite-Music-Player/raw/main/screenshots/album_screen.jpg" width="150"/> <img src="https://github.com/duy139/Lite-Music-Player/raw/main/screenshots/equalizer_screen.jpg" width="150"/> <img src="https://github.com/duy139/Lite-Music-Player/raw/main/screenshots/favorite_screen.jpg" width="150"/> <img src="https://github.com/duy139/Lite-Music-Player/raw/main/screenshots/playlist_screen.jpg" width="150"/> <img src="https://github.com/duy139/Lite-Music-Player/raw/main/screenshots/search_screen.jpg" width="150"/> 
<img src="https://github.com/duy139/Lite-Music-Player/raw/main/screenshots/setting_screen.jpg" width="150"/> <img src="https://github.com/duy139/Lite-Music-Player/raw/main/screenshots/tab_drawer_screen.jpg" width="150"/> <img src="https://github.com/duy139/Lite-Music-Player/raw/main/screenshots/player_screen1.jpg" width="150"/> <img src="https://github.com/duy139/Lite-Music-Player/raw/main/screenshots/player_screen2.jpg" width="150"/> 
<img src="https://github.com/duy139/Lite-Music-Player/raw/main/screenshots/player_screen3.jpg" width="150"/> <img src="https://github.com/duy139/Lite-Music-Player/raw/main/screenshots/main_screen_dark.jpg" width="150"/> <img src="https://github.com/duy139/Lite-Music-Player/raw/main/screenshots/miniplayer.jpg" width="150"/> 

Một số hình ảnh bìa bài hát trong các ảnh chụp màn hình trên được lấy từ các bài hát công khai trên mạng để minh họa. Những tài sản này không thuộc quyền sở hữu của Nhóm 12 và không nhằm mục đích thương mại. Mọi quyền liên quan thuộc về tác giả gốc hoặc chủ sở hữu bản quyền tương ứng. Nếu có vấn đề, xin hãy liên hệ đại diện nhóm.





## Bản quyền 📜
Lite Music Player - Đồ án Nhóm 12 được phát hành dưới **Giấy phép MIT** - tự do sử dụng, sao chép, chỉnh sửa và phân phối cho mục đích học tập hoặc thương mại. Liên hệ đại diện Nhóm 12 để trao đổi thêm nếu bạn có thắc mắc

Xem chi tiết giấy phép:
- [LICENSE (Tiếng Anh)](https://github.com/duy139/Lite-Music-Player/blob/main/LICENSE.md)
- [LICENSE_VN (Tiếng Việt)](https://github.com/duy139/Lite-Music-Player/blob/main/LICENSE_VN.md)

## Liên hệ
- **Github**: [duy139](https://github.com/duy139) (Đại diện nhóm 12)

---
**Lưu ý**: Đây là đồ án học tập của Nhóm 12, được phát triển với mục đích thực hành qua môn. Dự án có sự hỗ trợ từ trí tuệ nhân tạo (AI) trong quá trình phát triển để tối ưu hóa ý tưởng và triển khai tính năng. Tuy nhiên, mã nguồn có thể không được tối ưu hóa hoàn toàn và hiệu suất ứng dụng có thể không ổn định trong một số trường hợp. Mọi đóng góp hoặc ý kiến xây dựng xin vui lòng gửi về đại diện nhóm.


