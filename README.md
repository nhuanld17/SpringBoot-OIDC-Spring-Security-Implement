
# Google OIDC Login Demo with Spring Boot + Thymeleaf

## Mô tả dự án

**Google OIDC Login Demo with Spring Boot + Thymeleaf** là một ứng dụng web server-side được xây dựng nhằm minh họa cơ chế **OpenID Connect (OIDC)** thông qua việc đăng nhập người dùng bằng tài khoản Google. Ứng dụng cho phép người dùng thực hiện quy trình đăng nhập với Google, sau đó backend nhận và xác thực thông tin định danh của người dùng để thiết lập phiên đăng nhập trong hệ thống.

**Điểm đặc biệt:** Dự án implement OIDC flow **hoàn toàn thủ công** mà KHÔNG sử dụng Spring Security OAuth2 Client. Tất cả các bước (tạo authorization request, verify state, exchange code, verify ID token, quản lý token) đều được tự tay viết để minh họa rõ bản chất của OIDC.

## Mục tiêu

Mục tiêu của dự án là giúp người học hiểu đầy đủ một luồng **OIDC login hiện đại** trong bối cảnh thực tế với Google. Cụ thể, dự án sử dụng **Authorization Code flow**, kết hợp **PKCE**, **state**, **nonce**, **ID token**, và có thể kèm **access token** hoặc **refresh token** tùy nhu cầu mở rộng để đảm bảo vừa đúng bản chất OIDC vừa phù hợp với cách triển khai thực tế trong Spring Boot.

Sau khi hoàn thành dự án, người học cần nắm được:
- Vai trò của **client application**, **OpenID Provider**, và trình duyệt người dùng trong một hệ thống OIDC
- Cách hoạt động của **Authorization Code flow** khi dùng cho đăng nhập OIDC với Google
- Vai trò của `state` trong việc chống CSRF ở redirect-based flow
- Vai trò của `nonce` trong việc chống replay đối với **ID token**
- Vai trò của **PKCE** với `code_verifier` và `code_challenge` để bảo vệ authorization code khỏi bị đánh cắp
- Cách backend sử dụng **ID token** và thông tin user claims để xác định người dùng là ai.
- Cách backend tự xác thực **ID token** (verify signature RS256 bằng JWKS, verify claims: iss, aud, exp, iat, nonce)
- Cách backend quản lý token (access token, refresh token) trong session và tự động refresh khi sắp hết hạn

## Thành phần bảo mật chính

Dự án sử dụng **Authorization Code flow** vì đây là luồng phù hợp cho ứng dụng web có backend và cũng là cách phổ biến để triển khai OIDC login an toàn với nhà cung cấp danh tính như Google. Sau khi người dùng đăng nhập và consent, Google trả về authorization code để backend đổi lấy token, trong đó **ID token** là thành phần cốt lõi để xác thực danh tính.

Dự án có sử dụng **PKCE** với `code_verifier` và `code_challenge`, trong đó `code_challenge_method` nên là `S256` để đảm bảo mức an toàn hiện đại. PKCE giúp bảo vệ authorization code khỏi bị chặn và sử dụng trái phép trong quá trình đổi token.

Ngoài PKCE, dự án còn sử dụng **`state`** để liên kết request và callback response nhằm giảm rủi ro CSRF. Đồng thời, dự án sử dụng thêm **`nonce`** để liên kết authorization request với **ID token** nhận được, từ đó giúp phát hiện replay attack đối với token định danh.

Sau khi exchange code thành công, backend nhận **ID token** để xác thực người dùng, và có thể nhận thêm **access token** để gọi UserInfo endpoint hoặc các Google API khác nếu cần. Nếu ứng dụng cần duy trì khả năng truy cập dài hạn hoặc tích hợp thêm API của Google, hệ thống có thể nhận **refresh token** trong các điều kiện phù hợp do Google hỗ trợ.

## Luồng hoạt động

Luồng xử lý tổng quát của hệ thống diễn ra như sau:
1. Người dùng truy cập trang chủ và bấm nút **Connect with Google** trên giao diện Thymeleaf.
2. Backend khởi tạo authorization request theo **Authorization Code flow**, kèm `scope=openid profile email`, cùng `state`, `nonce`, `code_challenge`, `code_challenge_method=S256`, và redirect URI đã đăng ký trước với Google.
3. Trình duyệt được chuyển hướng đến Google để người dùng đăng nhập và cấp quyền chia sẻ thông tin định danh cơ bản cho ứng dụng.
4. Google chuyển hướng người dùng quay về ứng dụng cùng với authorization code và giá trị `state` phản hồi tương ứng.
5. Backend kiểm tra `state`, gửi authorization code cùng `code_verifier` tới token endpoint để đổi lấy tokens.
6. Hệ thống nhận **ID token**, xác thực chữ ký (RS256 bằng JWKS từ Google) và các claims quan trọng như `iss`, `aud`, `exp`, `iat`, và `nonce`, sau đó trích xuất thông tin người dùng như `sub`, `email`, `name`, `picture` nếu có.
7. Backend lưu thông tin user và token vào HttpSession, thiết lập phiên đăng nhập nội bộ mà KHÔNG cần Spring Security OAuth2 Client.

## Phạm vi chức năng

Phiên bản hiện tại của dự án bao gồm các chức năng sau:
- Trang chủ hiển thị nút **Connect with Google** để khởi tạo OIDC flow.
- Khởi tạo OIDC Authorization Code flow với Google (kèm PKCE, state, nonce).
- Nhận callback sau đăng nhập, verify state và exchange code lấy token.
- Xác thực **ID token** (signature RS256 + all claims) và trích xuất thông tin user.
- Hiển thị trang **connected** sau khi đăng nhập thành công (thông tin token).
- Trang **profile** hiển thị thông tin người dùng (name, email, picture, sub).
- Trang **calendars** và **events** để xem danh sách lịch và sự kiện từ Google Calendar API.
- Trang **refreshed** để demo force refresh access token.
- **Logout** để invalidate session.

Nếu muốn mở rộng thêm, có thể bổ sung:
- Ánh xạ người dùng Google vào user nội bộ trong database.
- Phân quyền theo email domain hoặc role nội bộ sau khi login.
- So sánh với cơ chế **OIDC logout** trong các provider hỗ trợ tốt hơn.

## Token và lưu trữ

Trong dự án này, **ID token** là token quan trọng nhất vì nó chứa thông tin định danh của người dùng và được dùng để xác thực rằng người dùng đã đăng nhập thành công thông qua Google. **Access token** được dùng để gọi Google Calendar API (xem danh sách lịch, sự kiện). **Refresh token** cho phép lấy access token mới khi hết hạn mà không cần user consent lại.

Token được lưu trong **HttpSession** ở phía server-side (sử dụng session attribute `oidc_token`, `oidc_expires_at`). Trình duyệt chỉ giữ session ID của ứng dụng, không trực tiếp giữ token nhạy cảm.

**Lưu ý quan trọng về refresh token:** Google không trả refresh token mới khi refresh. Do đó, ứng dụng phải **merge** - giữ lại refresh token cũ khi nhận response từ refresh endpoint.

## Công nghệ sử dụng

Dự án sử dụng:
- **Spring Boot** để xây dựng ứng dụng web.
- **Spring Web (MVC)** để xử lý HTTP requests — KHÔNG dùng Spring Security OAuth2 Client.
- **Spring Session** (HttpSession) để lưu token và thông tin user.
- **RestClient** (Spring's modern HTTP client) để gọi Google token endpoint và Calendar API.
- **Thymeleaf** để xây dựng giao diện server-side rendering.
- **Google** làm **OpenID Provider** để xác thực người dùng.
- Các scope: `openid profile email` (OIDC) và `https://www.googleapis.com/auth/calendar.readonly` (Calendar API).

## Ý nghĩa của dự án

Dự án này giúp người học nhìn thấy **toàn bộ bản chất của OIDC** được implement thủ công, không dựa vào thư viện OAuth2 Client. Người học sẽ thấy rõ cách từng thành phần (**Authorization Code flow**, **PKCE**, **state**, **nonce**, **ID token verification**, **token management**) hoạt động ở mức thấp nhất.

Đây là project phù hợp để:
- Học nền tảng **OIDC authentication** từ đầu, không che giấu bởi thư viện.
- Phân biệt rõ OIDC (xác thực danh tính) với **OAuth2** thuần (ủy quyền truy cập tài nguyên).
- Mở rộng sang các chủ đề nâng cao như ánh xạ user nội bộ, nhiều provider đăng nhập, SSO.