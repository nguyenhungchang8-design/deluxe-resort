package com.deluxe.feature.feature2_booking;

import com.deluxe.entity.Room;
import com.deluxe.entity.RoomType;
import com.deluxe.feature.feature2_booking.dto.RoomTypeDto;
import com.deluxe.feature.feature2_booking.dto.RoomTypeRequest;
import com.deluxe.feature.feature2_booking.service.RoomTypeService;
import com.deluxe.repository.RoomRepository;
import com.deluxe.repository.RoomTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomTypeServiceTest {

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomTypeService roomTypeService;

    @Test
    void getAll_returnsAllRoomTypes() {
        RoomType rt = new RoomType();
        rt.setId(1L);
        rt.setName("Deluxe");
        rt.setPricePerNight(new BigDecimal("1500000"));
        rt.setCapacity(3);
        rt.setDescription("Huong bien");

        when(roomTypeRepository.findAll()).thenReturn(List.of(rt));

        List<RoomTypeDto> result = roomTypeService.getAll();
        assertEquals(1, result.size());
        assertEquals("Deluxe", result.get(0).getName());
        assertEquals(new BigDecimal("1500000"), result.get(0).getPricePerNight());
        assertEquals(3, result.get(0).getCapacity());
    }

    @Test
    void create_validRequest_trimsAndSaves() {
        RoomTypeRequest req = new RoomTypeRequest();
        req.setName("  Presidential Suite  ");
        req.setPricePerNight(new BigDecimal("5500000"));
        req.setCapacity(4);
        req.setDescription("  VIP room  ");

        when(roomTypeRepository.existsByName("Presidential Suite")).thenReturn(false);
        when(roomTypeRepository.save(any(RoomType.class))).thenAnswer(inv -> {
            RoomType saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        RoomTypeDto created = roomTypeService.create(req);
        assertEquals(10L, created.getId());
        assertEquals("Presidential Suite", created.getName());
        assertEquals("VIP room", created.getDescription());
        assertEquals(4, created.getCapacity());
    }

    @Test
    void create_duplicateName_throwsBadRequest() {
        RoomTypeRequest req = new RoomTypeRequest();
        req.setName("Suite");
        req.setPricePerNight(new BigDecimal("2800000"));

        when(roomTypeRepository.existsByName("Suite")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> roomTypeService.create(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(roomTypeRepository, never()).save(any());
    }

    @Test
    void update_validRequest_updatesFields() {
        RoomType existing = new RoomType();
        existing.setId(2L);
        existing.setName("Deluxe");
        existing.setPricePerNight(new BigDecimal("1500000"));
        existing.setCapacity(3);

        RoomTypeRequest req = new RoomTypeRequest();
        req.setName("Deluxe Ocean");
        req.setPricePerNight(new BigDecimal("1800000"));
        req.setCapacity(4);
        req.setDescription("Updated");

        when(roomTypeRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(roomTypeRepository.existsByNameAndIdNot("Deluxe Ocean", 2L)).thenReturn(false);
        when(roomTypeRepository.save(any(RoomType.class))).thenAnswer(inv -> inv.getArgument(0));

        RoomTypeDto updated = roomTypeService.update(2L, req);
        assertEquals("Deluxe Ocean", updated.getName());
        assertEquals(new BigDecimal("1800000"), updated.getPricePerNight());
        assertEquals(4, updated.getCapacity());
    }

    @Test
    void delete_whenRoomsUseType_throwsConflict() {
        when(roomTypeRepository.existsById(1L)).thenReturn(true);
        Room r = new Room();
        r.setId(101L);
        r.setRoomTypeId(1L);
        when(roomRepository.findByRoomTypeId(1L)).thenReturn(List.of(r));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> roomTypeService.delete(1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(roomTypeRepository, never()).deleteById(any());
    }

    @Test
    void delete_whenUnused_deletesRoomType() {
        when(roomTypeRepository.existsById(5L)).thenReturn(true);
        when(roomRepository.findByRoomTypeId(5L)).thenReturn(List.of());

        roomTypeService.delete(5L);
        verify(roomTypeRepository).deleteById(5L);
    }
}

