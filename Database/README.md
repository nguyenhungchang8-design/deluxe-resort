# Database Design - Resort Management System (Group 6)

Database chung cho toàn bộ dự án **Resort Management System**, được thiết kế theo đúng Work Breakdown Structure (WBS) gồm **5 Feature** và tương thích 100% với Backend Spring Boot (`com.deluxe.entity`).

- **Hệ quản trị CSDL:** Microsoft SQL Server 2017+ (hoặc Azure SQL / Docker SQL Server)
- **Database Name:** `deluxe_db`
- **Tập tin SQL:** [deluxe_db.sql](file:///c:/Users/Admin/Desktop/deluxe-master/Database/deluxe_db.sql)

---

## 1. Phân chia Bảng theo 5 Feature

| Feature | Tên Bảng (Table) | Mô tả | Khóa chính / Khóa ngoại |
|---|---|---|---|
| **Feature 1: Auth & User** | `users` | Quản lý tài khoản và phân quyền cho 5 vai trò | PK: `id`, Roles: `ROLE_ADMIN`, `ROLE_CUSTOMER`, `ROLE_RECEPTIONIST`, `ROLE_HOUSEKEEPING`, `ROLE_ACCOUNTANT`, `ROLE_WAITER` |
| **Feature 2: Room & Booking** | `room_types` | Phân loại phòng (Standard, Deluxe, Suite, Villa) | PK: `id` |
| | `rooms` | Danh sách phòng thực tế và trạng thái phòng | PK: `id`, FK: `room_type_id` $\rightarrow$ `room_types(id)` |
| | `bookings` | Đơn đặt phòng của khách | PK: `id`, FK: `customer_id` $\rightarrow$ `users(id)`, `room_id` $\rightarrow$ `rooms(id)` |
| **Feature 3: Food Menu & Ordering** | `menu_items` | Danh mục món ăn, thức uống, giá và tình trạng | PK: `id` |
| | `food_orders` | Đơn gọi món ăn theo khách/phòng | PK: `id`, FK: `customer_id` $\rightarrow$ `users(id)`, `room_id` $\rightarrow$ `rooms(id)` |
| | `order_items` | Chi tiết số lượng và ghi chú từng món trong đơn | PK: `id`, FK: `order_id` $\rightarrow$ `food_orders(id)`, `menu_item_id` $\rightarrow$ `menu_items(id)` |
| **Feature 4: Housekeeping** | `cleaning_tasks` | Nhiệm vụ dọn phòng được giao cho nhân viên | PK: `id`, FK: `room_id` $\rightarrow$ `rooms(id)`, `assigned_to` $\rightarrow$ `users(id)` |
| | `maintenance_requests`| Báo cáo phòng hỏng hóc, thiết bị cần sửa chữa | PK: `id`, FK: `room_id` $\rightarrow$ `rooms(id)`, `reported_by` $\rightarrow$ `users(id)` |
| **Feature 5: Invoice, Payment & Reports** | `invoices` | Hóa đơn tổng hợp tiền phòng + tiền ăn | PK: `id`, FK: `booking_id` $\rightarrow$ `bookings(id)` |
| | `payments` | Lịch sử các lần thanh toán / quẹt thẻ / tiền mặt | PK: `id`, FK: `invoice_id` $\rightarrow$ `invoices(id)` |
| | `deposit_refunds` | Quản lý tiền cọc và lịch sử hoàn tiền | PK: `id`, FK: `booking_id` $\rightarrow$ `bookings(id)` |

---

## 2. Tài khoản mẫu đăng nhập (Seed Users)

Tất cả các tài khoản đã được mã hóa bằng chuẩn **BCrypt** của Spring Security:

| Username | Password mặc định | Họ và tên | Role trong hệ thống |
|---|---|---|---|
| `admin` | `admin123` | Quản Trị Viên | `ROLE_ADMIN` |
| `customer` | `123456` | Nguyễn Văn Khách | `ROLE_CUSTOMER` |
| `reception` | `123456` | Lê Thị Lễ Tân | `ROLE_RECEPTIONIST` |
| `housekeeping` | `123456` | Trần Văn Buồng Phòng | `ROLE_HOUSEKEEPING` |
| `accountant` | `123456` | Phạm Thu Kế Toán | `ROLE_ACCOUNTANT` |
| `waiter` | `123456` | Hoàng Phục Vụ | `ROLE_WAITER` |

---

## 3. Hướng dẫn chạy và kết nối

1. Mở **SQL Server Management Studio (SSMS)** hoặc **Azure Data Studio**.
2. Mở file [deluxe_db.sql](file:///c:/Users/Admin/Desktop/deluxe-master/Database/deluxe_db.sql).
3. Nhấn **Execute (F5)** để tự động tạo Database `deluxe_db`, các bảng, ràng buộc khóa ngoại, chỉ mục (Index) và nạp dữ liệu mẫu cho cả 5 Feature.
4. Cấu hình kết nối trong [application.properties](file:///c:/Users/Admin/Desktop/deluxe-master/backend/src/main/resources/application.properties):
   ```properties
   spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=deluxe_db;encrypt=true;trustServerCertificate=true
   spring.datasource.username=sa
   spring.datasource.password=sa
   ```
