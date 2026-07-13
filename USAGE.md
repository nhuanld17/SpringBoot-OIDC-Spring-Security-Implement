# USAGE — Hướng dẫn tạo credential Google & chạy ứng dụng

Tài liệu này hướng dẫn từ đầu cách tạo project trên Google Cloud, bật Google Calendar API, tạo OAuth Client ID (credential), rồi đưa giá trị vào file `.env` để chạy ứng dụng.

> Ứng dụng sử dụng luồng **OIDC Đăng nhập kết hợp ủy quyền Calendar (Authorization Code Grant + PKCE + Refresh Token + Nonce)** tự tay triển khai để vừa xác thực danh tính người dùng vừa có quyền truy cập đọc Google Calendar.

---

## Bước 1 — Tạo project trên Google Cloud

1. Vào <https://console.cloud.google.com/>.
2. Trên thanh trên cùng, bấm dropdown chọn project → **New Project**.
3. Đặt tên (ví dụ `springboot-oidc-calendar-demo`) → **Create**.
4. Sau khi tạo xong, **chọn đúng project vừa tạo** (rất quan trọng — mọi bước sau phải nằm trong project này).

## Bước 2 — Bật Google Calendar API

1. Menu trái → **APIs & Services** → **Library** (hoặc mở trực tiếp <https://console.cloud.google.com/apis/library>).
2. Tìm **Google Calendar API** → bấm vào → **Enable**.

## Bước 3 — Cấu hình OAuth consent screen

1. Menu trái → **APIs & Services** → **OAuth consent screen** (hoặc giao diện mới: **Google Auth Platform** → **Branding** / **Audience**).
2. **User Type: External** → **Create**.
3. Điền các trường bắt buộc:
    - **App name**
    - **User support email**
    - **Developer contact information** (email của bạn)
    - → **Save and Continue**.
4. **Scopes**: có thể bỏ qua → **Save and Continue**
   *(App tự động xin quyền truy cập Google Calendar ở client side lúc chạy ứng dụng).*
5. **Test users** → **+ Add Users** → thêm **chính email Google bạn sẽ dùng để cấp quyền** → **Save**.

   > ⚠️ App đang ở chế độ **Testing**, nên **chỉ email nằm trong Test users** mới đăng nhập được.
   > Quên bước này sẽ bị lỗi `access_denied` / "app chưa được xác minh".

## Bước 4 — Tạo OAuth Client ID (credential)

1. Menu trái → **APIs & Services** → **Credentials**.
2. **+ Create Credentials** → **OAuth client ID**.
3. **Application type: Web application**.
4. **Name**: tùy ý (ví dụ `springboot-oidc-local`).
5. **Authorized redirect URIs** → **+ Add URI** → dán **chính xác**:

   ```
   http://localhost:8080/oidc/callback
   ```

   > ⚠️ Đây là Redirect URI thủ công đã được đăng ký và cấu hình trong mã nguồn.
   > Sai một ký tự sẽ bị lỗi `redirect_uri_mismatch`.
6. **Create** → popup hiện **Client ID** và **Client secret**.
   **Copy cả hai** (hoặc tải file JSON credentials về).

---

## Bước 5 — Tạo file `.env` và điền credential

Ứng dụng đọc credential từ các biến môi trường cài đặt ở file `.env`.

1. Ở **thư mục gốc của project** (cùng cấp với `pom.xml`), tạo file tên `.env`.
2. Dán nội dung sau và thay bằng giá trị lấy ở Bước 4:

   ```properties
   GOOGLE_CLIENT_ID=xxxxxxxxxxxx.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=xxxxxxxxxxxxxxxx
   ```

> 🔒 File `.env` đã được cấu hình loại trừ trong `.gitignore` để đảm bảo bảo mật.

---

## Bước 6 — Chạy ứng dụng

Yêu cầu **JDK 17** trở lên.

```bash
./mvnw spring-boot:run
```

Sau khi app khởi động:

1. Mở trình duyệt: <http://localhost:8080/>.
2. Bấm nút **Login with Google**.
3. Đăng nhập bằng email trong danh sách **Test users** và đồng ý cấp quyền.
4. Sau khi bắt tay thành công, trang `/connected` hiển thị bao gồm:
   - Đã nhận được ID Token, Access Token và Refresh Token.
   - Thời hạn hết hạn và các Scope được cấp.
5. Click **Xem hồ sơ cá nhân (OIDC)** để xem thông tin Avatar, Tên, Email, sub của tài khoản (`/profile`).
6. Click **Mô phỏng Refresh Token** để chạy luồng thủ công sinh Access Token mới bằng Refresh Token (`/oidc/refresh`).
7. Click **Xem lịch của tôi** để xem danh sách lịch và các sự kiện sắp tới (`/calendars` → `/events`).

---

## Xử lý lỗi thường gặp

| Lỗi | Nguyên nhân & cách sửa |
|-----|------------------------|
| `redirect_uri_mismatch` | Redirect URI trên Google Console phải khớp chính xác với `http://localhost:8080/oidc/callback`. |
| `access_denied` / "app chưa được xác minh" | Email đăng nhập chưa được liệt kê trong danh sách **Test users** ở bước cấu hình OAuth Consent Screen. |
| App báo thiếu `GOOGLE_CLIENT_ID` khi chạy | File `.env` bị đặt sai vị trí hoặc sai định dạng. |
| `403` khi gọi Calendar API | Chưa bật **Google Calendar API** tại thư viện dự án Google Console hoặc scopes xin cấp quyền bị từ chối. |
| `invalid_grant` khi refresh | Refresh token bị thu hồi hoặc hết hiệu lực (xóa cookie / đăng xuất để bắt đầu lại). |
