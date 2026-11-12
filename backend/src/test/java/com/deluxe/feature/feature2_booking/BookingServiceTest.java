package com.deluxe.feature.feature2_booking;

import com.deluxe.entity.Booking;
import com.deluxe.entity.Room;
import com.deluxe.entity.RoomType;
import com.deluxe.entity.User;
import com.deluxe.enums.BookingStatus;
import com.deluxe.enums.RoomStatus;
import com.deluxe.feature.feature2_booking.dto.BookingDto;
import com.deluxe.feature.feature2_booking.dto.BookingRequest;
import com.deluxe.feature.feature2_booking.dto.CustomerOptionDto;
import com.deluxe.feature.feature2_booking.service.BookingService;
import com.deluxe.repository.BookingRepository;
import com.deluxe.repository.RoomRepository;
import com.deluxe.repository.RoomTypeRepository;
import com.deluxe.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private User customer;
    private User staff;
    private RoomType roomType;
    private Room room;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(2L);
        customer.setUsername("customer");
        customer.setFullName("Nguyen Van A");
        customer.setEmail("a@example.com");
        customer.setRole("ROLE_CUSTOMER");

        staff = new User();
        staff.setId(3L);
        staff.setUsername("reception");
        staff.setFullName("Le Tan");
        staff.setRole("ROLE_RECEPTIONIST");

        roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("Deluxe");
        roomType.setPricePerNight(new BigDecimal("1500000"));
        roomType.setCapacity(3);

        room = new Room();
        room.setId(10L);
        room.setRoomNumber("201");
        room.setRoomTypeId(1L);
        room.setStatus(RoomStatus.AVAILABLE);
    }

    @Test
    void create_validAvailableRoom_savesPendingCalculatesTotalAndSetsRoomBooked() {
        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(4);

        BookingRequest req = new BookingRequest();
        req.setRoomId(10L);
        req.setCheckIn(in);
        req.setCheckOut(out);
        req.setDeposit(new BigDecimal("1000000"));

        when(userRepository.existsById(2L)).thenReturn(true);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsConflict(10L, in, out, -1L)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(99L);
            return b;
        });
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));

        BookingDto dto = bookingService.create(2L, req);
        assertEquals(99L, dto.getId());
        assertEquals(BookingStatus.PENDING, dto.getStatus());
        assertEquals(3L, dto.getNights());
        assertEquals(new BigDecimal("4500000"), dto.getTotalAmount());
        assertEquals(RoomStatus.BOOKED, room.getStatus());
        verify(roomRepository).save(room);
    }

    @Test
    void create_maintenanceRoom_throwsBadRequest() {
        room.setStatus(RoomStatus.MAINTENANCE);
        BookingRequest req = new BookingRequest();
        req.setRoomId(10L);
        req.setCheckIn(LocalDate.now().plusDays(1));
        req.setCheckOut(LocalDate.now().plusDays(3));

        when(userRepository.existsById(2L)).thenReturn(true);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.create(2L, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void create_futureDateOnOccupiedRoomWithCheckedIn_keepsOccupiedStatus() {
        room.setStatus(RoomStatus.OCCUPIED);
        LocalDate in = LocalDate.now().plusDays(5);
        LocalDate out = LocalDate.now().plusDays(7);
        BookingRequest req = new BookingRequest();
        req.setRoomId(10L);
        req.setCheckIn(in);
        req.setCheckOut(out);

        when(userRepository.existsById(2L)).thenReturn(true);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsByRoomIdAndStatusIn(10L, List.of(BookingStatus.CHECKED_IN))).thenReturn(true);
        when(bookingRepository.existsConflict(10L, in, out, -1L)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(55L);
            return b;
        });

        BookingDto dto = bookingService.create(2L, req);
        assertEquals(BookingStatus.PENDING, dto.getStatus());
        assertEquals(RoomStatus.OCCUPIED, room.getStatus());
        verify(roomRepository, never()).save(any());
    }

    @Test
    void create_conflictingDates_throwsConflict() {
        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(3);
        BookingRequest req = new BookingRequest();
        req.setRoomId(10L);
        req.setCheckIn(in);
        req.setCheckOut(out);

        when(userRepository.existsById(2L)).thenReturn(true);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsConflict(10L, in, out, -1L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.create(2L, req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void confirm_checkIn_checkOut_lifecycleTransitionsCorrectly() {
        Booking b = new Booking();
        b.setId(1L);
        b.setCustomerId(2L);
        b.setRoomId(10L);
        b.setCheckIn(LocalDate.now());
        b.setCheckOut(LocalDate.now().plusDays(2));
        b.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(b));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsConflict(10L, b.getCheckIn(), b.getCheckOut(), 1L)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));

        BookingDto confirmed = bookingService.confirm(1L);
        assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(RoomStatus.BOOKED, room.getStatus());

        BookingDto checkedIn = bookingService.checkIn(1L);
        assertEquals(BookingStatus.CHECKED_IN, checkedIn.getStatus());
        assertEquals(RoomStatus.OCCUPIED, room.getStatus());

        BookingDto checkedOut = bookingService.checkOut(1L);
        assertEquals(BookingStatus.CHECKED_OUT, checkedOut.getStatus());
        assertEquals(RoomStatus.CLEANING, room.getStatus());
    }

    @Test
    void checkIn_cleaningRoom_throwsBadRequest() {
        Booking b = new Booking();
        b.setId(1L);
        b.setCustomerId(2L);
        b.setRoomId(10L);
        b.setStatus(BookingStatus.CONFIRMED);
        room.setStatus(RoomStatus.CLEANING);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(b));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> bookingService.checkIn(1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void cancel_customerNotOwner_throwsForbidden() {
        Booking b = new Booking();
        b.setId(1L);
        b.setCustomerId(2L);
        b.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(b));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.cancel(1L, 999L, false));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void cancel_lastBookingOnBookedRoom_returnsRoomToAvailable() {
        Booking b = new Booking();
        b.setId(1L);
        b.setCustomerId(2L);
        b.setRoomId(10L);
        b.setStatus(BookingStatus.CONFIRMED);
        room.setStatus(RoomStatus.BOOKED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(b));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsByRoomIdAndStatusIn(10L, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDto cancelled = bookingService.cancel(1L, 2L, false);
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
        verify(roomRepository).save(room);
    }

    @Test
    void cancel_whenOtherFutureBookingExists_keepsRoomBooked() {
        Booking b = new Booking();
        b.setId(1L);
        b.setCustomerId(2L);
        b.setRoomId(10L);
        b.setStatus(BookingStatus.PENDING);
        room.setStatus(RoomStatus.BOOKED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(b));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsByRoomIdAndStatusIn(10L, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)))
                .thenReturn(true);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.cancel(1L, 3L, true);
        assertEquals(RoomStatus.BOOKED, room.getStatus());
        verify(roomRepository, never()).save(any());
    }

    @Test
    void getByIdForUser_nonOwnerCustomer_throwsForbidden() {
        Booking b = new Booking();
        b.setId(1L);
        b.setCustomerId(2L);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(b));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.getByIdForUser(1L, 77L, false));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getCustomerOptions_filtersOnlyRoleCustomer() {
        when(userRepository.findAll()).thenReturn(List.of(customer, staff));
        List<CustomerOptionDto> opts = bookingService.getCustomerOptions();
        assertEquals(1, opts.size());
        assertEquals("customer", opts.get(0).getUsername());
        assertEquals("Nguyen Van A", opts.get(0).getFullName());
    }
}

