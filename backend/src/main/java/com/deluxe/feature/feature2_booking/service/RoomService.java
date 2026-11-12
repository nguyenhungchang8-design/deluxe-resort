package com.deluxe.feature.feature2_booking.service;

import com.deluxe.entity.Room;
import com.deluxe.entity.RoomType;
import com.deluxe.enums.BookingStatus;
import com.deluxe.enums.RoomStatus;
import com.deluxe.feature.feature2_booking.dto.RoomDto;
import com.deluxe.feature.feature2_booking.dto.RoomRequest;
import com.deluxe.repository.BookingRepository;
import com.deluxe.repository.RoomRepository;
import com.deluxe.repository.RoomTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingRepository bookingRepository;

    public RoomService(RoomRepository roomRepository,
                       RoomTypeRepository roomTypeRepository,
                       BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingRepository = bookingRepository;
    }

    /** Lấy toàn bộ danh sách phòng */
    public List<RoomDto> getAll() {
        return getFiltered(null, null);
    }

    /** Lấy danh sách phòng có lọc tùy chọn theo hạng phòng / trạng thái */
    public List<RoomDto> getFiltered(Long roomTypeId, RoomStatus status) {
        Map<Long, RoomType> typeMap = loadRoomTypeMap();
        return roomRepository.findAll().stream()
                .filter(r -> roomTypeId == null || roomTypeId.equals(r.getRoomTypeId()))
                .filter(r -> status == null || status == r.getStatus())
                .map(r -> toDto(r, typeMap.get(r.getRoomTypeId())))
                .collect(Collectors.toList());
    }

    /** Lấy phòng theo id */
    public RoomDto getById(Long id) {
        return toDto(findRoomOrThrow(id));
    }

    /** Lấy phòng theo trạng thái */
    public List<RoomDto> getByStatus(RoomStatus status) {
        Map<Long, RoomType> typeMap = loadRoomTypeMap();
        return roomRepository.findByStatus(status).stream()
                .map(r -> toDto(r, typeMap.get(r.getRoomTypeId())))
                .collect(Collectors.toList());
    }

    /**
     * Tìm phòng còn trống trong khoảng thời gian checkIn - checkOut
     * (chống trùng lịch với các booking chưa hủy / chưa check-out, hỗ trợ lọc thêm hạng phòng & sức chứa).
     */
    public List<RoomDto> findAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        return findAvailableRooms(checkIn, checkOut, null, null);
    }

    public List<RoomDto> findAvailableRooms(LocalDate checkIn, LocalDate checkOut,
                                            Long roomTypeId, Integer capacity) {
        if (checkIn == null || checkOut == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngay nhan va ngay tra phong khong duoc de trong");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngay nhan phong khong duoc la ngay trong qua khu");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngay tra phong phai sau ngay nhan phong");
        }

        Map<Long, RoomType> typeMap = loadRoomTypeMap();
        boolean startingToday = !checkIn.isAfter(LocalDate.now());

        return roomRepository.findAll().stream()
                .filter(r -> r.getStatus() != RoomStatus.MAINTENANCE)
                .filter(r -> {
                    if (startingToday) {
                        return r.getStatus() != RoomStatus.CLEANING && r.getStatus() != RoomStatus.OCCUPIED;
                    }
                    // Nếu đang OCCUPIED nhưng không có booking CHECKED_IN nào để xác định ngày trả dự kiến
                    if (r.getStatus() == RoomStatus.OCCUPIED &&
                            !bookingRepository.existsByRoomIdAndStatusIn(r.getId(), List.of(BookingStatus.CHECKED_IN))) {
                        return false;
                    }
                    return true;
                })
                .filter(r -> roomTypeId == null || roomTypeId.equals(r.getRoomTypeId()))
                .filter(r -> {
                    if (capacity == null) return true;
                    RoomType rt = typeMap.get(r.getRoomTypeId());
                    return rt != null && rt.getCapacity() != null && rt.getCapacity() >= capacity;
                })
                .filter(r -> !bookingRepository.existsConflict(r.getId(), checkIn, checkOut, -1L))
                .map(r -> toDto(r, typeMap.get(r.getRoomTypeId())))
                .collect(Collectors.toList());
    }

    /** Tạo mới phòng (chỉ ADMIN) */
    public RoomDto create(RoomRequest req) {
        String roomNumber = req.getRoomNumber().trim();
        if (roomRepository.existsByRoomNumber(roomNumber)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "So phong da ton tai");
        }
        findRoomTypeOrThrow(req.getRoomTypeId());
        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setRoomTypeId(req.getRoomTypeId());
        room.setStatus(req.getStatus() != null ? req.getStatus() : RoomStatus.AVAILABLE);
        return toDto(roomRepository.save(room));
    }

    /** Cập nhật thông tin phòng (chỉ ADMIN/RECEPTIONIST) */
    public RoomDto update(Long id, RoomRequest req) {
        Room room = findRoomOrThrow(id);
        String roomNumber = req.getRoomNumber().trim();
        if (roomRepository.existsByRoomNumberAndIdNot(roomNumber, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "So phong da ton tai");
        }
        findRoomTypeOrThrow(req.getRoomTypeId());
        if (req.getStatus() == RoomStatus.AVAILABLE &&
                bookingRepository.existsByRoomIdAndStatusIn(id, List.of(BookingStatus.CHECKED_IN))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phong dang co khach luu tru, vui long thuc hien Check-out truoc");
        }
        room.setRoomNumber(roomNumber);
        room.setRoomTypeId(req.getRoomTypeId());
        if (req.getStatus() != null) room.setStatus(req.getStatus());
        return toDto(roomRepository.save(room));
    }

    /** Thay đổi trạng thái phòng (ADMIN/RECEPTIONIST) */
    public RoomDto updateStatus(Long id, RoomStatus newStatus) {
        if (newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang thai phong khong hop le");
        }
        Room room = findRoomOrThrow(id);
        if (newStatus == RoomStatus.AVAILABLE &&
                bookingRepository.existsByRoomIdAndStatusIn(id, List.of(BookingStatus.CHECKED_IN))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phong dang co khach luu tru (CHECKED_IN), vui long thuc hien Check-out truoc");
        }
        room.setStatus(newStatus);
        return toDto(roomRepository.save(room));
    }

    /** Xóa phòng (chỉ ADMIN) */
    public void delete(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay phong");
        }
        if (!bookingRepository.findByRoomId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khong the xoa phong da co lich su dat phong");
        }
        roomRepository.deleteById(id);
    }

    // ---- Helper ----
    private Map<Long, RoomType> loadRoomTypeMap() {
        return roomTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RoomType::getId, Function.identity()));
    }

    private Room findRoomOrThrow(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay phong"));
    }

    private void findRoomTypeOrThrow(Long roomTypeId) {
        if (!roomTypeRepository.existsById(roomTypeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hang phong");
        }
    }

    RoomDto toDto(Room r) {
        RoomType rt = roomTypeRepository.findById(r.getRoomTypeId()).orElse(null);
        return toDto(r, rt);
    }

    private RoomDto toDto(Room r, RoomType rt) {
        return new RoomDto(
                r.getId(),
                r.getRoomNumber(),
                r.getRoomTypeId(),
                rt != null ? rt.getName() : null,
                rt != null ? rt.getPricePerNight() : null,
                rt != null ? rt.getCapacity() : null,
                rt != null ? rt.getDescription() : null,
                r.getStatus()
        );
    }
}
