/* ====================================================================================
   BẢN CẬP NHẬT DATABASE CHO FEATURE 4: HOUSEKEEPING & MAINTENANCE
   Tác giả: Lương Xuân Bách (Member 4)
   Mục đích:
     1. Bổ sung trạng thái 'READY' vào ràng buộc CHK_rooms_status của bảng dbo.rooms
        (dùng khi nhân viên buồng phòng xác nhận hoàn thành dọn phòng).
     2. Cập nhật lại mã hash BCrypt hợp lệ cho các tài khoản mẫu trong dbo.users
        (admin / admin123; customer, reception, housekeeping, accountant, waiter / 123456)
        nếu database đang dùng bản seed cũ chưa đăng nhập được.
   Lưu ý: File tách riêng khỏi deluxe_db.sql để tránh xung đột (conflict) khi gộp nhánh.
          An toàn để chạy lại nhiều lần (idempotent) trên Microsoft SQL Server.
   ==================================================================================== */

USE deluxe_db;
GO

-- 1. Cập nhật CHECK constraint cho cột status của bảng dbo.rooms (thêm 'READY')
IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'CHK_rooms_status'
      AND parent_object_id = OBJECT_ID(N'dbo.rooms')
)
BEGIN
    ALTER TABLE dbo.rooms DROP CONSTRAINT CHK_rooms_status;
END
GO

ALTER TABLE dbo.rooms WITH CHECK ADD CONSTRAINT CHK_rooms_status CHECK (status IN (
    'AVAILABLE',    -- Phòng trống
    'READY',        -- Sẵn sàng đón khách (Buồng phòng đã dọn xong)
    'BOOKED',       -- Đã được đặt
    'OCCUPIED',     -- Đang có khách ở
    'CLEANING',     -- Đang dọn dẹp
    'MAINTENANCE'   -- Đang hỏng / bảo trì
));
GO

-- 2. Đồng bộ lại hash mật khẩu BCrypt hợp lệ cho 6 tài khoản mẫu
--    admin -> admin123
UPDATE dbo.users
SET password = N'$2a$10$5zOBa9Opt.ZSbEoXJXDStO1fnlbkyEUerK3jl7qx6l2KTmLy0xh5G'
WHERE username = N'admin';

--    customer, reception, housekeeping, accountant, waiter -> 123456
UPDATE dbo.users
SET password = N'$2a$10$MKyIuX.srH1HUiNrbt4zhu8rOLZ0O/a8Lg2Uz8HXHZiWMYEIfvZGy'
WHERE username IN (N'customer', N'reception', N'housekeeping', N'accountant', N'waiter');
GO

-- 3. Thêm các tài khoản nhân viên buồng phòng mẫu để phân công nhiệm vụ
IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE username = N'housekeeping2')
BEGIN
    INSERT INTO dbo.users (username, email, password, full_name, role)
    VALUES (N'housekeeping2', N'housekeeping2@deluxe.com', N'$2a$10$MKyIuX.srH1HUiNrbt4zhu8rOLZ0O/a8Lg2Uz8HXHZiWMYEIfvZGy', N'Nguyễn Thị Hoa', N'ROLE_HOUSEKEEPING');
END

IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE username = N'housekeeping3')
BEGIN
    INSERT INTO dbo.users (username, email, password, full_name, role)
    VALUES (N'housekeeping3', N'housekeeping3@deluxe.com', N'$2a$10$MKyIuX.srH1HUiNrbt4zhu8rOLZ0O/a8Lg2Uz8HXHZiWMYEIfvZGy', N'Lê Văn Nam', N'ROLE_HOUSEKEEPING');
END
GO

PRINT N'Đã cập nhật database cho Feature 4 thành công!';
GO

