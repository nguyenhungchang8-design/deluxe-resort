# Phan chia Feature (backend)

Backend to chuc theo chuan Spring Boot MVC ket hop gom nhom feature:
- **Dung chung toan du an:**
  - `com.deluxe.entity`: Chua toan bo Entity va Enum (`User`, `Room`, `RoomType`, `Booking`, `FoodOrder`, `MenuItem`, `OrderItem`, `CleaningTask`, `MaintenanceRequest`, `Invoice`, `Payment`, `DepositRefund`, va cac Enum trang thai).
  - `com.deluxe.repository`: Chua toan bo Spring Data JPA Repository.
  - `com.deluxe.config`: Chua `SecurityConfig`, `WebConfig`.
- **Theo tung Feature (`com.deluxe.feature.*`):** Moi feature chua `controller` - `service` - `dto`.
- **SQL tao DB:** `Database/deluxe_db.sql`.

| Feature | Package (`com.deluxe.feature.*`) | PIC | API |
|---|---|---|---|
| 1. Auth & User | `feature.feature1_auth` | Member 1 | `/api/auth/*` |
| 2. Room & Booking | `feature.feature2_booking` | Member 2 | `/api/room-types`, `/api/rooms`, `/api/bookings` |
| 3. Food Menu & Ordering | `feature.feature3_food` | Member 3 | `/api/menu`, `/api/orders`, `/api/order-items` |
| 4. Housekeeping | `feature.feature4_housekeeping` | Member 4 | `/api/housekeeping/tasks`, `/api/housekeeping/maintenance` |
| 5. Invoice, Payment & Reports | `feature.feature5_billing` | Member 5 | `/api/accounting/invoices`, `/payments`, `/deposits` |

## Viec can lam them cho tung feature (TODO)
- **F1:** UC04 cap nhat profile, UC05 doi mat khau, quan ly tai khoan nhan vien (admin).
- **F2:** tim phong trong theo ngay (chong trung lich), confirm/cancel, check-in/out, doi `Room.status`.
- **F3:** tao order kem danh sach `OrderItem`, tinh tien, Waiter doi `OrderStatus`, quy dinh huy order.
- **F4:** nhan/doi `CleaningStatus`, khi DONE dong bo `Room.status` -> AVAILABLE; bao cao hong -> MAINTENANCE.
- **F5:** tong hop tien phong + tien an, tao Invoice, ghi nhan Payment, hoan coc, bao cao doanh thu ngay/thang (`/api/accounting/reports/...`).

## Quy uoc chung
- Cac Entity va Repository nam chung tai `com.deluxe.entity` va `com.deluxe.repository`.
- Doi entity/enum dung chung phai bao ca nhom.
- Role: `ADMIN`, `CUSTOMER`, `RECEPTIONIST`, `HOUSEKEEPING`, `ACCOUNTANT`, `WAITER` (xem `SecurityConfig`).
- Phan quyen theo duong dan trong `SecurityConfig`, chi tiet hon bang `@PreAuthorize` tren method.
- Bang trong DB tu tao (`ddl-auto=update`). Can Lombok plugin trong IDE.
