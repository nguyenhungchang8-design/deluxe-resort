package com.deluxe.feature.feature4_housekeeping.controller;

import com.deluxe.entity.User;
import com.deluxe.feature.feature4_housekeeping.dto.AssignTaskRequest;
import com.deluxe.feature.feature4_housekeeping.dto.CreateTaskRequest;
import com.deluxe.feature.feature4_housekeeping.dto.HousekeepingStaffDto;
import com.deluxe.feature.feature4_housekeeping.dto.MaintenanceResponseDto;
import com.deluxe.feature.feature4_housekeeping.dto.ReportMaintenanceRequest;
import com.deluxe.feature.feature4_housekeeping.dto.RoomSimpleDto;
import com.deluxe.feature.feature4_housekeeping.dto.RoomToCleanDto;
import com.deluxe.feature.feature4_housekeeping.dto.UpdateStatusRequest;
import com.deluxe.feature.feature4_housekeeping.service.HousekeepingService;
import com.deluxe.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/housekeeping")
public class HousekeepingController {

    private final HousekeepingService housekeepingService;
    private final UserRepository userRepository;

    public HousekeepingController(HousekeepingService housekeepingService, UserRepository userRepository) {
        this.housekeepingService = housekeepingService;
        this.userRepository = userRepository;
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<RoomToCleanDto>> getTasks(
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) Boolean myTasks,
            Authentication auth) {

        Long effectiveAssignedTo = assignedTo;

        if (Boolean.TRUE.equals(myTasks) && auth != null) {
            User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
            if (currentUser != null) {
                effectiveAssignedTo = currentUser.getId();
            }
        }

        return ResponseEntity.ok(housekeepingService.getRoomsToClean(roomNumber, status, effectiveAssignedTo));
    }

    @GetMapping("/tasks/rooms-to-clean")
    public ResponseEntity<List<RoomToCleanDto>> getRoomsToClean(
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) Boolean myTasks,
            Authentication auth) {
        return getTasks(roomNumber, status, assignedTo, myTasks, auth);
    }

    @PostMapping("/tasks")
    public ResponseEntity<?> createTask(@RequestBody(required = false) CreateTaskRequest req, Authentication auth) {
        if (isHousekeepingOnly(auth)) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", "Nhân viên buồng phòng không có quyền tạo nhiệm vụ dọn phòng."));
        }
        try {
            if (req == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu yêu cầu không được để trống"));
            }
            return ResponseEntity.ok(housekeepingService.createCleaningTask(req));
        } catch (IllegalArgumentException | IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Yêu cầu không hợp lệ";
            return ResponseEntity.badRequest().body(Map.of("message", msg));
        }
    }

    @PostMapping("/tasks/assign")
    public ResponseEntity<?> assignTask(@RequestBody(required = false) AssignTaskRequest req, Authentication auth) {
        if (isHousekeepingOnly(auth)) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", "Nhân viên buồng phòng không có quyền phân công nhiệm vụ."));
        }
        try {
            if (req == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu phân công không được để trống"));
            }
            return ResponseEntity.ok(housekeepingService.assignTask(req));
        } catch (IllegalArgumentException | IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Yêu cầu không hợp lệ";
            return ResponseEntity.badRequest().body(Map.of("message", msg));
        }
    }

    @PutMapping("/tasks/{id}/accept")
    public ResponseEntity<?> acceptTask(
            @PathVariable Long id,
            @RequestParam(required = false) Long roomId,
            Authentication auth) {
        try {
            Long currentUserId = null;
            if (auth != null) {
                currentUserId = userRepository.findByUsername(auth.getName()).map(User::getId).orElse(null);
            }
            return ResponseEntity.ok(housekeepingService.acceptCleaningTask(id, currentUserId, roomId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Không thể nhận nhiệm vụ";
            return ResponseEntity.badRequest().body(Map.of("message", msg));
        }
    }

    @PutMapping("/tasks/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Long roomId,
            @RequestBody(required = false) UpdateStatusRequest req,
            Authentication auth) {
        try {
            if (req == null || req.getStatus() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Vui lòng chọn trạng thái mới hợp lệ (PENDING, IN_PROGRESS, DONE)"));
            }

            Long currentUserId = null;
            String userRole = null;
            if (auth != null) {
                User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
                if (currentUser != null) {
                    currentUserId = currentUser.getId();
                    userRole = currentUser.getRole();
                }
            }

            return ResponseEntity
                    .ok(housekeepingService.updateTaskStatus(id, roomId, req.getStatus(), currentUserId, userRole));
        } catch (IllegalArgumentException | IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Quy trình cập nhật trạng thái không hợp lệ";
            return ResponseEntity.badRequest().body(Map.of("message", msg));
        }
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<RoomSimpleDto>> getRooms() {
        return ResponseEntity.ok(housekeepingService.getAllRoomsForTaskCreation());
    }

    @GetMapping("/staff")
    public ResponseEntity<List<HousekeepingStaffDto>> getStaff() {
        return ResponseEntity.ok(housekeepingService.getHousekeepingStaff());
    }

    @PostMapping("/maintenance")
    public ResponseEntity<?> reportMaintenance(@RequestBody(required = false) ReportMaintenanceRequest req,
            Authentication auth) {
        try {
            Long currentUserId = null;
            if (auth != null) {
                User user = userRepository.findByUsername(auth.getName()).orElse(null);
                if (user != null) {
                    currentUserId = user.getId();
                }
            }
            return ResponseEntity.ok(housekeepingService.reportMaintenance(req, currentUserId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Không thể gửi báo cáo sự cố";
            return ResponseEntity.badRequest().body(Map.of("message", msg));
        }
    }

    @GetMapping("/maintenance")
    public ResponseEntity<List<MaintenanceResponseDto>> getMaintenanceRequests(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(housekeepingService.getMaintenanceRequests(roomId, status));
    }

    @PutMapping("/maintenance/{id}/status")
    public ResponseEntity<?> updateMaintenanceStatus(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        try {
            String status = body != null ? body.get("status") : null;
            if (status == null || status.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Vui lòng cung cấp trạng thái bảo trì hợp lệ"));
            }
            String userRole = null;
            if (auth != null) {
                userRole = userRepository.findByUsername(auth.getName()).map(User::getRole).orElse(null);
            }
            return ResponseEntity.ok(housekeepingService.updateMaintenanceStatus(id, status, userRole));
        } catch (IllegalArgumentException | IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Không thể cập nhật trạng thái sự cố";
            return ResponseEntity.badRequest().body(Map.of("message", msg));
        }
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id, @RequestParam(required = false) Long roomId,
            Authentication auth) {
        if (isHousekeepingOnly(auth)) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", "Nhân viên buồng phòng không có quyền xóa nhiệm vụ"));
        }
        try {
            housekeepingService.deleteCleaningTask(id, roomId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa nhiệm vụ dọn phòng thành công"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Không thể xóa nhiệm vụ";
            return ResponseEntity.badRequest().body(Map.of("message", msg));
        }
    }

    /**
     * Xoa su co bao tri
     */
    @DeleteMapping("/maintenance/{id}")
    public ResponseEntity<?> deleteMaintenance(@PathVariable Long id, Authentication auth) {
        if (isHousekeepingOnly(auth)) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", "Nhân viên buồng phòng không có quyền xóa sự cố bảo trì"));
        }
        try {
            housekeepingService.deleteMaintenanceRequest(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa sự cố bảo trì thành công"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Không thể xóa sự cố";
            return ResponseEntity.badRequest().body(Map.of("message", msg));
        }
    }

    private boolean isHousekeepingOnly(Authentication auth) {
        if (auth == null)
            return false;
        return userRepository.findByUsername(auth.getName())
                .map(u -> "ROLE_HOUSEKEEPING".equals(u.getRole()))
                .orElse(false);
    }
}
