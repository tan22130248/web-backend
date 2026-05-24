# Plan giao diện — Order & Delivery Flow (Desktop)

Danh sách các màn hình cần bổ sung, chia theo role (Buyer / Seller).

---

## BUYER

### 1. Giỏ hàng (Cart Page)
- **Route:** `/cart`
- **Nội dung:**
  - Danh sách sản phẩm trong giỏ (ảnh, tên, giá, số lượng, tổng)
  - Nhóm theo shop (mỗi shop 1 group)
  - Nút +/- số lượng, nút xoá item
  - Cảnh báo nếu sản phẩm hết hàng / đã ẩn / giá thay đổi (từ API batch)
  - Tổng tiền + phí ship (nếu có)
  - Nút "Đặt hàng" → chuyển sang Checkout

### 2. Checkout (Đặt hàng)
- **Route:** `/checkout`
- **Nội dung:**
  - Chọn/nhập địa chỉ giao hàng (tên, SĐT, địa chỉ chi tiết)
  - Danh sách sản phẩm sắp mua (review lần cuối)
  - Phương thức thanh toán: COD (mặc định)
  - Ô ghi chú cho seller
  - Tổng tiền + phí ship
  - Nút "Xác nhận đặt hàng"

### 3. Đặt hàng thành công
- **Route:** `/order-success`
- **Nội dung:**
  - Thông báo đặt hàng thành công (icon check)
  - Mã đơn hàng
  - Nút "Xem đơn hàng" / "Tiếp tục mua sắm"

### 4. Danh sách đơn hàng (Buyer)
- **Route:** `/orders`
- **Nội dung:**
  - Tabs filter theo trạng thái: Tất cả | Chờ xác nhận | Đã xác nhận | Đang giao | Đã giao | Đã huỷ | Hoàn tiền
  - Mỗi đơn hiển thị: mã đơn, tên shop, sản phẩm (ảnh + tên), tổng tiền, trạng thái, ngày đặt
  - Nút hành động theo trạng thái:
    - Pending/Confirmed → "Huỷ đơn"
    - Delivered → "Yêu cầu hoàn tiền"
  - Pagination

### 5. Chi tiết đơn hàng (Buyer)
- **Route:** `/orders/{id}`
- **Nội dung:**
  - Thông tin đơn: mã, trạng thái (badge màu), ngày tạo
  - Timeline trạng thái (pending → confirmed → shipping → delivered)
  - Thông tin giao hàng: tên, SĐT, địa chỉ
  - Danh sách sản phẩm (ảnh, tên, variant, số lượng, đơn giá, thành tiền)
  - Tổng: tiền hàng + phí ship = tổng thanh toán
  - Phương thức thanh toán
  - Nút hành động: "Huỷ đơn" hoặc "Yêu cầu hoàn tiền" (tuỳ trạng thái)

### 6. Dialog huỷ đơn (Buyer)
- **Dạng:** Modal/Dialog
- **Nội dung:**
  - Chọn lý do huỷ (dropdown hoặc radio): "Đổi ý", "Muốn thay đổi địa chỉ", "Tìm được giá tốt hơn", "Khác"
  - Nút "Xác nhận huỷ"

### 7. Dialog yêu cầu hoàn tiền (Buyer)
- **Dạng:** Modal/Dialog
- **Nội dung:**
  - Chọn lý do: "Hàng không đúng mô tả", "Hàng bị lỗi/hư", "Nhận sai sản phẩm", "Khác"
  - Ô mô tả chi tiết (textarea)
  - Nút "Gửi yêu cầu"

---

## SELLER

### 8. Danh sách đơn hàng (Seller)
- **Route:** `/seller/orders`
- **Nội dung:**
  - Tabs: Chờ xác nhận | Đã xác nhận | Đang giao | Đã giao | Đã huỷ | Hoàn tiền
  - Mỗi đơn: mã, tên buyer, sản phẩm, tổng tiền, trạng thái, ngày đặt
  - Nút hành động theo trạng thái:
    - Pending → "Xác nhận" / "Huỷ đơn"
    - Confirmed → "Giao hàng"
    - Shipping → "Đã giao"
  - Pagination
  - Có thể thêm: search theo mã đơn, filter theo ngày

### 9. Chi tiết đơn hàng (Seller)
- **Route:** `/seller/orders/{id}`
- **Nội dung:**
  - Giống buyer nhưng thêm:
    - Thông tin buyer (tên, SĐT)
    - Nút hành động seller (Xác nhận / Ship / Đã giao / Huỷ)
  - Timeline trạng thái
  - Ghi chú từ buyer

### 10. Dialog huỷ đơn (Seller)
- **Dạng:** Modal/Dialog
- **Nội dung:**
  - Lý do: "Hết hàng", "Không liên lạc được buyer", "Sản phẩm bị lỗi", "Khác"
  - Nút "Xác nhận huỷ"

---

## SHARED / COMPONENTS

### 11. Order Status Badge
- Component hiển thị trạng thái đơn hàng với màu sắc:
  - Pending → Vàng/Orange
  - Confirmed → Xanh dương
  - Shipping → Tím/Indigo
  - Delivered → Xanh lá
  - Cancelled → Đỏ/Xám
  - Refunded → Đỏ

### 12. Order Timeline
- Component hiển thị lịch sử trạng thái dạng vertical timeline:
  - Mỗi step: icon + tên trạng thái + thời gian + ghi chú
  - Step hiện tại highlight, step tương lai mờ

### 13. Notification Dropdown / Page
- **Route:** `/notifications` (hoặc dropdown ở header)
- **Nội dung:**
  - Danh sách thông báo (icon + title + body + thời gian)
  - Badge số thông báo chưa đọc
  - Click vào → đánh dấu đã đọc + navigate đến đơn hàng liên quan

---

## Tổng hợp routes

| # | Route | Role | Màn hình |
|---|-------|------|----------|
| 1 | `/cart` | Buyer | Giỏ hàng |
| 2 | `/checkout` | Buyer | Đặt hàng |
| 3 | `/order-success` | Buyer | Thành công |
| 4 | `/orders` | Buyer | DS đơn hàng |
| 5 | `/orders/{id}` | Buyer | Chi tiết đơn |
| 6 | `/seller/orders` | Seller | DS đơn hàng seller |
| 7 | `/seller/orders/{id}` | Seller | Chi tiết đơn seller |
| 8 | `/notifications` | All | Thông báo |

---

## Ghi chú

- Tất cả trang cần auth (trừ cart nếu muốn cho guest xem giỏ local).
- Cart lưu client-side (localStorage), gọi `POST /api/products/batch` để refresh data khi mở trang.
- Khi đặt hàng, frontend gửi `POST /api/orders` với body chứa `items[]` + `shippingAddress`.
- Sau khi đặt thành công, frontend xoá localStorage cart.
