/* ====================================================================================
   DỰ ÁN: RESORT MANAGEMENT SYSTEM - GROUP 6
   HỆ QUẢN TRỊ CSDL: MICROSOFT SQL SERVER (T-SQL)
   Tập tin: Database/deluxe_db.sql
   
   Cách dùng:
     1. Mở SQL Server Management Studio (SSMS) hoặc Azure Data Studio.
     2. Mở file này và nhấn [F5] (hoặc nút Execute).
     3. Script sẽ tự động tạo Database, xóa bảng cũ (nếu có), tạo lại các bảng
        và nạp dữ liệu mẫu hoàn chỉnh cho cả 5 Feature.
   ==================================================================================== */

-- 1. TẠO HOẶC CHUYỂN ĐẾN DATABASE
USE master;
GO

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'deluxe_db')
BEGIN
    CREATE DATABASE deluxe_db;
    PRINT N'Đã tạo mới database deluxe_db.';
END
ELSE
BEGIN
    PRINT N'Database deluxe_db đã tồn tại, tiến hành sử dụng.';
END
GO

USE deluxe_db;
GO

-- 2. XÓA CÁC BẢNG CŨ (NẾU CÓ) THEO ĐÚNG THỨ TỰ RÀNG BUỘC KHÓA NGOẠI
IF OBJECT_ID('dbo.payments', 'U') IS NOT NULL DROP TABLE dbo.payments;
IF OBJECT_ID('dbo.deposit_refunds', 'U') IS NOT NULL DROP TABLE dbo.deposit_refunds;
IF OBJECT_ID('dbo.invoices', 'U') IS NOT NULL DROP TABLE dbo.invoices;
IF OBJECT_ID('dbo.order_items', 'U') IS NOT NULL DROP TABLE dbo.order_items;
IF OBJECT_ID('dbo.food_orders', 'U') IS NOT NULL DROP TABLE dbo.food_orders;
IF OBJECT_ID('dbo.menu_items', 'U') IS NOT NULL DROP TABLE dbo.menu_items;
IF OBJECT_ID('dbo.maintenance_requests', 'U') IS NOT NULL DROP TABLE dbo.maintenance_requests;
IF OBJECT_ID('dbo.cleaning_tasks', 'U') IS NOT NULL DROP TABLE dbo.cleaning_tasks;
IF OBJECT_ID('dbo.bookings', 'U') IS NOT NULL DROP TABLE dbo.bookings;
IF OBJECT_ID('dbo.rooms', 'U') IS NOT NULL DROP TABLE dbo.rooms;
IF OBJECT_ID('dbo.room_types', 'U') IS NOT NULL DROP TABLE dbo.room_types;
IF OBJECT_ID('dbo.users', 'U') IS NOT NULL DROP TABLE dbo.users;
GO

PRINT N'Bắt đầu tạo các bảng cho 5 Feature...';
GO

/* ====================================================================================
   FEATURE 1: AUTHENTICATION & USER MANAGEMENT (MEMBER 1 - Nguyễn Việt Hoàng)
   Actor chính: Customer, Receptionist, Housekeeping, Accountant, Waiter, Admin
   ==================================================================================== */

CREATE TABLE dbo.users (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    username    NVARCHAR(50)         NOT NULL,
    email       NVARCHAR(100)        NOT NULL,
    password    NVARCHAR(255)        NOT NULL,
    full_name   NVARCHAR(100)        NULL,
    role        NVARCHAR(30)         NOT NULL DEFAULT 'ROLE_CUSTOMER',
    active      BIT                  NOT NULL DEFAULT 1,
    CONSTRAINT PK_users PRIMARY KEY CLUSTERED (id ASC),
    CONSTRAINT UQ_users_username UNIQUE (username),
    CONSTRAINT UQ_users_email UNIQUE (email),
    CONSTRAINT CHK_users_role CHECK (role IN (
        'ROLE_ADMIN',
        'ROLE_CUSTOMER',
        'ROLE_RECEPTIONIST',
        'ROLE_HOUSEKEEPING',
        'ROLE_ACCOUNTANT',
        'ROLE_WAITER'
    ))
);
GO

/* ====================================================================================
   FEATURE 2: ROOM & BOOKING MANAGEMENT (MEMBER 2 - Vũ Trường Sơn)
   Actor chính: Customer, Receptionist
   ==================================================================================== */

-- Bảng loại phòng
CREATE TABLE dbo.room_types (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    name            NVARCHAR(100)        NOT NULL,
    price_per_night DECIMAL(19,2)        NOT NULL,
    capacity        INT                  NULL DEFAULT 2,
    description     NVARCHAR(500)        NULL,
    CONSTRAINT PK_room_types PRIMARY KEY CLUSTERED (id ASC)
);
GO

-- Bảng phòng
CREATE TABLE dbo.rooms (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    room_number  NVARCHAR(20)         NOT NULL,
    room_type_id BIGINT               NOT NULL,
    status       NVARCHAR(20)         NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT PK_rooms PRIMARY KEY CLUSTERED (id ASC),
    CONSTRAINT UQ_rooms_number UNIQUE (room_number),
    CONSTRAINT FK_rooms_room_type FOREIGN KEY (room_type_id) REFERENCES dbo.room_types(id),
    CONSTRAINT CHK_rooms_status CHECK (status IN (
        'AVAILABLE',    -- Phòng trống
        'BOOKED',       -- Đã được đặt
        'OCCUPIED',     -- Đang có khách ở
        'CLEANING',     -- Đang dọn dẹp
        'MAINTENANCE'   -- Đang hỏng / bảo trì
    ))
);
CREATE NONCLUSTERED INDEX IX_rooms_status ON dbo.rooms(status);
CREATE NONCLUSTERED INDEX IX_rooms_room_type ON dbo.rooms(room_type_id);
GO

-- Bảng đơn đặt phòng
CREATE TABLE dbo.bookings (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    customer_id BIGINT               NOT NULL,
    room_id     BIGINT               NOT NULL,
    check_in    DATE                 NOT NULL,
    check_out   DATE                 NOT NULL,
    status      NVARCHAR(20)         NOT NULL DEFAULT 'PENDING',
    deposit     DECIMAL(19,2)        NULL DEFAULT 0,
    created_at  DATETIME2            NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT PK_bookings PRIMARY KEY CLUSTERED (id ASC),
    CONSTRAINT FK_bookings_customer FOREIGN KEY (customer_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_bookings_room FOREIGN KEY (room_id) REFERENCES dbo.rooms(id),
    CONSTRAINT CHK_bookings_dates CHECK (check_out > check_in),
    CONSTRAINT CHK_bookings_status CHECK (status IN (
        'PENDING',      -- Đặt mới, chờ lễ tân duyệt
        'CONFIRMED',    -- Đã xác nhận
        'CHECKED_IN',   -- Đã nhận phòng
        'CHECKED_OUT',  -- Đã trả phòng
        'CANCELLED'     -- Đã hủy
    ))
);
CREATE NONCLUSTERED INDEX IX_bookings_customer ON dbo.bookings(customer_id);
CREATE NONCLUSTERED INDEX IX_bookings_room ON dbo.bookings(room_id);
CREATE NONCLUSTERED INDEX IX_bookings_dates ON dbo.bookings(check_in, check_out, status);
GO

/* ====================================================================================
   FEATURE 3: FOOD MENU & ORDERING (MEMBER 3 - Nguyễn Lâm Phúc)
   Actor chính: Customer, Waiter
   ==================================================================================== */

-- Bảng danh mục thực đơn món ăn
CREATE TABLE dbo.menu_items (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    name        NVARCHAR(100)        NOT NULL,
    price       DECIMAL(19,2)        NOT NULL,
    available   BIT                  NOT NULL DEFAULT 1,
    description NVARCHAR(500)        NULL,
    CONSTRAINT PK_menu_items PRIMARY KEY CLUSTERED (id ASC)
);
GO

-- Bảng đơn đặt món
CREATE TABLE dbo.food_orders (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    customer_id BIGINT               NOT NULL,
    room_id     BIGINT               NULL,
    status      NVARCHAR(20)         NOT NULL DEFAULT 'NEW',
    note        NVARCHAR(255)        NULL,
    created_at  DATETIME2            NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT PK_food_orders PRIMARY KEY CLUSTERED (id ASC),
    CONSTRAINT FK_food_orders_customer FOREIGN KEY (customer_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_food_orders_room FOREIGN KEY (room_id) REFERENCES dbo.rooms(id),
    CONSTRAINT CHK_food_orders_status CHECK (status IN (
        'NEW',          -- Mới tạo
        'CONFIRMED',    -- Phục vụ đã xác nhận
        'PREPARING',    -- Bếp đang nấu
        'SERVING',      -- Đang giao/phục vụ
        'COMPLETED',    -- Hoàn tất
        'CANCELLED'     -- Đã hủy
    ))
);
CREATE NONCLUSTERED INDEX IX_food_orders_customer ON dbo.food_orders(customer_id);
CREATE NONCLUSTERED INDEX IX_food_orders_room ON dbo.food_orders(room_id);
GO

-- Bảng chi tiết món trong đơn gọi món
CREATE TABLE dbo.order_items (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    order_id     BIGINT               NOT NULL,
    menu_item_id BIGINT               NOT NULL,
    quantity     INT                  NOT NULL,
    note         NVARCHAR(255)        NULL,
    CONSTRAINT PK_order_items PRIMARY KEY CLUSTERED (id ASC),
    CONSTRAINT FK_order_items_order FOREIGN KEY (order_id) REFERENCES dbo.food_orders(id) ON DELETE CASCADE,
    CONSTRAINT FK_order_items_menu_item FOREIGN KEY (menu_item_id) REFERENCES dbo.menu_items(id),
    CONSTRAINT CHK_order_items_qty CHECK (quantity > 0)
);
CREATE NONCLUSTERED INDEX IX_order_items_order ON dbo.order_items(order_id);
GO

/* ====================================================================================
   FEATURE 4: HOUSEKEEPING MANAGEMENT (MEMBER 4 - Lương Xuân Bách)
   Actor chính: Housekeeping, Receptionist
   ==================================================================================== */

-- Bảng nhiệm vụ dọn buồng phòng
CREATE TABLE dbo.cleaning_tasks (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    room_id     BIGINT               NOT NULL,
    assigned_to BIGINT               NULL,
    status      NVARCHAR(20)         NOT NULL DEFAULT 'PENDING',
    created_at  DATETIME2            NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT PK_cleaning_tasks PRIMARY KEY CLUSTERED (id ASC),
    CONSTRAINT FK_cleaning_tasks_room FOREIGN KEY (room_id) REFERENCES dbo.rooms(id),
    CONSTRAINT FK_cleaning_tasks_user FOREIGN KEY (assigned_to) REFERENCES dbo.users(id),
    CONSTRAINT CHK_cleaning_tasks_status CHECK (status IN (
        'PENDING',      -- Chờ dọn
        'IN_PROGRESS',  -- Đang dọn
        'DONE'          -- Đã hoàn thành (đồng bộ sang AVAILABLE)
    ))
);
CREATE NONCLUSTERED INDEX IX_cleaning_tasks_room ON dbo.cleaning_tasks(room_id);
CREATE NONCLUSTERED INDEX IX_cleaning_tasks_assigned ON dbo.cleaning_tasks(assigned_to);
GO

-- Bảng báo cáo sự cố & sửa chữa phòng
CREATE TABLE dbo.maintenance_requests (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    room_id     BIGINT               NOT NULL,
    reported_by BIGINT               NULL,
    description NVARCHAR(500)        NOT NULL,
    severity    NVARCHAR(20)         NULL DEFAULT 'MEDIUM',
    status      NVARCHAR(20)         NULL DEFAULT 'PENDING',
    created_at  DATETIME2            NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT PK_maintenance_requests PRIMARY KEY CLUSTERED (id ASC),
    CONSTRAINT FK_maintenance_requests_room FOREIGN KEY (room_id) REFERENCES dbo.rooms(id),
    CONSTRAINT FK_maintenance_requests_user FOREIGN KEY (reported_by) REFERENCES dbo.users(id),
    CONSTRAINT CHK_maintenance_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT CHK_maintenance_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'))
);
CREATE NONCLUSTERED INDEX IX_maintenance_requests_room ON dbo.maintenance_requests(room_id);
GO

/* ====================================================================================
   FEATURE 5: INVOICE, PAYMENT & REPORTS (MEMBER 5 - Nguyễn Hùng Cháng - Leader)
   Actor chính: Accountant, Receptionist
   ==================================================================================== */

-- Bảng hóa đơn
CREATE TABLE dbo.invoices (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    booking_id  BIGINT               NOT NULL,
    room_charge DECIMAL(19,2)        NOT NULL DEFAULT 0,
    food_charge DECIMAL(19,2)        NOT NULL DEFAULT 0,
    deposit     DECIMAL(19,2)        NOT NULL DEFAULT 0,
    total       DECIMAL(19,2)        NOT NULL DEFAULT 0,
    status      NVARCHAR(20)         NOT NULL DEFAULT 'DRAFT',
    created_at  DATETIME2            NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT PK_invoices PRIMARY KEY CLUSTERED (id ASC),
    CONSTRAINT FK_invoices_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings(id),
    CONSTRAINT CHK_invoices_status CHECK (status IN (
        'DRAFT',    -- Hóa đơn nháp
        'UNPAID',   -- Chưa thanh toán
        'PAID',     -- Đã thanh toán
        'REFUNDED'  -- Đã hoàn tiền
    ))
);
CREATE NONCLUSTERED INDEX IX_invoices_booking ON dbo.invoices(booking_id);
GO

-- Bảng ghi nhận giao dịch thanh toán
CREATE TABLE dbo.payments (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    invoice_id BIGINT               NOT NULL,
    amount     DECIMAL(19,2)        NOT NULL,
    method     NVARCHAR(50)         NULL,
    paid_at    DATETIME2            NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT PK_payments PRIMARY KEY CLUSTERED (id ASC),
    CONSTRAINT FK_payments_invoice FOREIGN KEY (invoice_id) REFERENCES dbo.invoices(id) ON DELETE CASCADE
);
CREATE NONCLUSTERED INDEX IX_payments_invoice ON dbo.payments(invoice_id);
GO

-- Bảng quản lý tiền cọc & hoàn tiền
CREATE TABLE dbo.deposit_refunds (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    booking_id BIGINT               NOT NULL,
    amount     DECIMAL(19,2)        NOT NULL,
    status     NVARCHAR(20)         NOT NULL DEFAULT 'HELD',
    CONSTRAINT PK_deposit_refunds PRIMARY KEY CLUSTERED (id ASC),
    CONSTRAINT FK_deposit_refunds_booking FOREIGN KEY (booking_id) REFERENCES dbo.bookings(id),
    CONSTRAINT CHK_deposit_refunds_status CHECK (status IN ('HELD', 'REFUNDED'))
);
GO

PRINT N'Tạo bảng thành công! Đang chèn dữ liệu mẫu...';
GO

/* ====================================================================================
   DỮ LIỆU MẪU (SEED DATA CHO CẢ 5 FEATURES)
   ==================================================================================== */

-- 1. Chèn người dùng mẫu cho đủ 5 vai trò (Password BCrypt: admin123 cho admin, 123456 cho tài khoản khác)
SET IDENTITY_INSERT dbo.users ON;
INSERT INTO dbo.users (id, username, email, password, full_name, role) VALUES
(1, N'admin',       N'admin@deluxe.com',        N'$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQabh42', N'Quản Trị Viên',        N'ROLE_ADMIN'),
(2, N'customer',    N'customer@deluxe.com',     N'$2a$10$wT0EmsmB5kS241.j44oK9ePj5u8t9UqBvB5JdE8v3lGvC.bQ9v5Gy', N'Nguyễn Văn Khách',     N'ROLE_CUSTOMER'),
(3, N'reception',   N'reception@deluxe.com',    N'$2a$10$wT0EmsmB5kS241.j44oK9ePj5u8t9UqBvB5JdE8v3lGvC.bQ9v5Gy', N'Lê Thị Lễ Tân',        N'ROLE_RECEPTIONIST'),
(4, N'housekeeping',N'housekeeping@deluxe.com', N'$2a$10$wT0EmsmB5kS241.j44oK9ePj5u8t9UqBvB5JdE8v3lGvC.bQ9v5Gy', N'Trần Văn Buồng Phòng', N'ROLE_HOUSEKEEPING'),
(5, N'accountant',  N'accountant@deluxe.com',   N'$2a$10$wT0EmsmB5kS241.j44oK9ePj5u8t9UqBvB5JdE8v3lGvC.bQ9v5Gy', N'Phạm Thu Kế Toán',     N'ROLE_ACCOUNTANT'),
(6, N'waiter',      N'waiter@deluxe.com',       N'$2a$10$wT0EmsmB5kS241.j44oK9ePj5u8t9UqBvB5JdE8v3lGvC.bQ9v5Gy', N'Hoàng Phục Vụ',        N'ROLE_WAITER');
SET IDENTITY_INSERT dbo.users OFF;
GO

-- 2. Hạng phòng & Danh sách phòng
SET IDENTITY_INSERT dbo.room_types ON;
INSERT INTO dbo.room_types (id, name, price_per_night, capacity, description) VALUES
(1, N'Standard Cozy Room',           900000.00, 2, N'Phòng tiêu chuẩn ấm cúng, đầy đủ tiện nghi, view sân vườn mát mẻ.'),
(2, N'Deluxe Sea View',             1500000.00, 3, N'Phòng cao cấp hướng biển trực diện, ban công riêng đón bình minh.'),
(3, N'Executive Ocean Suite',        2800000.00, 4, N'Phòng Suite sang trọng, phòng khách riêng, bồn tắm sục Jacuzzi view biển.'),
(4, N'Presidential Beachfront Villa', 5500000.00, 6, N'Biệt thự tổng thống có hồ bơi vô cực riêng và phục vụ 24/7.');
SET IDENTITY_INSERT dbo.room_types OFF;
GO

SET IDENTITY_INSERT dbo.rooms ON;
INSERT INTO dbo.rooms (id, room_number, room_type_id, status) VALUES
(1,  '101', 1, 'AVAILABLE'),
(2,  '102', 1, 'OCCUPIED'),
(3,  '103', 1, 'CLEANING'),
(4,  '201', 2, 'OCCUPIED'),
(5,  '202', 2, 'AVAILABLE'),
(6,  '203', 2, 'BOOKED'),
(7,  '301', 3, 'AVAILABLE'),
(8,  '302', 3, 'MAINTENANCE'),
(9,  '401', 4, 'AVAILABLE'),
(10, '402', 4, 'OCCUPIED');
SET IDENTITY_INSERT dbo.rooms OFF;
GO

-- 3. Thực đơn món ăn
SET IDENTITY_INSERT dbo.menu_items ON;
INSERT INTO dbo.menu_items (id, name, price, available, description) VALUES
(1, N'Phở Bò Wagyu Deluxe',          160000.00, 1, N'Phở bò tái nạm dùng thịt bò Wagyu thượng hạng, nước dùng hầm 24h.'),
(2, N'Cơm Chiên Hải Sản Hoàng Gia',  180000.00, 1, N'Cơm chiên tôm sú, mực tươi vịnh Hạ Long và cồi sò điệp vàng ươm.'),
(3, N'Bò Bít Tết Úc Sốt Tiêu Đen',   420000.00, 1, N'Thăn bò Úc nướng sốt tiêu Phú Quốc kèm khoai tây nghiền và măng tây.'),
(4, N'Tôm Hùm Bỏ Lò Phô Mai',        680000.00, 1, N'Nửa con tôm hùm đút lò cùng phô mai Mozzarella và bơ tỏi thơm ngậy.'),
(5, N'Lẩu Hải Sản Tomyum Hạ Long',   490000.00, 0, N'Nước lẩu chua cay đậm đà, tôm cua ghẹ tươi sống (Tạm hết hôm nay).'),
(6, N'Nước Cam Tươi Ép Nguyên Chất',  60000.00, 1, N'Cam sành mọng nước ép tươi nguyên chất không đường, giải nhiệt sảng khoái.'),
(7, N'Rượu Vang Đỏ Cabernet Sauv.',  850000.00, 1, N'Rượu vang đỏ nhập khẩu từ vùng Bordeaux Pháp.');
SET IDENTITY_INSERT dbo.menu_items OFF;
GO

-- 4. Đơn đặt phòng mẫu
SET IDENTITY_INSERT dbo.bookings ON;
INSERT INTO dbo.bookings (id, customer_id, room_id, check_in, check_out, status, deposit, created_at) VALUES
(1, 2, 2, CAST(DATEADD(day, -1, GETDATE()) AS DATE), CAST(DATEADD(day, 2, GETDATE()) AS DATE), 'CHECKED_IN',  500000.00, DATEADD(day, -3, SYSDATETIME())),
(2, 2, 4, CAST(GETDATE() AS DATE),                   CAST(DATEADD(day, 3, GETDATE()) AS DATE), 'CHECKED_IN', 1000000.00, DATEADD(day, -2, SYSDATETIME())),
(3, 2, 6, CAST(DATEADD(day, 1, GETDATE()) AS DATE),  CAST(DATEADD(day, 4, GETDATE()) AS DATE), 'CONFIRMED',   800000.00, DATEADD(day, -1, SYSDATETIME())),
(4, 2, 9, CAST(DATEADD(day, 5, GETDATE()) AS DATE),  CAST(DATEADD(day, 7, GETDATE()) AS DATE), 'PENDING',          0.00, SYSDATETIME());
SET IDENTITY_INSERT dbo.bookings OFF;
GO

-- 5. Đơn gọi món & Chi tiết món
SET IDENTITY_INSERT dbo.food_orders ON;
INSERT INTO dbo.food_orders (id, customer_id, room_id, status, note, created_at) VALUES
(1, 2, 2, 'SERVING', N'Giao tận phòng 102 trước 19h00', DATEADD(hour, -1, SYSDATETIME()));
SET IDENTITY_INSERT dbo.food_orders OFF;

SET IDENTITY_INSERT dbo.order_items ON;
INSERT INTO dbo.order_items (id, order_id, menu_item_id, quantity, note) VALUES
(1, 1, 1, 2, N'Ít hành, nhiều ớt'),
(2, 1, 6, 2, N'Uống kèm đá riêng');
SET IDENTITY_INSERT dbo.order_items OFF;
GO

-- 6. Công việc buồng phòng & Báo cáo bảo trì
SET IDENTITY_INSERT dbo.cleaning_tasks ON;
INSERT INTO dbo.cleaning_tasks (id, room_id, assigned_to, status, created_at) VALUES
(1, 3, 4, 'IN_PROGRESS', DATEADD(hour, -2, SYSDATETIME())),
(2, 4, 4, 'PENDING',     DATEADD(minute, -30, SYSDATETIME()));
SET IDENTITY_INSERT dbo.cleaning_tasks OFF;
GO

SET IDENTITY_INSERT dbo.maintenance_requests ON;
INSERT INTO dbo.maintenance_requests (id, room_id, reported_by, description, severity, status, created_at) VALUES
(1, 8, 4, N'Máy điều hòa nhiệt độ không lạnh và có tiếng ồn bất thường ở dàn lạnh.', 'HIGH', 'IN_PROGRESS', DATEADD(day, -1, SYSDATETIME()));
SET IDENTITY_INSERT dbo.maintenance_requests OFF;
GO

-- 7. Hóa đơn, Thanh toán & Tiền cọc
SET IDENTITY_INSERT dbo.invoices ON;
INSERT INTO dbo.invoices (id, booking_id, room_charge, food_charge, deposit, total, status, created_at) VALUES
(1, 1, 1800000.00, 440000.00, 500000.00, 1740000.00, 'UNPAID', DATEADD(hour, -1, SYSDATETIME()));
SET IDENTITY_INSERT dbo.invoices OFF;
GO

SET IDENTITY_INSERT dbo.payments ON;
INSERT INTO dbo.payments (id, invoice_id, amount, method, paid_at) VALUES
(1, 1, 500000.00, N'Chuyển khoản (Đã cọc)', DATEADD(day, -1, SYSDATETIME()));
SET IDENTITY_INSERT dbo.payments OFF;
GO

SET IDENTITY_INSERT dbo.deposit_refunds ON;
INSERT INTO dbo.deposit_refunds (id, booking_id, amount, status) VALUES
(1, 1, 500000.00, 'HELD');
SET IDENTITY_INSERT dbo.deposit_refunds OFF;
GO

PRINT N'===================================================================';
PRINT N'ĐÃ HOÀN TẤT THIẾT LẬP CƠ SỞ DỮ LIỆU SQL SERVER: deluxe_db';
PRINT N'===================================================================';
GO

-- KIỂM TRA NHANH SỐ LƯỢNG BẢN GHI ĐÃ NẠP
SELECT 'users' AS TableName, COUNT(*) AS TotalRows FROM dbo.users
UNION ALL
SELECT 'room_types', COUNT(*) FROM dbo.room_types
UNION ALL
SELECT 'rooms', COUNT(*) FROM dbo.rooms
UNION ALL
SELECT 'bookings', COUNT(*) FROM dbo.bookings
UNION ALL
SELECT 'menu_items', COUNT(*) FROM dbo.menu_items
UNION ALL
SELECT 'food_orders', COUNT(*) FROM dbo.food_orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM dbo.order_items
UNION ALL
SELECT 'cleaning_tasks', COUNT(*) FROM dbo.cleaning_tasks
UNION ALL
SELECT 'maintenance_requests', COUNT(*) FROM dbo.maintenance_requests
UNION ALL
SELECT 'invoices', COUNT(*) FROM dbo.invoices
UNION ALL
SELECT 'payments', COUNT(*) FROM dbo.payments
UNION ALL
SELECT 'deposit_refunds', COUNT(*) FROM dbo.deposit_refunds;
GO
