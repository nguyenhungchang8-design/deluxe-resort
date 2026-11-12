package com.deluxe.feature.feature2_booking;

import com.deluxe.entity.Booking;
import com.deluxe.entity.Room;
import com.deluxe.entity.RoomType;
import com.deluxe.enums.BookingStatus;
import com.deluxe.enums.RoomStatus;
import com.deluxe.feature.feature2_booking.dto.RoomDto;
import com.deluxe.feature.feature2_booking.dto.RoomRequest;
import com.deluxe.feature.feature2_booking.service.RoomService;
import com.deluxe.repository.BookingRepository;
import com.deluxe.repository.RoomRepository;
import com.deluxe.repository.RoomTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private RoomService roomService;

    private RoomType standardType;
    private RoomType suiteType;
    private Room r101Available;
    private Room r102Cleaning;
    private Room r201OccupiedWithCheckIn;
    private Room r202OccupiedUntracked;
    private Room r301Maintenance;

    @BeforeEach
    void setUp() {
        standardType = new RoomType();
        standardType.setId(1L);
        standardType.setName("Standard");
        standardType.setPricePerNight(new BigDecimal("900000"));
        standardType.setCapacity(2);

        suiteType = new RoomType();
        suiteType.setId(2L);
        suiteType.setName("Suite");
        suiteType.setPricePerNight(new BigDecimal("2800000"));
        suiteType.setCapacity(4);

        r101Available = makeRoom(1L, "101", 1L, RoomStatus.AVAILABLE);
        r102Cleaning = makeRoom(2L, "102", 1L, RoomStatus.CLEANING);
        r201OccupiedWithCheckIn = makeRoom(3L, "201", 2L, RoomStatus.OCCUPIED);
        r202OccupiedUntracked = makeRoom(4L, "202", 2L, RoomStatus.OCCUPIED);
        r301Maintenance = makeRoom(5L, "301", 2L, RoomStatus.MAINTENANCE);
    }

    private Room makeRoom(Long id, String num, Long typeId, RoomStatus status) {
        Room r = new Room();
        r.setId(id);
        r.setRoomNumber(num);
        r.setRoomTypeId(typeId);
        r.setStatus(status);
        return r;
    }

    @Test
    void getFiltered_filtersByRoomTypeAndStatus() {
        when(roomTypeRepository.findAll()).thenReturn(List.of(standardType, suiteType));
        when(roomRepository.findAll()).thenReturn(List.of(r101Available, r102Cleaning, r201OccupiedWithCheckIn));

        List<RoomDto> result = roomService.getFiltered(1L, RoomStatus.AVAILABLE);
        assertEquals(1, result.size());
        assertEquals("101", result.get(0).getRoomNumber());
        assertEquals(new BigDecimal("900000"), result.get(0).getPricePerNight());
    }

    @Test
    void findAvailableRooms_invalidDates_throwsBadRequest() {
        LocalDate today = LocalDate.now();
        assertThrows(ResponseStatusException.class,
                () -> roomService.findAvailableRooms(today.minusDays(1), today.plusDays(1), null, null));
        assertThrows(ResponseStatusException.class,
                () -> roomService.findAvailableRooms(today.plusDays(2), today.plusDays(2), null, null));
        assertThrows(ResponseStatusException.class,
                () -> roomService.findAvailableRooms(null, today.plusDays(2), null, null));
    }

    @Test
    void findAvailableRooms_startingToday_excludesCleaningOccupiedMaintenanceAndConflicts() {
        LocalDate today = LocalDate.now();
        LocalDate out = today.plusDays(2);
        Room r103BookedNoConflict = makeRoom(6L, "103", 1L, RoomStatus.BOOKED);
        Room r104Conflicted = makeRoom(7L, "104", 1L, RoomStatus.BOOKED);

        when(roomTypeRepository.findAll()).thenReturn(List.of(standardType, suiteType));
        when(roomRepository.findAll()).thenReturn(List.of(
                r101Available, r102Cleaning, r201OccupiedWithCheckIn, r301Maintenance, r103BookedNoConflict, r104Conflicted
        ));
        when(bookingRepository.existsConflict(1L, today, out, -1L)).thenReturn(false);
        when(bookingRepository.existsConflict(6L, today, out, -1L)).thenReturn(false);
        when(bookingRepository.existsConflict(7L, today, out, -1L)).thenReturn(true);

        List<RoomDto> avail = roomService.findAvailableRooms(today, out, null, null);
        assertEquals(2, avail.size());
        assertEquals(List.of("101", "103"), avail.stream().map(RoomDto::getRoomNumber).toList());
    }

    @Test
    void findAvailableRooms_futureDates_allowsFutureCheckoutOccupiedAndFiltersByCapacity() {
        LocalDate futureIn = LocalDate.now().plusDays(5);
        LocalDate futureOut = LocalDate.now().plusDays(7);

        when(roomTypeRepository.findAll()).thenReturn(List.of(standardType, suiteType));
        when(roomRepository.findAll()).thenReturn(List.of(
                r101Available, r102Cleaning, r201OccupiedWithCheckIn, r202OccupiedUntracked, r301Maintenance
        ));
        when(bookingRepository.existsByRoomIdAndStatusIn(3L, List.of(BookingStatus.CHECKED_IN))).thenReturn(true);
        when(bookingRepository.existsByRoomIdAndStatusIn(4L, List.of(BookingStatus.CHECKED_IN))).thenReturn(false);
        when(bookingRepository.existsConflict(3L, futureIn, futureOut, -1L)).thenReturn(false);

        // capacity=3 excludes Standard (cap 2), so only r201OccupiedWithCheckIn (cap 4) remains
        List<RoomDto> avail = roomService.findAvailableRooms(futureIn, futureOut, null, 3);
        assertEquals(1, avail.size());
        assertEquals("201", avail.get(0).getRoomNumber());
    }

    @Test
    void updateStatus_toAvailableWhileCheckedIn_throwsConflict() {
        when(roomRepository.findById(3L)).thenReturn(Optional.of(r201OccupiedWithCheckIn));
        when(bookingRepository.existsByRoomIdAndStatusIn(3L, List.of(BookingStatus.CHECKED_IN))).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> roomService.updateStatus(3L, RoomStatus.AVAILABLE));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(roomRepository, never()).save(any());
    }

    @Test
    void updateStatus_toMaintenance_succeeds() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(r101Available));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(standardType));

        RoomDto dto = roomService.updateStatus(1L, RoomStatus.MAINTENANCE);
        assertEquals(RoomStatus.MAINTENANCE, dto.getStatus());
    }

    @Test
    void delete_whenBookingExists_throwsConflict() {
        when(roomRepository.existsById(1L)).thenReturn(true);
        when(bookingRepository.findByRoomId(1L)).thenReturn(List.of(new Booking()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> roomService.delete(1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(roomRepository, never()).deleteById(any());
    }

    @Test
    void create_duplicateRoomNumber_throwsBadRequest() {
        RoomRequest req = new RoomRequest();
        req.setRoomNumber(" 101 ");
        req.setRoomTypeId(1L);
        when(roomRepository.existsByRoomNumber("101")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> roomService.create(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}

