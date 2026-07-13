
# Google OIDC Login Demo with Spring Boot + Thymeleaf

## Mô tả dự án

**Google OIDC Login Demo with Spring Boot + Thymeleaf** là một ứng dụng web server-side được xây dựng nhằm minh họa cơ chế **OpenID Connect (OIDC)** thông qua việc đăng nhập người dùng bằng tài khoản Google. Ứng dụng cho phép người dùng thực hiện quy trình đăng nhập với Google, sau đó backend nhận và xác thực thông tin định danh của người dùng để thiết lập phiên đăng nhập trong hệ thống

Dự án này **không tập trung vào việc gọi Google API để thao tác trên tài nguyên của người dùng** như Google Calendar hay Google Drive, mà tập trung vào bản chất của OIDC: xác thực danh tính người dùng dựa trên **ID token**. Vì vậy, đây là một project phù hợp để phân biệt rõ với OAuth2 thuần, nơi trọng tâm là ủy quyền truy cập tài nguyên thay mặt người dùng

## Mục tiêu

Mục tiêu của dự án là giúp người học hiểu đầy đủ một luồng **OIDC login hiện đại** trong bối cảnh thực tế với Google. Cụ thể, dự án sử dụng **Authorization Code flow**, kết hợp **PKCE**, **state**, **nonce**, **ID token**, và có thể kèm **access token** hoặc **refresh token** tùy nhu cầu mở rộng để đảm bảo vừa đúng bản chất OIDC vừa phù hợp với cách triển khai thực tế trong Spring Boot

Sau khi hoàn thành dự án, người học cần nắm được:
- Vai trò của **client application**, **OpenID Provider**, và trình duyệt người dùng trong một hệ thống OIDC
- Cách hoạt động của **Authorization Code flow** khi dùng cho đăng nhập OIDC với Google
- Vai trò của `state` trong việc chống CSRF ở redirect-based flow
- Vai trò của `nonce` trong việc chống replay đối với **ID token**
- Vai trò của **PKCE** với `code_verifier` và `code_challenge` để bảo vệ authorization code khỏi bị đánh cắp
- Cách backend sử dụng **ID token** và thông tin user claims để xác định người dùng là ai.
- Cách Spring Security thiết lập phiên đăng nhập cục bộ sau khi xác thực thành công với Google.

## Thành phần bảo mật chính

Dự án sử dụng **Authorization Code flow** vì đây là luồng phù hợp cho ứng dụng web có backend và cũng là cách phổ biến để triển khai OIDC login an toàn với nhà cung cấp danh tính như Google. Sau khi người dùng đăng nhập và consent, Google trả về authorization code để backend đổi lấy token, trong đó **ID token** là thành phần cốt lõi để xác thực danh tính.

Dự án có sử dụng **PKCE** với `code_verifier` và `code_challenge`, trong đó `code_challenge_method` nên là `S256` để đảm bảo mức an toàn hiện đại. PKCE giúp bảo vệ authorization code khỏi bị chặn và sử dụng trái phép trong quá trình đổi token.

Ngoài PKCE, dự án còn sử dụng **`state`** để liên kết request và callback response nhằm giảm rủi ro CSRF. Đồng thời, dự án sử dụng thêm **`nonce`** để liên kết authorization request với **ID token** nhận được, từ đó giúp phát hiện replay attack đối với token định danh.

Sau khi exchange code thành công, backend nhận **ID token** để xác thực người dùng, và có thể nhận thêm **access token** để gọi UserInfo endpoint hoặc các Google API khác nếu cần. Nếu ứng dụng cần duy trì khả năng truy cập dài hạn hoặc tích hợp thêm API của Google, hệ thống có thể nhận **refresh token** trong các điều kiện phù hợp do Google hỗ trợ.

## Luồng hoạt động

Luồng xử lý tổng quát của hệ thống diễn ra như sau:
1. Người dùng truy cập trang chủ và bấm nút **Login with Google** trên giao diện Thymeleaf.
2. Backend khởi tạo authorization request theo **Authorization Code flow**, kèm `scope=openid profile email`, cùng `state`, `nonce`, `code_challenge`, `code_challenge_method=S256`, và redirect URI đã đăng ký trước với Google.
3. Trình duyệt được chuyển hướng đến Google để người dùng đăng nhập và cấp quyền chia sẻ thông tin định danh cơ bản cho ứng dụng.
4. Google chuyển hướng người dùng quay về ứng dụng cùng với authorization code và giá trị `state` phản hồi tương ứng.
5. Backend kiểm tra `state`, gửi authorization code cùng `code_verifier` tới token endpoint để đổi lấy tokens.
6. Hệ thống nhận **ID token**, xác thực chữ ký và các claims quan trọng như `iss`, `aud`, `exp`, và `nonce`, sau đó trích xuất thông tin người dùng như `sub`, `email`, `name`, `picture` nếu có.
7. Spring Security tạo đối tượng người dùng đã xác thực và thiết lập phiên đăng nhập trong ứng dụng để người dùng truy cập các trang được bảo vệ.

## Phạm vi chức năng

Phiên bản đơn giản của dự án nên bao gồm các chức năng sau:
- Trang chủ hiển thị nút **Login with Google**.
- Khởi động OIDC Authorization Code flow với Google.
- Nhận callback sau đăng nhập.
- Xác thực **ID token** và lấy thông tin người dùng từ claims hoặc UserInfo endpoint.
- Hiển thị trang hồ sơ người dùng sau khi đăng nhập, ví dụ gồm họ tên, email, avatar, và subject identifier nếu có

Nếu muốn mở rộng thêm nhưng vẫn giữ đúng tinh thần OIDC demo, có thể bổ sung:
- Ánh xạ người dùng Google vào user nội bộ trong database.
- Phân quyền theo email domain hoặc role nội bộ sau khi login.
- Logout cục bộ và so sánh với cơ chế **OIDC logout** trong các provider hỗ trợ tốt hơn.

## Token và lưu trữ

Trong dự án này, **ID token** là token quan trọng nhất vì nó chứa thông tin định danh của người dùng và được dùng để xác thực rằng người dùng đã đăng nhập thành công thông qua Google. **Access token** có thể xuất hiện kèm theo, nhưng nó không phải thành phần chính để chứng minh danh tính trong OIDC; vai trò chính của nó là truy cập tài nguyên hoặc endpoint bổ sung được bảo vệ.

Ở phiên bản học tập đơn giản, token có thể được Spring Security OAuth2 Client quản lý ở phía server-side, còn trình duyệt chỉ giữ session của ứng dụng chứ không trực tiếp giữ token nhạy cảm. Nếu cần hỗ trợ đăng nhập lâu dài hoặc gọi thêm Google API sau khi người dùng đã rời khỏi flow ban đầu, ứng dụng có thể xử lý thêm refresh token theo chiến lược lưu trữ an toàn ở backend.

## Công nghệ sử dụng

Dự án sử dụng:
- Spring Boot để xây dựng ứng dụng web.
- Spring Security OAuth2 Client để triển khai OIDC login với Google..
- Thymeleaf để xây dựng giao diện tối giản và render dữ liệu server-side.
- Google làm **OpenID Provider** để xác thực người dùng.
- Các scope cơ bản như `openid`, `profile`, và `email` để nhận thông tin định danh cần thiết từ Google.

## Ý nghĩa của dự án

Dự án này giúp người học nhìn thấy đầy đủ mối liên hệ giữa **Authorization Code flow**, **PKCE**, **state**, **nonce**, **ID token**, và phiên đăng nhập nội bộ trong một ứng dụng thực tế dùng Google làm Identity Provider. Đây là một project phù hợp để học nền tảng **OIDC authentication** hiện đại trước khi mở rộng sang các chủ đề nâng cao hơn như ánh xạ user nội bộ, nhiều provider đăng nhập, SSO, hoặc so sánh trực tiếp với một project OAuth2 thuần ở hướng delegated authorization.