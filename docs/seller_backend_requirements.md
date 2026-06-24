# Yêu cầu API Backend cho Phân hệ Người Bán (Seller)

Dưới đây là chi tiết các API cần thiết để thực hiện các chức năng của Người Bán (Seller Center), bao gồm cả các tham số truyền vào (Input/Query) và cấu trúc dữ liệu trả về dự kiến (Output). Các endpoint này đều yêu cầu **xác thực JWT (Role: Seller)**.

---

## 1. Bảng điều khiển (Dashboard)

### 1.1 Thống kê tổng quan (Summary Bento Cards)
- **Endpoint**: `GET /api/seller/dashboard/summary`
- **Query / Body**: Không có
- **Output (200 OK)**:
  ```json
  {
    "totalRevenue": 15000000,
    "newOrders": 12,
    "productsOnline": 45
  }
  ```

### 1.2 Biểu đồ doanh thu (Revenue Bar Chart)
- **Endpoint**: `GET /api/seller/dashboard/revenue-chart`
- **Query Params**: 
  - `timeRange`: `week` | `month` | `year`
- **Output (200 OK)**:
  ```json
  [
    { "label": "Thứ 2", "value": 1500000 },
    { "label": "Thứ 3", "value": 2000000 }
  ]
  ```

### 1.3 Biểu đồ tròn theo danh mục (Category Revenue Pie Chart)
- **Endpoint**: `GET /api/seller/dashboard/category-chart`
- **Query / Body**: Không có
- **Output (200 OK)**:
  ```json
  [
    { "categoryName": "Áo Nam", "percentage": 40.5, "revenue": 5000000 },
    { "categoryName": "Quần Nữ", "percentage": 59.5, "revenue": 7000000 }
  ]
  ```

---

## 2. Quản lý sản phẩm (Product Management)

### 2.1 Lấy danh sách sản phẩm
- **Endpoint**: `GET /api/seller/products`
- **Query Params**: 
  - Cơ bản: `page` (int), `size` (int), `search` (string).
  - Bộ lọc: `status` (boolean), `categoryId` (uuid), `stockStatus` (in_stock, low_stock, out_of_stock), `minPrice`, `maxPrice` (int).
  - Sắp xếp: `sortBy` (price_asc, price_desc, created_at_desc, sold_count_desc).
- **Output (200 OK)** (Dạng chuẩn Spring Data `Page`):
  ```json
  {
    "content": [
      {
        "id": "uuid-1",
        "name": "Áo Thun Nam",
        "price": 150000,
        "stock": 50,
        "soldCount": 10,
        "isActive": true,
        "conditionStatus": "new",
        "imageUrl": "https://link-anh-bia.jpg",
        "category": { "id": "uuid-cat", "name": "Áo Nam" }
      }
    ],
    "totalElements": 50,
    "totalPages": 5,
    "size": 10,
    "number": 0
  }
  ```

### 2.2 Thêm / Cập nhật sản phẩm
- **Thêm mới**: `POST /api/seller/products`
- **Cập nhật**: `PUT /api/seller/products/{id}`
- **Input Body (JSON)**:
  ```json
  {
    "name": "Áo Thun Nam",
    "description": "Mô tả chi tiết sản phẩm...",
    "price": 150000,
    "stock": 50,
    "categoryId": "uuid-cat",
    "conditionStatus": "new",
    "imageUrl": "https://link-anh-bia.jpg",
    "images": [
      "https://link-anh-phu-1.jpg",
      "https://link-anh-phu-2.jpg"
    ]
  }
  ```
- **Output (200/201)**: Đối tượng sản phẩm vừa được tạo/cập nhật (cấu trúc tương tự item trong content ở trên).

### 2.3 Ẩn / Hiện sản phẩm
- **Endpoint**: `PATCH /api/seller/products/{id}/status`
- **Input Body**:
  ```json
  { "isActive": false }
  ```
- **Output (200 OK)**: Trả về trạng thái HTTP thành công (không cần body).

---

## 3. Quản lý đơn hàng (Order Management)

### 3.1 Lấy danh sách đơn hàng
- **Endpoint**: `GET /api/orders?role=seller`
- **Query Params**: 
  - Cơ bản: `page`, `size`, `search` (Mã đơn hàng, SĐT người mua).
  - Bộ lọc: `status` (pending, shipping, delivered, cancelled), `fromDate`, `toDate`, `paymentMethod`, `paymentStatus`.
- **Output (200 OK)** (Dạng chuẩn Spring Data `Page`):
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "orderCode": "TC-2024-0891",
        "status": "pending",
        "totalAmount": 300000,
        "shippingFee": 30000,
        "paymentMethod": "cod",
        "paymentStatus": "unpaid",
        "buyerName": "Nguyễn Văn A",
        "buyerPhone": "0901234567",
        "shippingAddress": "123 Đường A, Phường B, Quận C, TP D",
        "createdAt": "2024-05-19T08:00:00",
        "ghnTrackingCode": null,
        "items": [
          {
            "productName": "Áo Thun Nam",
            "quantity": 2,
            "price": 150000,
            "imageUrl": "https://link-anh.jpg"
          }
        ]
      }
    ],
    "totalElements": 20,
    "totalPages": 2
  }
  ```

### 3.2 Cập nhật trạng thái đơn hàng (Actions)
- **Xác nhận**: `PATCH /api/orders/{id}/confirm`
- **Hoàn thành**: `PATCH /api/orders/{id}/deliver`
- **Giao hàng (Ship)**: `PATCH /api/orders/{id}/ship`
  - **Lưu ý**: Lệnh Ship sẽ gọi sang GHN để tạo đơn giao hàng.
  - **Output (200 OK)**: Trả về Object Đơn hàng đã cập nhật kèm `ghnTrackingCode`.
- **Hủy đơn (Cancel)**: `PATCH /api/orders/{id}/cancel`
  - **Input Body**:
    ```json
    { "cancelReason": "Hết hàng trong kho" }
    ```
  - **Output (200 OK)**: Đối tượng Đơn hàng đã chuyển status sang `cancelled`.

---

## 4. Trang Đánh giá & Tích điểm uy tín (Reputation / Points)

### 4.1 Chỉ số uy tín tổng quan
- **Endpoint**: `GET /api/seller/reputation/summary`
- **Query / Body**: Không có
- **Output (200 OK)**:
  ```json
  {
    "averageRating": 4.8,
    "totalReviews": 150,
    "cancellationRate": 2.5,
    "responseRate": 98.0,
    "currentPoints": 1200
  }
  ```

### 4.2 Lịch sử đánh giá / biến động điểm (History Table)
- **Endpoint**: `GET /api/seller/reputation/history`
- **Query Params**: `page`, `size`, `rating` (1-5), `isReplied` (boolean), `fromDate`, `toDate`.
- **Output (200 OK)** (Dạng chuẩn Spring Data `Page`):
  ```json
  {
    "content": [
      {
        "id": "uuid-review",
        "rating": 5,
        "comment": "Đóng gói cẩn thận, áo đẹp",
        "buyerName": "Lê Thị B",
        "orderId": "uuid-order",
        "orderCode": "TC-2024-0891",
        "productName": "Áo Thun Nam",
        "sellerReply": null,
        "createdAt": "2024-05-20T10:00:00"
      }
    ],
    "totalElements": 150,
    "totalPages": 15
  }
  ```

---

## 5. Hồ sơ cửa hàng (Store Profile)

### 5.1 Lấy / Cập nhật hồ sơ cửa hàng
- **Lấy hồ sơ**: `GET /api/seller/profile`
- **Cập nhật hồ sơ**: `PUT /api/seller/profile`
- **Input/Output Cập nhật (JSON)**:
  ```json
  {
    "storeName": "Tủ cũ chill",
    "description": "Chuyên thời trang second-hand chất lượng cao.",
    "avatarUrl": "https://link-avatar.jpg",
    "coverUrl": "https://link-cover.jpg",
    "address": "456 Đường X, Phường Y, Quận Z, TP. HCM",
    "phone": "0987654321",
    "ghnShopId": 123456
  }
  ```

---

> [!TIP]
> **Xử lý Upload Ảnh (Media Handling)**
> Thay vì gửi trực tiếp file ảnh trong các request trên (qua `multipart/form-data`), Backend nên thiết kế một API Upload riêng biệt (ví dụ: `POST /api/upload`) để client upload từng file và nhận lại chuỗi URL (`imageUrl`). Client sau đó chỉ cần gửi cục chuỗi URL này trong các payload JSON của Product/Profile như mô tả.
