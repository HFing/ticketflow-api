# Kế hoạch refactor Payment theo Stripe Checkout

## 1. Mục tiêu

Xây lại phần payment backend theo một luồng duy nhất:

```text
Client gửi yêu cầu checkout
    -> Backend khóa dữ liệu giữ chỗ và kiểm tra Payment
    -> Backend tạo/reuse Stripe Checkout Session bằng idempotency key
    -> Client nhận checkoutUrl để redirect sang Stripe
    -> Stripe xử lý thanh toán
    -> Stripe gọi webhook backend
    -> Backend xác thực Stripe-Signature
    -> Backend cập nhật Payment = PAID
    -> Backend xác nhận Booking = CONFIRMED và phát hành Ticket
```

Frontend chưa nằm trong phạm vi thực hiện. Backend chỉ trả `checkoutUrl` để frontend dùng sau này.

## 2. Hiện trạng đã khảo sát

- Checkout hiện nằm tại `POST /api/v1/customer/bookings/checkout` và đã nhận `Idempotency-Key`.
- Việc giữ vé đã dùng pessimistic lock trên các `TicketType`, giúp tránh overselling.
- Booking và Payment được tạo trong transaction nội bộ; Stripe API được gọi sau khi transaction giữ chỗ kết thúc. Cách tách này cần được giữ để không giữ database lock trong lúc gọi mạng.
- Stripe Checkout Session đã dùng `RequestOptions`, nhưng key hiện tại là `checkout-session:{paymentId}` thay vì `checkout-session:{bookingId}`.
- Webhook hiện đi qua abstraction đa provider (`PaymentGateway`, `PaymentGatewayRegistry`, `PaymentService`) dù hệ thống chỉ triển khai Stripe.
- Backend hiện có endpoint để client chủ động verify Stripe Session. Endpoint này làm client có thể tác động vào việc chuyển trạng thái payment, không đúng với yêu cầu webhook là nguồn xác nhận chính.
- `PaymentStatus.SUCCESS` chưa đúng tên nghiệp vụ mong muốn là `PAID`.
- `PaymentTransactionService` đang gộp quá nhiều trách nhiệm: giữ vé, tạo booking/payment, gắn Stripe Session, xử lý webhook, xác nhận booking, phát hành vé, cancel và expire.
- Scheduler hết hạn booking vẫn cần thiết để giải phóng số vé đang giữ khi Checkout Session bị bỏ dở.

## 3. Quyết định thiết kế

### 3.1. Chỉ hỗ trợ Stripe

Bỏ abstraction đa payment provider chưa được sử dụng. Code nghiệp vụ sẽ phụ thuộc vào một Stripe service rõ ràng thay vì chọn gateway qua registry.

Giữ trường `provider` trong Payment ở lần refactor này để không tạo thay đổi database phá vỡ dữ liệu hiện có, nhưng enum chỉ còn `STRIPE`. Việc xóa hẳn cột này nên là một migration riêng nếu thực sự cần.

### 3.2. Hai lớp idempotency

- Idempotency phía backend: tiếp tục yêu cầu header `Idempotency-Key`, lưu theo customer và request fingerprint. Cùng key + cùng request trả lại booking/session hiện có; cùng key + request khác trả `409`.
- Idempotency phía Stripe: luôn gửi `RequestOptions` với key ổn định `checkout-session:{bookingId}` khi tạo Checkout Session.
- Không tạo key Stripe mới cho mỗi lần retry.
- Lưu `providerSessionId` với unique constraint để một Stripe Session chỉ liên kết với một Payment.

### 3.3. Webhook là nguồn xác nhận thanh toán

- Endpoint chuẩn: `POST /api/v1/payments/stripe/webhook`.
- Endpoint public nhưng bắt buộc header `Stripe-Signature`.
- Xác thực chữ ký bằng raw request body và `Webhook.constructEvent(...)` trước khi xử lý event.
- Chỉ xác nhận trả tiền từ `checkout.session.completed` khi `payment_status=paid`; đồng thời hỗ trợ `checkout.session.async_payment_succeeded` để không bỏ sót phương thức thanh toán bất đồng bộ.
- Event không liên quan trả `200` và không thay đổi dữ liệu.
- Webhook lặp hoặc đến không đúng thứ tự phải idempotent: Payment đã `PAID` và Booking đã `CONFIRMED` thì trả thành công, không phát hành vé lần hai.
- Không dùng success URL hoặc endpoint do frontend gọi để xác nhận Payment.

### 3.4. Transaction và khóa

Không mở database transaction xuyên qua lời gọi Stripe.

Luồng checkout được chia thành ba đoạn:

1. Transaction giữ chỗ: lock các `TicketType` theo thứ tự ID, validate tồn kho, tạo/reuse Booking và Payment.
2. Ngoài transaction: tạo hoặc retrieve Stripe Checkout Session với Stripe idempotency key.
3. Transaction ngắn: lock Payment/Booking và lưu `providerSessionId`, `providerPaymentId`, checkout state.

Luồng webhook chạy trong một transaction ngắn:

1. Tìm và pessimistic-lock Payment theo Stripe Session ID.
2. Kiểm tra metadata `booking_id`, amount, currency và Payment/Booking hiện tại.
3. Chuyển Payment sang `PAID`, đặt `paidAt`.
4. Chuyển Booking sang `CONFIRMED`, chuyển held quantity sang sold quantity và tạo Ticket đúng một lần.

## 4. Cấu trúc code dự kiến

### Giữ và làm gọn

- `BookingServiceImpl`: orchestration checkout và các API booking; không chứa Stripe SDK.
- `PaymentRepository`, `BookingRepository`, `TicketTypeRepository`: bổ sung đúng query lock/fetch cần thiết.
- `PaymentExpirationScheduler`: giữ để hết hạn booking và giải phóng inventory; đổi sang gọi service Stripe trực tiếp.
- `PaymentMapper`: chỉ giữ mapping thực sự được API sử dụng.
- `PaymentProperties`: chỉ còn cấu hình chung cần thiết và Stripe.

### Tạo hoặc tách mới

- `StripeCheckoutService`: tạo, retrieve và expire Checkout Session; là nơi duy nhất dùng `StripeClient` cho checkout.
- `StripeWebhookController`: nhận raw payload, lấy `Stripe-Signature`, xác thực và chuyển Event hợp lệ cho service.
- `StripeWebhookService`: lọc loại event, đọc Checkout Session và gọi transaction xác nhận payment.
- `BookingPaymentTransactionService`: các transaction ngắn cho reserve, attach session, confirm, cancel/expire; tách helper theo trách nhiệm để thay thế class 467 dòng hiện tại.

### Xóa hoặc thay thế

- Xóa `PaymentGateway`.
- Xóa `PaymentGatewayRegistry`.
- Xóa `PaymentService` sau khi chuyển webhook sang `StripeWebhookService`.
- Thay `StripePaymentGateway` bằng `StripeCheckoutService` và logic webhook chuyên biệt.
- Xóa endpoint `POST /api/v1/payments/stripe/sessions/{sessionId}/verify`.
- Thay `PaymentController` bằng `StripeWebhookController`.
- Xóa/đổi tên các DTO generic chỉ phục vụ gateway abstraction; DTO nội bộ còn dùng sẽ chuyển sang tên Stripe/checkout hoặc booking rõ nghĩa.
- Bỏ các giá trị provider chưa triển khai (`PAYOS`, `MOMO`, `MOCK`).
- Xóa test của endpoint/service bị loại bỏ và viết lại test theo cấu trúc mới.

## 5. Thay đổi API và model

### Checkout

Giữ API hiện tại để tránh thay đổi không cần thiết:

```http
POST /api/v1/customer/bookings/checkout
Authorization: Bearer <token>
Idempotency-Key: <client-generated-key>
Content-Type: application/json
```

Response tiếp tục chứa tối thiểu:

- `bookingId`
- `payment.id`
- `payment.status`
- `payment.checkoutUrl`
- `expiresAt`

### Webhook

Đổi thành:

```http
POST /api/v1/payments/stripe/webhook
Stripe-Signature: <stripe-signature>
Content-Type: application/json
```

Phản hồi:

- `200`: event hợp lệ, kể cả event bị bỏ qua hoặc đã xử lý trước đó.
- `400`: thiếu/sai Stripe signature hoặc payload không hợp lệ.
- `500`: lỗi nội bộ tạm thời để Stripe retry; không nuốt lỗi database/nghiệp vụ chưa xử lý xong.

### Trạng thái

- Đổi `PaymentStatus.SUCCESS` thành `PaymentStatus.PAID`.
- Các trạng thái tối thiểu được giữ: `PENDING`, `PROCESSING`, `PAID`, `FAILED`, `CANCELLED`, `EXPIRED`, `REFUNDED` nếu luồng cancel/refund hiện tại vẫn cần tương thích.
- Booking chỉ chuyển `PENDING_PAYMENT -> CONFIRMED` từ webhook đã xác thực.

## 6. Trình tự triển khai

1. Viết test mô tả luồng đích trước khi refactor: checkout retry, Stripe idempotency key, signature invalid, webhook paid, webhook duplicate và webhook amount/currency mismatch.
2. Tách Stripe Checkout SDK ra `StripeCheckoutService`; đổi Stripe idempotency key thành `checkout-session:{bookingId}`.
3. Tách transaction giữ booking/payment khỏi class payment hiện tại, giữ nguyên nguyên tắc không gọi Stripe trong transaction.
4. Tạo `StripeWebhookController` và `StripeWebhookService`; cập nhật public endpoint trong security config.
5. Implement transaction webhook: lock Payment/Booking, validate Session, cập nhật `PAID`, xác nhận Booking và phát hành Ticket idempotently.
6. Chuyển scheduler/cancel sang service mới để vẫn giải phóng held inventory an toàn.
7. Xóa endpoint verify từ client và toàn bộ generic gateway/registry/service không còn tham chiếu.
8. Đổi `SUCCESS` thành `PAID`, cập nhật mapper, response, test và dữ liệu cấu hình liên quan.
9. Chạy full test suite, kiểm tra compile và tìm unused class/import/config.
10. Test tích hợp bằng Stripe CLI: forward webhook, thanh toán thành công, gửi lại cùng event và kiểm tra chỉ có một bộ Ticket được tạo.

## 7. Test bắt buộc

- Hai request checkout đồng thời cho cùng `Idempotency-Key` chỉ tạo một Booking, một Payment và một Stripe Checkout Session.
- Retry sau khi Stripe đã tạo Session nhưng backend chưa kịp lưu Session ID vẫn nhận đúng cùng Session nhờ Stripe idempotency key.
- Hai khách cùng mua lượng vé cuối không gây oversell.
- Thiếu hoặc sai `Stripe-Signature` trả `400` và không thay đổi Payment/Booking.
- `checkout.session.completed` chưa paid không xác nhận Booking.
- Event paid đúng amount/currency/booking metadata chuyển Payment sang `PAID` và Booking sang `CONFIRMED`.
- Gửi lại cùng webhook nhiều lần không tạo Ticket trùng và vẫn trả `200`.
- Event có Session ID, Payment Intent ID, booking metadata, amount hoặc currency không khớp không xác nhận Booking.
- Webhook đến sau khi booking vừa expire phải không âm thầm xác nhận sai; cần log/cảnh báo và có chính sách xử lý rõ ràng.
- Booking hết hạn giải phóng held quantity đúng một lần và Checkout Session được expire nếu đã tồn tại.

## 8. Tiêu chí hoàn thành

- Luồng thành công chỉ được chốt bởi webhook Stripe đã xác thực.
- Payment là `PAID` và Booking là `CONFIRMED` trong cùng database transaction.
- Checkout có idempotency ở cả database và Stripe.
- Không có Stripe network call bên trong database transaction đang giữ lock.
- Không còn endpoint client verify session và không còn gateway registry đa provider.
- Không oversell, không double payment resource, không phát hành Ticket trùng.
- Toàn bộ test Maven pass và không còn class/config/test mồ côi.

## 9. Ngoài phạm vi

- Frontend disable button và redirect.
- Trang success/cancel phía frontend.
- Refund đầy đủ, dispute/chargeback và reconciliation job nâng cao.
- Thêm payment provider khác ngoài Stripe.
