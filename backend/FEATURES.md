# Phan chia Feature (backend)

Backend chia theo **feature**; trong moi feature theo mo hinh MVC: `model` - `repository` - `service` - `controller`.
Dung chung: `config/SecurityConfig`. SQL tao DB: `Database/deluxe_db.sql`.

| Feature | Package (com.deluxe.*) | PIC | API |
|---|---|---|---|
| 1. Auth & User | `feature1_auth` | Member 1 | `/api/auth/*` |
| 2. Room & Booking | `feature2_booking` | Member 2 | `/api/room-types`, `/api/rooms`, `/api/bookings` |
| 3. Food Menu & Ordering | `feature3_food` | Member 3 | `/api/menu`, `/api/orders`, `/api/order-items` |
| 4. Housekeeping | `feature4_housekeeping` | Member 4 | `/api/housekeeping/tasks`, `/api/housekeeping/maintenance` |
| 5. Invoice, Payment & Reports | `feature5_billing` | Member 5 | `/api/accounting/invoices`, `/payments`, `/deposits` |

## Viec can lam them cho tung feature (TODO)
- **F1:** UC04 cap nhat profile, UC05 doi mat khau, quan ly tai khoan nhan vien (admin).
- **F2:** tim phong trong theo ngay (chong trung lich), confirm/cancel, check-in/out, doi `Room.status`.
- **F3:** tao order kem danh sach `OrderItem`, tinh tien, Waiter doi `OrderStatus`, quy dinh huy order.
- **F4:** nhan/doi `CleaningStatus`, khi DONE dong bo `Room.status` -> AVAILABLE; bao cao hong -> MAINTENANCE.
- **F5:** tong hop tien phong + tien an, tao Invoice, ghi nhan Payment, hoan coc, bao cao doanh thu ngay/thang (`/api/accounting/reports/...`).

## Quy uoc chung
- Cac feature tham chieu nhau **chi bang id** (`roomId`, `bookingId`, `customerId`...), khong dung `@ManyToOne` cheo package.
- Enum trang thai dung chung: `RoomStatus` (booking) duoc F4 cap nhat, `BookingStatus`/`OrderStatus` duoc F5 doc.
  Doi enum phai bao ca nhom.
- Role: `ADMIN`, `CUSTOMER`, `RECEPTIONIST`, `HOUSEKEEPING`, `ACCOUNTANT`, `WAITER` (xem `SecurityConfig`).
- Phan quyen theo duong dan trong `SecurityConfig`, chi tiet hon bang `@PreAuthorize` tren method.
- Bang trong DB tu tao (`ddl-auto=update`). Can Lombok plugin trong IDE.



