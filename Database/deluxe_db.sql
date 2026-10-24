/* =====================================================================
   Resort Management System - SQL Server
   Chay file nay trong SSMS / Azure Data Studio (hoac sqlcmd) de tao
   database, bang va du lieu mau.

   Luu y: tai khoan dang nhap (admin, customer, reception, ...) KHONG seed
   o day, vi mat khau phai ma hoa BCrypt. Backend Spring Boot tu tao khi
   khoi dong lan dau (xem config/SecurityConfig.java).
   Ten bang/cot khop voi cac entity trong backend/.../model.
   ===================================================================== */

IF DB_ID('deluxe_db') IS NULL
    CREATE DATABASE deluxe_db;
GO

USE deluxe_db;
GO

/* ---------- Feature 1: Auth & User ---------- */
IF OBJECT_ID('users') IS NULL
CREATE TABLE users (
    id        BIGINT IDENTITY(1,1) PRIMARY KEY,
    username  NVARCHAR(50)  NOT NULL UNIQUE,
    email     NVARCHAR(100) NOT NULL UNIQUE,
    password  NVARCHAR(255) NOT NULL,
    full_name NVARCHAR(100) NULL,
    role      NVARCHAR(20)  NOT NULL DEFAULT 'ROLE_CUSTOMER'
        CHECK (role IN ('ROLE_ADMIN','ROLE_CUSTOMER','ROLE_RECEPTIONIST','ROLE_HOUSEKEEPING','ROLE_ACCOUNTANT','ROLE_WAITER'))
);
GO

/* ---------- Feature 2: Room & Booking ---------- */
IF OBJECT_ID('room_types') IS NULL
CREATE TABLE room_types (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(100) NOT NULL,
    price_per_night DECIMAL(19,2) NOT NULL,
    capacity        INT NULL,
    description     NVARCHAR(500) NULL
);
GO

IF OBJECT_ID('rooms') IS NULL
CREATE TABLE rooms (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    room_number  NVARCHAR(20) NOT NULL UNIQUE,
    room_type_id BIGINT NOT NULL REFERENCES room_types(id),
    status       NVARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (status IN ('AVAILABLE','BOOKED','OCCUPIED','CLEANING','MAINTENANCE'))
);
GO

IF OBJECT_ID('bookings') IS NULL
CREATE TABLE bookings (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES users(id),
    room_id     BIGINT NOT NULL REFERENCES rooms(id),
    check_in    DATE NOT NULL,
    check_out   DATE NOT NULL,
    status      NVARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED')),
    deposit     DECIMAL(19,2) NULL,
    created_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CHECK (check_out > check_in)
);
GO

/* ---------- Feature 3: Food Menu & Ordering ---------- */
IF OBJECT_ID('menu_items') IS NULL
CREATE TABLE menu_items (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100) NOT NULL,
    price       DECIMAL(19,2) NOT NULL,
    available   BIT NOT NULL DEFAULT 1,
    description NVARCHAR(500) NULL
);
GO

IF OBJECT_ID('food_orders') IS NULL
CREATE TABLE food_orders (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES users(id),
    room_id     BIGINT NULL REFERENCES rooms(id),
    status      NVARCHAR(20) NOT NULL DEFAULT 'NEW'
        CHECK (status IN ('NEW','CONFIRMED','PREPARING','SERVING','COMPLETED','CANCELLED')),
    note        NVARCHAR(255) NULL,
    created_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID('order_items') IS NULL
CREATE TABLE order_items (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id     BIGINT NOT NULL REFERENCES food_orders(id),
    menu_item_id BIGINT NOT NULL REFERENCES menu_items(id),
    quantity     INT NOT NULL CHECK (quantity > 0),
    note         NVARCHAR(255) NULL
);
GO

/* ---------- Feature 4: Housekeeping ---------- */
IF OBJECT_ID('cleaning_tasks') IS NULL
CREATE TABLE cleaning_tasks (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    room_id     BIGINT NOT NULL REFERENCES rooms(id),
    assigned_to BIGINT NULL REFERENCES users(id),
    status      NVARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','IN_PROGRESS','DONE')),
    created_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID('maintenance_requests') IS NULL
CREATE TABLE maintenance_requests (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    room_id     BIGINT NOT NULL REFERENCES rooms(id),
    reported_by BIGINT NULL REFERENCES users(id),
    description NVARCHAR(500) NOT NULL,
    severity    NVARCHAR(20) NULL,
    status      NVARCHAR(20) NULL,
    created_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

/* ---------- Feature 5: Invoice, Payment & Reports ---------- */
IF OBJECT_ID('invoices') IS NULL
CREATE TABLE invoices (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    booking_id  BIGINT NOT NULL REFERENCES bookings(id),
    room_charge DECIMAL(19,2) NOT NULL DEFAULT 0,
    food_charge DECIMAL(19,2) NOT NULL DEFAULT 0,
    deposit     DECIMAL(19,2) NOT NULL DEFAULT 0,
    total       DECIMAL(19,2) NOT NULL DEFAULT 0,
    status      NVARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT','UNPAID','PAID','REFUNDED')),
    created_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID('payments') IS NULL
CREATE TABLE payments (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    amount     DECIMAL(19,2) NOT NULL,
    method     NVARCHAR(30) NULL,
    paid_at    DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID('deposit_refunds') IS NULL
CREATE TABLE deposit_refunds (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    amount     DECIMAL(19,2) NOT NULL,
    status     NVARCHAR(20) NOT NULL DEFAULT 'HELD' CHECK (status IN ('HELD','REFUNDED'))
);
GO

/* ---------- Du lieu mau ---------- */
IF NOT EXISTS (SELECT 1 FROM room_types)
BEGIN
    INSERT INTO room_types (name, price_per_night, capacity, description) VALUES
        (N'Standard', 900000,  2, N'Phong tieu chuan'),
        (N'Deluxe',   1500000, 3, N'Phong cao cap'),
        (N'Suite',    2800000, 4, N'Phong suite');

    INSERT INTO rooms (room_number, room_type_id, status) VALUES
        ('101', 2, 'OCCUPIED'),
        ('102', 1, 'AVAILABLE'),
        ('201', 3, 'CLEANING'),
        ('202', 3, 'AVAILABLE'),
        ('305', 2, 'MAINTENANCE');
END
GO

IF NOT EXISTS (SELECT 1 FROM menu_items)
    INSERT INTO menu_items (name, price, available, description) VALUES
        (N'Pho bo',    90000,  1, N'Pho bo tai'),
        (N'Com chien', 75000,  1, N'Com chien hai san'),
        (N'Lau',       350000, 0, N'Lau thai (tam het)'),
        (N'Nuoc cam',  40000,  1, N'Nuoc cam ep');
GO
