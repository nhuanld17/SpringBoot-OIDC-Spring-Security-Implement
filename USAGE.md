# USAGE — Hướng dẫn tạo credential Google & chạy ứng dụng

Tài liệu này hướng dẫn từ đầu cách tạo project trên Google Cloud, bật Google Calendar API,
tạo OAuth Client ID (credential), rồi đưa giá trị vào file `.env` để chạy ứng dụng.

> Ứng dụng dùng **Authorization Code Grant + PKCE + Refresh Token** (Spring Security OAuth2 Client)
> để được **ủy quyền** truy cập Google Calendar của người dùng — không phải đăng nhập/OIDC.

---

## Bước 1 — Tạo project trên Google Cloud

1. Vào <https://console.cloud.google.com/>.
2. Trên thanh trên cùng, bấm dropdown chọn project → **New Project**.
3. Đặt tên (ví dụ `oauth2-calendar-demo`) → **Create**.
4. Sau khi tạo xong, **chọn đúng project vừa tạo** (rất quan trọng — mọi bước sau phải nằm trong project này).

## Bước 2 — Bật Google Calendar API

1. Menu trái → **APIs & Services** → **Library**
   (hoặc mở trực tiếp <https://console.cloud.google.com/apis/library>).
2. Tìm **Google Calendar API** → bấm vào → **Enable**.

## Bước 3 — Cấu hình OAuth consent screen

1. Menu trái → **APIs & Services** → **OAuth consent screen**
   (trong giao diện mới có thể nằm ở **Google Auth Platform → Branding / Audience / Clients**).
2. **User Type: External** → **Create**.
3. Điền các trường bắt buộc:
    - **App name**
    - **User support email**
    - **Developer contact information** (email của bạn)
    - → **Save and Continue**.
4. **Scopes**: có thể bỏ qua → **Save and Continue**
   *(App tự xin scope `calendar.readonly` lúc chạy qua code, không cần khai ở đây.)*
5. **Test users** → **+ Add Users** → thêm **chính email Google bạn sẽ dùng để cấp quyền** → **Save**.

   > ⚠️ App đang ở chế độ **Testing**, nên **chỉ email nằm trong Test users** mới đăng nhập được.
   > Quên bước này sẽ bị lỗi `access_denied` / "app chưa được xác minh".

## Bước 4 — Tạo OAuth Client ID (credential)

1. Menu trái → **APIs & Services** → **Credentials**.
2. **+ Create Credentials** → **OAuth client ID**.
3. **Application type: Web application**.
4. **Name**: tùy ý (ví dụ `springboot-local`).
5. **Authorized redirect URIs** → **+ Add URI** → dán **chính xác**:

   ```
   http://localhost:8080/login/oauth2/code/google
   ```

   > ⚠️ Đây là redirect URI mặc định của Spring Security (`/login/oauth2/code/{registrationId}`).
   > Sai một ký tự sẽ bị lỗi `redirect_uri_mismatch`.
   > (Không cần điền **Authorized JavaScript origins** vì đây là flow server-side.)
6. **Create** → popup hiện **Client ID** và **Client secret**.
   **Copy cả hai** (hoặc bấm **Download JSON** để lưu lại).

---

## Bước 5 — Tạo file `.env` và điền credential

Ứng dụng đọc credential từ **biến môi trường** `GOOGLE_CLIENT_ID` và `GOOGLE_CLIENT_SECRET`.
Cách dễ nhất là dùng file `.env` (project đã bật sẵn qua
`spring.config.import=optional:file:.env[.properties]` trong `application.properties`).

1. Ở **thư mục gốc của project** (cùng cấp với `pom.xml`), tạo file tên `.env`.
2. Dán nội dung sau và thay bằng giá trị lấy ở Bước 4:

   ```properties
   GOOGLE_CLIENT_ID=xxxxxxxxxxxx.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=xxxxxxxxxxxxxxxx
   ```

    - `GOOGLE_CLIENT_ID`: dạng `...apps.googleusercontent.com`.
    - `GOOGLE_CLIENT_SECRET`: chuỗi bí mật đi kèm client.

> 🔒 File `.env` đã nằm trong `.gitignore` — **không commit** lên git. Đừng chia sẻ secret công khai.

**Cách thay thế (không dùng `.env`):** khai biến môi trường trong IntelliJ:
Run → **Edit Configurations…** → **Environment variables**:

```
GOOGLE_CLIENT_ID=...;GOOGLE_CLIENT_SECRET=...
```

---

## Bước 6 — Chạy ứng dụng

Yêu cầu **JDK 17+** (project target Java 17).

```bash
./mvnw spring-boot:run
```

> Nếu `java -version` của máy < 17, trỏ `JAVA_HOME` sang JDK 17/21 trước khi chạy, ví dụ:
> `JAVA_HOME="C:/Program Files/Java/jdk-21" ./mvnw spring-boot:run`

Sau khi app khởi động:

1. Mở trình duyệt: <http://localhost:8080/>.
2. Bấm **Connect Google Calendar**.
3. Đăng nhập bằng **email đã thêm ở Test users** (Bước 3.5) và **cấp quyền**.
4. Được chuyển về trang `/connected` — hiển thị: có access token, có refresh token,
   thời điểm hết hạn, scope được cấp.
5. Vào **Xem lịch của tôi** (`/calendars`) → chọn một lịch → xem **sự kiện sắp tới** (`/events`).

---

## Xử lý lỗi thường gặp

| Lỗi | Nguyên nhân & cách sửa |
|-----|------------------------|
| `redirect_uri_mismatch` | Redirect URI trên Google Console không khớp. Phải đúng `http://localhost:8080/login/oauth2/code/google`. |
| `access_denied` / "app chưa được xác minh" | Email đang dùng chưa nằm trong **Test users**. Thêm email đó ở OAuth consent screen. |
| App báo thiếu `GOOGLE_CLIENT_ID` khi khởi động | File `.env` chưa tạo/sai vị trí, hoặc chưa set biến môi trường. |
| `403` khi gọi Calendar API | Chưa **Enable** Google Calendar API (Bước 2), hoặc scope không đủ. |
| `invalid_grant` khi refresh | Refresh token bị thu hồi/hết hiệu lực → bấm Connect lại để cấp quyền mới. |
