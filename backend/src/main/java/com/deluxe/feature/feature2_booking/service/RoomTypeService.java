package com.deluxe.feature.feature2_booking.service;

import com.deluxe.entity.RoomType;
import com.deluxe.feature.feature2_booking.dto.RoomTypeDto;
import com.deluxe.feature.feature2_booking.dto.RoomTypeRequest;
import com.deluxe.repository.RoomRepository;
import com.deluxe.repository.RoomTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository,
                           RoomRepository roomRepository) {
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
    }

    /** Lấy toàn bộ danh sách hạng phòng */
    public List<RoomTypeDto> getAll() {
        return roomTypeRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** Lấy hạng phòng theo id */
    public RoomTypeDto getById(Long id) {
        RoomType rt = roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hang phong"));
        return toDto(rt);
    }

    /** Tạo mới hạng phòng (chỉ ADMIN) */
    public RoomTypeDto create(RoomTypeRequest req) {
        String name = req.getName().trim();
        if (roomTypeRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ten hang phong da ton tai");
        }
        RoomType rt = new RoomType();
        rt.setName(name);
        rt.setPricePerNight(req.getPricePerNight());
        rt.setCapacity(req.getCapacity() != null ? req.getCapacity() : 2);
        rt.setDescription(req.getDescription() != null ? req.getDescription().trim() : null);
        return toDto(roomTypeRepository.save(rt));
    }

    /** Cập nhật hạng phòng (chỉ ADMIN) */
    public RoomTypeDto update(Long id, RoomTypeRequest req) {
        RoomType rt = roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hang phong"));
        String name = req.getName().trim();
        if (roomTypeRepository.existsByNameAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ten hang phong da ton tai");
        }
        rt.setName(name);
        rt.setPricePerNight(req.getPricePerNight());
        rt.setCapacity(req.getCapacity() != null ? req.getCapacity() : rt.getCapacity());
        rt.setDescription(req.getDescription() != null ? req.getDescription().trim() : null);
        return toDto(roomTypeRepository.save(rt));
    }

    /** Xóa hạng phòng (chỉ ADMIN) */
    public void delete(Long id) {
        if (!roomTypeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hang phong");
        }
        if (!roomRepository.findByRoomTypeId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khong the xoa hang phong dang co phong su dung");
        }
        roomTypeRepository.deleteById(id);
    }

    private RoomTypeDto toDto(RoomType rt) {
        return new RoomTypeDto(rt.getId(), rt.getName(), rt.getPricePerNight(),
                rt.getCapacity(), rt.getDescription());
    }
}
