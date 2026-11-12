package com.deluxe.feature.feature4_housekeeping;

import com.deluxe.entity.CleaningTask;
import com.deluxe.entity.MaintenanceRequest;
import com.deluxe.entity.Room;
import com.deluxe.entity.RoomType;
import com.deluxe.entity.User;
import com.deluxe.enums.CleaningStatus;
import com.deluxe.enums.RoomStatus;
import com.deluxe.feature.feature4_housekeeping.dto.CreateTaskRequest;
import com.deluxe.feature.feature4_housekeeping.dto.MaintenanceResponseDto;
import com.deluxe.feature.feature4_housekeeping.dto.ReportMaintenanceRequest;
import com.deluxe.feature.feature4_housekeeping.dto.RoomSimpleDto;
import com.deluxe.feature.feature4_housekeeping.dto.RoomToCleanDto;
import com.deluxe.feature.feature4_housekeeping.service.HousekeepingService;
import com.deluxe.repository.CleaningTaskRepository;
import com.deluxe.repository.MaintenanceRequestRepository;
import com.deluxe.repository.RoomRepository;
import com.deluxe.repository.RoomTypeRepository;
import com.deluxe.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HousekeepingServiceTest {

    @Mock
    private CleaningTaskRepository cleaningTaskRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MaintenanceRequestRepository maintenanceRequestRepository;

    @InjectMocks
    private HousekeepingService housekeepingService;

    private Room room103;
    private Room room201;
    private RoomType type1;
    private RoomType type2;
    private CleaningTask taskPending;
    private CleaningTask taskInProgress;
    private CleaningTask taskDone;
    private User staff1;
    private User staff2;

    @BeforeEach
    void setUp() {
        type1 = RoomType.builder().id(1L).name("Standard Cozy Room").pricePerNight(new BigDecimal("900000")).build();
        type2 = RoomType.builder().id(2L).name("Deluxe Sea View").pricePerNight(new BigDecimal("1500000")).build();

        room103 = Room.builder().id(3L).roomNumber("103").roomTypeId(1L).status(RoomStatus.CLEANING).build();
        room201 = Room.builder().id(4L).roomNumber("201").roomTypeId(2L).status(RoomStatus.OCCUPIED).build();

        taskPending = CleaningTask.builder().id(1L).roomId(3L).assignedTo(4L).status(CleaningStatus.PENDING).createdAt(LocalDateTime.now()).build();
        taskInProgress = CleaningTask.builder().id(2L).roomId(3L).assignedTo(4L).status(CleaningStatus.IN_PROGRESS).createdAt(LocalDateTime.now()).build();
        taskDone = CleaningTask.builder().id(3L).roomId(3L).assignedTo(4L).status(CleaningStatus.DONE).createdAt(LocalDateTime.now()).build();

        staff1 = User.builder().id(4L).username("housekeeping1").fullName("Trần Văn Buồng Phòng 1")
                .role("ROLE_HOUSEKEEPING").build();
        staff2 = User.builder().id(5L).username("housekeeping2").fullName("Lê Thị Buồng Phòng 2")
                .role("ROLE_HOUSEKEEPING").build();
    }

    @Test
    void testGetRoomsToClean_FilterByAssignedTo() {
        when(roomRepository.findAll()).thenReturn(List.of(room103, room201));
        when(roomTypeRepository.findAll()).thenReturn(List.of(type1, type2));
        when(cleaningTaskRepository.findAll()).thenReturn(List.of(taskPending));
        when(userRepository.findAll()).thenReturn(List.of(staff1));

        List<RoomToCleanDto> staff1Tasks = housekeepingService.getRoomsToClean(null, null, 4L);
        assertEquals(1, staff1Tasks.size());
        assertEquals("103", staff1Tasks.get(0).getRoomNumber());
    }

    @Test
    void testUpdateStatus_ValidTransition_PendingToInProgress() {
        when(cleaningTaskRepository.findById(1L)).thenReturn(Optional.of(taskPending));
        when(cleaningTaskRepository.save(any(CleaningTask.class))).thenAnswer(i -> i.getArgument(0));
        when(roomRepository.findById(3L)).thenReturn(Optional.of(room103));
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(type1));
        when(userRepository.findById(4L)).thenReturn(Optional.of(staff1));

        RoomToCleanDto dto = housekeepingService.updateTaskStatus(1L, CleaningStatus.IN_PROGRESS, 4L, "ROLE_HOUSEKEEPING");

        assertNotNull(dto);
        assertEquals(CleaningStatus.IN_PROGRESS, dto.getCleaningStatus());
        assertEquals(CleaningStatus.IN_PROGRESS, taskPending.getStatus());
        assertEquals(RoomStatus.CLEANING, room103.getStatus());
    }

    @Test
    void testUpdateStatus_ValidTransition_InProgressToDone_SetsRoomAvailable() {
        when(cleaningTaskRepository.findById(2L)).thenReturn(Optional.of(taskInProgress));
        when(cleaningTaskRepository.save(any(CleaningTask.class))).thenAnswer(i -> i.getArgument(0));
        when(roomRepository.findById(3L)).thenReturn(Optional.of(room103));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(type1));
        when(userRepository.findById(4L)).thenReturn(Optional.of(staff1));
        when(maintenanceRequestRepository.findByRoomId(3L)).thenReturn(List.of());

        RoomToCleanDto dto = housekeepingService.updateTaskStatus(2L, CleaningStatus.DONE, 4L, "ROLE_HOUSEKEEPING");

        assertNotNull(dto);
        assertEquals(CleaningStatus.DONE, dto.getCleaningStatus());
        assertEquals(CleaningStatus.DONE, taskInProgress.getStatus());
        assertEquals(RoomStatus.AVAILABLE, room103.getStatus());
        verify(roomRepository, times(1)).save(room103);
    }

    @Test
    void testUpdateStatus_InvalidTransition_PendingToDone_ThrowsIllegalStateException() {
        when(cleaningTaskRepository.findById(1L)).thenReturn(Optional.of(taskPending));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                housekeepingService.updateTaskStatus(1L, CleaningStatus.DONE, 4L, "ROLE_HOUSEKEEPING"));

        assertTrue(ex.getMessage().contains("chỉ có thể chuyển sang 'Đang dọn (IN_PROGRESS)'"));
    }

    @Test
    void testUpdateStatus_InvalidTransition_InProgressToPending_ThrowsIllegalStateException() {
        when(cleaningTaskRepository.findById(2L)).thenReturn(Optional.of(taskInProgress));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                housekeepingService.updateTaskStatus(2L, CleaningStatus.PENDING, 4L, "ROLE_HOUSEKEEPING"));

        assertTrue(ex.getMessage().contains("chỉ có thể chuyển sang 'Đã dọn xong (DONE)'"));
    }

    @Test
    void testUpdateStatus_InvalidTransition_DoneToPending_ThrowsIllegalStateException() {
        when(cleaningTaskRepository.findById(3L)).thenReturn(Optional.of(taskDone));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                housekeepingService.updateTaskStatus(3L, CleaningStatus.PENDING, 4L, "ROLE_HOUSEKEEPING"));

        assertTrue(ex.getMessage().contains("không thể thay đổi trạng thái"));
    }

    @Test
    void testUpdateStatus_SameStatus_ThrowsIllegalArgumentException() {
        when(cleaningTaskRepository.findById(1L)).thenReturn(Optional.of(taskPending));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                housekeepingService.updateTaskStatus(1L, CleaningStatus.PENDING, 4L, "ROLE_HOUSEKEEPING"));

        assertTrue(ex.getMessage().contains("không cần cập nhật lại"));
    }

    @Test
    void testUpdateStatus_UnauthorizedStaff_ThrowsIllegalStateException() {
        when(cleaningTaskRepository.findById(1L)).thenReturn(Optional.of(taskPending)); // assignedTo = 4

        // Staff 5 tries to update staff 4's task
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                housekeepingService.updateTaskStatus(1L, CleaningStatus.IN_PROGRESS, 5L, "ROLE_HOUSEKEEPING"));

        assertTrue(ex.getMessage().contains("Bạn không được phép cập nhật nhiệm vụ"));
    }

    @Test
    void testCreateCleaningTask_ReceptionistAssignsStaff() {
        when(roomRepository.findById(3L)).thenReturn(Optional.of(room103));
        when(userRepository.findById(4L)).thenReturn(Optional.of(staff1));
        when(cleaningTaskRepository.findByRoomId(3L)).thenReturn(List.of());
        when(cleaningTaskRepository.save(any(CleaningTask.class))).thenAnswer(i -> {
            CleaningTask t = i.getArgument(0);
            t.setId(10L);
            return t;
        });
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(type1));

        CreateTaskRequest req = new CreateTaskRequest(3L, 4L, "Khách vừa check-out");
        RoomToCleanDto dto = housekeepingService.createCleaningTask(req);

        assertNotNull(dto);
        assertEquals("103", dto.getRoomNumber());
        assertEquals(CleaningStatus.PENDING, dto.getCleaningStatus());
        assertEquals(RoomStatus.CLEANING, room103.getStatus());
    }

    @Test
    void testGetAllRoomsForTaskCreation() {
        when(roomRepository.findAll()).thenReturn(List.of(room103, room201));
        when(roomTypeRepository.findAll()).thenReturn(List.of(type1, type2));

        List<RoomSimpleDto> rooms = housekeepingService.getAllRoomsForTaskCreation();
        assertEquals(2, rooms.size());
    }

    @Test
    void testReportMaintenance_HousekeepingReportsIssue_SetsRoomMaintenance() {
        when(roomRepository.findById(3L)).thenReturn(Optional.of(room103));
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenAnswer(i -> {
            MaintenanceRequest mr = i.getArgument(0);
            mr.setId(100L);
            return mr;
        });
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(type1));
        when(userRepository.findById(4L)).thenReturn(Optional.of(staff1));

        ReportMaintenanceRequest req = new ReportMaintenanceRequest(3L, "Máy lạnh rò rỉ nước", "HIGH");
        MaintenanceResponseDto dto = housekeepingService.reportMaintenance(req, 4L);

        assertNotNull(dto);
        assertEquals("103", dto.getRoomNumber());
        assertEquals("Máy lạnh rò rỉ nước", dto.getDescription());
        assertEquals("HIGH", dto.getSeverity());
        assertEquals("PENDING", dto.getStatus());
        assertEquals(RoomStatus.MAINTENANCE, room103.getStatus());
        verify(roomRepository, times(1)).save(room103);
    }

    @Test
    void testUpdateStatus_InProgressToDone_BlockedWhenRoomHasActiveMaintenance() {
        when(cleaningTaskRepository.findById(2L)).thenReturn(Optional.of(taskInProgress));
        when(roomRepository.findById(3L)).thenReturn(Optional.of(room103));

        MaintenanceRequest activeIssue = MaintenanceRequest.builder()
                .id(100L)
                .roomId(3L)
                .description("Vỡ ống nước")
                .status("PENDING")
                .build();
        when(maintenanceRequestRepository.findByRoomId(3L)).thenReturn(List.of(activeIssue));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                housekeepingService.updateTaskStatus(2L, CleaningStatus.DONE, 4L, "ROLE_HOUSEKEEPING"));

        assertTrue(ex.getMessage().contains("Không thể đánh dấu phòng 103 sẵn sàng đón khách (AVAILABLE) vì phòng đang có sự cố hư hỏng cần bảo trì"));
        // Đảm bảo không lưu trạng thái AVAILABLE cho phòng
        assertNotEquals(RoomStatus.AVAILABLE, room103.getStatus());
    }

    @Test
    void testUpdateMaintenanceStatus_Resolved_RestoresRoomToCleaning() {
        room103.setStatus(RoomStatus.MAINTENANCE);
        MaintenanceRequest issue = MaintenanceRequest.builder()
                .id(100L)
                .roomId(3L)
                .description("Máy lạnh rò rỉ nước")
                .status("PENDING")
                .build();

        when(maintenanceRequestRepository.findById(100L)).thenReturn(Optional.of(issue));
        when(roomRepository.findById(3L)).thenReturn(Optional.of(room103));
        when(maintenanceRequestRepository.findByRoomId(3L)).thenReturn(List.of(issue));
        when(cleaningTaskRepository.findByRoomId(3L)).thenReturn(List.of());
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(type1));

        MaintenanceResponseDto res = housekeepingService.updateMaintenanceStatus(100L, "RESOLVED");

        assertNotNull(res);
        assertEquals("RESOLVED", issue.getStatus());
        assertEquals(RoomStatus.CLEANING, room103.getStatus());
        verify(roomRepository, times(1)).save(room103);
        verify(cleaningTaskRepository, times(1)).save(any(CleaningTask.class));
    }

    @Test
    void testUpdateMaintenanceStatus_HousekeepingCannotSkipToResolved() {
        MaintenanceRequest issue = MaintenanceRequest.builder()
                .id(100L)
                .roomId(3L)
                .description("Hỏng điều hòa")
                .status("PENDING")
                .build();
        when(maintenanceRequestRepository.findById(100L)).thenReturn(Optional.of(issue));

        assertThrows(IllegalStateException.class, () ->
                housekeepingService.updateMaintenanceStatus(100L, "RESOLVED", "ROLE_HOUSEKEEPING"));
    }

    @Test
    void testCreateCleaningTask_OccupiedRoom_PreservesOccupiedStatus() {
        when(roomRepository.findById(4L)).thenReturn(Optional.of(room201));
        when(userRepository.findById(4L)).thenReturn(Optional.of(staff1));
        when(cleaningTaskRepository.findByRoomId(4L)).thenReturn(List.of());
        when(cleaningTaskRepository.save(any(CleaningTask.class))).thenAnswer(i -> {
            CleaningTask t = i.getArgument(0);
            t.setId(20L);
            return t;
        });
        when(roomTypeRepository.findById(2L)).thenReturn(Optional.of(type2));

        CreateTaskRequest req = new CreateTaskRequest(4L, 4L, "Dọn giữa kỳ");
        RoomToCleanDto dto = housekeepingService.createCleaningTask(req);

        assertEquals(CleaningStatus.PENDING, dto.getCleaningStatus());
        assertEquals(RoomStatus.OCCUPIED, room201.getStatus());
        verify(roomRepository, never()).save(room201);
    }

    @Test
    void testCreateCleaningTask_Unassigned_AndHousekeepingAccepts() {
        Room roomAvailable = Room.builder().id(5L).roomNumber("105").roomTypeId(1L).status(RoomStatus.AVAILABLE).build();
        when(roomRepository.findById(5L)).thenReturn(Optional.of(roomAvailable));
        when(cleaningTaskRepository.findByRoomId(5L)).thenReturn(List.of());
        when(cleaningTaskRepository.save(any(CleaningTask.class))).thenAnswer(i -> {
            CleaningTask t = i.getArgument(0);
            if (t.getId() == null) t.setId(30L);
            return t;
        });
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(type1));

        CreateTaskRequest req = new CreateTaskRequest(5L, null, null);
        RoomToCleanDto created = housekeepingService.createCleaningTask(req);

        assertNull(created.getAssignedTo());
        assertEquals(CleaningStatus.PENDING, created.getCleaningStatus());
        assertEquals(RoomStatus.CLEANING, roomAvailable.getStatus());

        CleaningTask savedTask = CleaningTask.builder().id(30L).roomId(5L).assignedTo(null).status(CleaningStatus.PENDING).build();
        when(cleaningTaskRepository.findById(30L)).thenReturn(Optional.of(savedTask));
        when(userRepository.findById(4L)).thenReturn(Optional.of(staff1));

        RoomToCleanDto accepted = housekeepingService.acceptCleaningTask(30L, 4L);
        assertEquals(4L, accepted.getAssignedTo());
        assertEquals(CleaningStatus.IN_PROGRESS, accepted.getCleaningStatus());
    }
}
