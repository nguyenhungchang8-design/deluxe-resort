package com.deluxe.feature.feature2_booking;

import com.deluxe.config.SecurityConfig;
import com.deluxe.entity.User;
import com.deluxe.enums.BookingStatus;
import com.deluxe.enums.RoomStatus;
import com.deluxe.feature.feature2_booking.controller.BookingController;
import com.deluxe.feature.feature2_booking.controller.Feature2ExceptionHandler;
import com.deluxe.feature.feature2_booking.controller.RoomController;
import com.deluxe.feature.feature2_booking.controller.RoomTypeController;
import com.deluxe.feature.feature2_booking.dto.BookingDto;
import com.deluxe.feature.feature2_booking.dto.RoomDto;
import com.deluxe.feature.feature2_booking.dto.RoomTypeDto;
import com.deluxe.feature.feature2_booking.service.BookingService;
import com.deluxe.feature.feature2_booking.service.RoomService;
import com.deluxe.feature.feature2_booking.service.RoomTypeService;
import com.deluxe.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        RoomTypeController.class,
        RoomController.class,
        BookingController.class,
        Feature2ExceptionHandler.class
})
@Import(SecurityConfig.class)
class Feature2ControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomTypeService roomTypeService;

    @MockBean
    private RoomService roomService;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void customer_canViewRoomsAndAvailable_butForbiddenOnAdminAndReceptionEndpoints() throws Exception {
        when(roomTypeService.getAll()).thenReturn(List.of(
                new RoomTypeDto(1L, "Standard", new BigDecimal("900000"), 2, "Co ban")
        ));
        when(roomService.findAvailableRooms(any(), any(), eq(1L), eq(2))).thenReturn(List.of(
                new RoomDto(1L, "101", 1L, "Standard", new BigDecimal("900000"), 2, "Co ban", RoomStatus.AVAILABLE)
        ));

        mockMvc.perform(get("/api/room-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Standard"));

        String checkIn = LocalDate.now().plusDays(1).toString();
        String checkOut = LocalDate.now().plusDays(2).toString();
        mockMvc.perform(get("/api/rooms/available")
                        .param("checkIn", checkIn)
                        .param("checkOut", checkOut)
                        .param("roomTypeId", "1")
                        .param("capacity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomNumber").value("101"));

        mockMvc.perform(post("/api/room-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"VIP\",\"pricePerNight\":5000000}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/bookings/1/confirm"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void customer_createBooking_alwaysUsesOwnUserIdEvenIfCustomerIdProvided() throws Exception {
        User cus = new User();
        cus.setId(2L);
        cus.setUsername("customer");
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(cus));

        BookingDto res = new BookingDto();
        res.setId(15L);
        res.setCustomerId(2L);
        res.setStatus(BookingStatus.PENDING);
        when(bookingService.create(eq(2L), any())).thenReturn(res);

        String checkIn = LocalDate.now().plusDays(2).toString();
        String checkOut = LocalDate.now().plusDays(4).toString();
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":999,\"roomId\":10,\"checkIn\":\"" + checkIn + "\",\"checkOut\":\"" + checkOut + "\",\"deposit\":500000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.customerId").value(2));

        verify(bookingService).create(eq(2L), any());
    }

    @Test
    @WithMockUser(username = "reception", roles = "RECEPTIONIST")
    void receptionist_canBookForCustomer_andUpdateRoomStatus_butCannotCreateRoom() throws Exception {
        User rec = new User();
        rec.setId(3L);
        rec.setUsername("reception");
        when(userRepository.findByUsername("reception")).thenReturn(Optional.of(rec));

        BookingDto res = new BookingDto();
        res.setId(20L);
        res.setCustomerId(7L);
        res.setStatus(BookingStatus.PENDING);
        when(bookingService.create(eq(7L), any())).thenReturn(res);

        String checkIn = LocalDate.now().plusDays(1).toString();
        String checkOut = LocalDate.now().plusDays(3).toString();
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":7,\"roomId\":10,\"checkIn\":\"" + checkIn + "\",\"checkOut\":\"" + checkOut + "\",\"deposit\":0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(7));

        when(roomService.updateStatus(10L, RoomStatus.MAINTENANCE))
                .thenReturn(new RoomDto(10L, "101", 1L, "Standard", RoomStatus.MAINTENANCE));
        mockMvc.perform(put("/api/rooms/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MAINTENANCE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));

        mockMvc.perform(put("/api/rooms/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNKNOWN_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomNumber\":\"999\",\"roomTypeId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void admin_validationFailure_returnsStructuredJsonMessage() throws Exception {
        mockMvc.perform(post("/api/room-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"pricePerNight\":-100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.errors.name").exists());
    }
}

