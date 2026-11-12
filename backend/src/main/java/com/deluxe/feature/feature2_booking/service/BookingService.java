package com.deluxe.feature.feature2_booking.service;

import com.deluxe.entity.Booking;
import com.deluxe.entity.Room;
import com.deluxe.entity.RoomType;
import com.deluxe.entity.User;
import com.deluxe.enums.BookingStatus;
import com.deluxe.enums.RoomStatus;
import com.deluxe.feature.feature2_booking.dto.BookingDto;
import com.deluxe.feature.feature2_booking.dto.BookingRequest;
import com.deluxe.feature.feature2_booking.dto.CustomerOptionDto;
import com.deluxe.repository.BookingRepository;
import com.deluxe.repository.RoomRepository;
import com.deluxe.repository.RoomTypeRepository;
import com.deluxe.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
            RoomRepository roomRepository,
            RoomTypeRepository roomTypeRepository,
            UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.userRepository = userRepository;
    }

    /** Lấy toàn bộ danh sách đặt phòng (ADMIN/RECEPTIONIST) */
    public List<BookingDto> getAll() {
        return bookingRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** Lấy đặt phòng theo id */
    public BookingDto getById(Long id) {
        return toDto(findBookingOrThrow(id));
    }

    /** Lấy chi tiết đặt phòng (Customer chỉ được xem đơn của chính mình) */
    public BookingDto getByIdForUser(Long id, Long requesterId, boolean isStaff) {
        Booking b = findBookingOrThrow(id);
        if (!isStaff && !b.getCustomerId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen xem booking nay");
        }
        return toDto(b);
    }

    /** Lấy đặt phòng của một khách hàng */
    public List<BookingDto> getByCustomer(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** Lấy đặt phòng theo trạng thái */
    public List<BookingDto> getByStatus(BookingStatus status) {
        return bookingRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** Danh sách cần check-in hôm nay */
    public List<BookingDto> getTodayCheckIn() {
        return bookingRepository.findTodayCheckIn(LocalDate.now()).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    /** Danh sách cần check-out hôm nay */
    public List<BookingDto> getTodayCheckOut() {
        return bookingRepository.findTodayCheckOut(LocalDate.now()).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    /** Danh sách khách hàng để lễ tân chọn khi tạo đặt phòng hộ (UC09) */
    public List<CustomerOptionDto> getCustomerOptions() {
        return userRepository.findAll().stream()
                .filter(u -> "ROLE_CUSTOMER".equals(u.getRole()))
                .map(u -> new CustomerOptionDto(
                        u.getId(),
                        u.getUsername(),
                        u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getUsername(),
                        u.getEmail()))
                .collect(Collectors.toList());
    }

    /**
     * Tạo đặt phòng mới.
     * - Kiểm tra khách hàng tồn tại
     * - Kiểm tra checkIn >= hôm nay, checkOut > checkIn, deposit >= 0
     * - Kiểm tra trạng thái phòng và chống trùng lịch
     * - Status ban đầu: PENDING; nếu phòng đang AVAILABLE thì chuyển sang BOOKED
     */
    @Transactional
    public BookingDto create(Long customerId, BookingRequest req) {
        if (customerId == null || !userRepository.existsById(customerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay khach hang");
        }
        validateDates(req.getCheckIn(), req.getCheckOut());
        if (req.getDeposit() != null && req.getDeposit().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tien coc khong duoc am");
        }

        Room room = findRoomOrThrow(req.getRoomId());
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Phong dang bao tri, khong the dat");
        }
        if (!req.getCheckIn().isAfter(LocalDate.now()) &&
                (room.getStatus() == RoomStatus.OCCUPIED || room.getStatus() == RoomStatus.CLEANING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phong hien dang co khach hoac dang don dep, chua the dat cho hom nay");
        }
        if (room.getStatus() == RoomStatus.OCCUPIED &&
                !bookingRepository.existsByRoomIdAndStatusIn(room.getId(), List.of(BookingStatus.CHECKED_IN))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phong hien dang co khach luu tru");
        }

        if (bookingRepository.existsConflict(room.getId(), req.getCheckIn(), req.getCheckOut(), -1L)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phong da co lich dat trong khoang thoi gian nay");
        }

        Booking b = new Booking();
        b.setCustomerId(customerId);
        b.setRoomId(room.getId());
        b.setCheckIn(req.getCheckIn());
        b.setCheckOut(req.getCheckOut());
        b.setStatus(BookingStatus.PENDING);
        b.setDeposit(req.getDeposit() != null ? req.getDeposit() : BigDecimal.ZERO);

        // Chỉ chuyển phòng sang BOOKED nếu hiện tại đang AVAILABLE (không ghi đè
        // OCCUPIED/CLEANING của lượt ở hiện tại)
        if (room.getStatus() == RoomStatus.AVAILABLE) {
            room.setStatus(RoomStatus.BOOKED);
            roomRepository.save(room);
        }

        return toDto(bookingRepository.save(b));
    }

    /**
     * Lễ tân xác nhận đặt phòng (PENDING -> CONFIRMED).
     */
    @Transactional
    public BookingDto confirm(Long id) {
        Booking b = findBookingOrThrow(id);
        if (b.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chi co the xac nhan booking dang PENDING");
        }
        Room room = findRoomOrThrow(b.getRoomId());
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Phong dang bao tri, khong the xac nhan booking");
        }
        if (bookingRepository.existsConflict(b.getRoomId(), b.getCheckIn(), b.getCheckOut(), b.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phong da co lich dat khac trung khoang thoi gian nay");
        }

        b.setStatus(BookingStatus.CONFIRMED);
        if (room.getStatus() == RoomStatus.AVAILABLE) {
            room.setStatus(RoomStatus.BOOKED);
            roomRepository.save(room);
        }
        return toDto(bookingRepository.save(b));
    }

    /**
     * Lễ tân thực hiện Check-in (CONFIRMED -> CHECKED_IN, Room -> OCCUPIED).
     */
    @Transactional
    public BookingDto checkIn(Long id) {
        Booking b = findBookingOrThrow(id);
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chi co the check-in booking da CONFIRMED");
        }

        Room room = findRoomOrThrow(b.getRoomId());
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Phong dang bao tri, khong the check-in");
        }
        if (room.getStatus() == RoomStatus.CLEANING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Phong dang don dep, chua san sang check-in");
        }
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phong hien dang co khach luu tru");
        }

        b.setStatus(BookingStatus.CHECKED_IN);
        bookingRepository.save(b);

        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        return toDto(b);
    }

    /**
     * Lễ tân thực hiện Check-out (CHECKED_IN -> CHECKED_OUT, Room -> CLEANING).
     */
    @Transactional
    public BookingDto checkOut(Long id) {
        Booking b = findBookingOrThrow(id);
        if (b.getStatus() != BookingStatus.CHECKED_IN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chi co the check-out booking dang CHECKED_IN");
        }
        b.setStatus(BookingStatus.CHECKED_OUT);
        bookingRepository.save(b);

        Room room = findRoomOrThrow(b.getRoomId());
        room.setStatus(RoomStatus.CLEANING);
        roomRepository.save(room);

        return toDto(b);
    }

    /**
     * Hủy đặt phòng (PENDING/CONFIRMED -> CANCELLED).
     * Customer chỉ hủy booking của chính mình.
     * Chỉ trả phòng về AVAILABLE nếu phòng đang BOOKED và không còn đơn
     * PENDING/CONFIRMED nào khác.
     */
    @Transactional
    public BookingDto cancel(Long id, Long requesterId, boolean isStaff) {
        Booking b = findBookingOrThrow(id);

        if (!isStaff && !b.getCustomerId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen huy booking nay");
        }

        if (b.getStatus() == BookingStatus.CHECKED_IN || b.getStatus() == BookingStatus.CHECKED_OUT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Khong the huy booking khi khach da nhan phong hoac da tra phong");
        }
        if (b.getStatus() == BookingStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking da bi huy truoc do");
        }

        b.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(b);

        Room room = findRoomOrThrow(b.getRoomId());
        if (room.getStatus() == RoomStatus.BOOKED &&
                !bookingRepository.existsByRoomIdAndStatusIn(
                        room.getId(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }

        return toDto(b);
    }

    // ---- Helper ----
    private Booking findBookingOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay booking"));
    }

    private Room findRoomOrThrow(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay phong"));
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngay nhan/tra phong khong hop le");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngay tra phong phai sau ngay nhan phong");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngay nhan phong khong duoc la ngay trong qua khu");
        }
    }

    BookingDto toDto(Booking b) {
        BookingDto dto = new BookingDto();
        dto.setId(b.getId());
        dto.setCustomerId(b.getCustomerId());
        dto.setRoomId(b.getRoomId());
        dto.setCheckIn(b.getCheckIn());
        dto.setCheckOut(b.getCheckOut());
        dto.setStatus(b.getStatus());
        dto.setDeposit(b.getDeposit());
        dto.setCreatedAt(b.getCreatedAt());

        long nights = 0;
        if (b.getCheckIn() != null && b.getCheckOut() != null) {
            nights = Math.max(1, ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut()));
        }
        dto.setNights(nights);

        // Enrich customer info
        userRepository.findById(b.getCustomerId()).ifPresent(u -> {
            dto.setCustomerName(
                    u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getUsername());
            dto.setCustomerEmail(u.getEmail());
        });

        // Enrich room & rate info
        roomRepository.findById(b.getRoomId()).ifPresent(room -> {
            dto.setRoomNumber(room.getRoomNumber());
            dto.setRoomTypeId(room.getRoomTypeId());
            dto.setRoomStatus(room.getStatus());
            roomTypeRepository.findById(room.getRoomTypeId()).ifPresent(rt -> {
                dto.setRoomTypeName(rt.getName());
                dto.setPricePerNight(rt.getPricePerNight());
                if (rt.getPricePerNight() != null && dto.getNights() != null) {
                    dto.setTotalAmount(rt.getPricePerNight().multiply(BigDecimal.valueOf(dto.getNights())));
                }
            });
        });

        return dto;
    }
}
