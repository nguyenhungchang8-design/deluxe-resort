package com.deluxe.feature.feature4_housekeeping.service;

import com.deluxe.entity.CleaningTask;
import com.deluxe.entity.MaintenanceRequest;
import com.deluxe.entity.Room;
import com.deluxe.entity.RoomType;
import com.deluxe.entity.User;
import com.deluxe.enums.CleaningStatus;
import com.deluxe.enums.RoomStatus;
import com.deluxe.feature.feature4_housekeeping.dto.*;
import com.deluxe.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class HousekeepingService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CleaningTaskRepository cleaningTaskRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final UserRepository userRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;

    public HousekeepingService(CleaningTaskRepository cleaningTaskRepository,
                               RoomRepository roomRepository,
                               RoomTypeRepository roomTypeRepository,
                               UserRepository userRepository,
                               MaintenanceRequestRepository maintenanceRequestRepository) {
        this.cleaningTaskRepository = cleaningTaskRepository;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.userRepository = userRepository;
        this.maintenanceRequestRepository = maintenanceRequestRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomToCleanDto> getRoomsToClean(String searchRoomNumber, String statusFilter, Long assignedToFilter) {
        List<Room> allRooms = roomRepository.findAll();
        List<CleaningTask> allTasks = cleaningTaskRepository.findAll();
        Map<Long, RoomType> typeMap = roomTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RoomType::getId, Function.identity(), (a, b) -> a));
        Map<Long, User> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        Map<Long, List<CleaningTask>> tasksByRoom = allTasks.stream()
                .sorted(Comparator.comparing(CleaningTask::getId).reversed())
                .collect(Collectors.groupingBy(CleaningTask::getRoomId));

        List<RoomToCleanDto> dtoList = new ArrayList<>();

        for (Room room : allRooms) {
            List<CleaningTask> roomTasks = tasksByRoom.getOrDefault(room.getId(), Collections.emptyList());
            CleaningTask latestActiveTask = roomTasks.stream()
                    .filter(t -> t.getStatus() == CleaningStatus.PENDING || t.getStatus() == CleaningStatus.IN_PROGRESS)
                    .findFirst()
                    .orElse(null);

            CleaningTask latestTask = latestActiveTask != null ? latestActiveTask : (roomTasks.isEmpty() ? null : roomTasks.get(0));

            boolean needsCleaning = room.getStatus() == RoomStatus.CLEANING || latestActiveTask != null;
            boolean hasAnyTask = latestTask != null;

            if (!needsCleaning && !hasAnyTask) {
                continue;
            }

            CleaningStatus cStatus;
            Long taskId = null;
            Long assignedTo = null;
            LocalDateTime createdAt = null;

            if (latestTask != null) {
                taskId = latestTask.getId();
                cStatus = latestTask.getStatus();
                assignedTo = latestTask.getAssignedTo();
                createdAt = latestTask.getCreatedAt();
            } else {
                cStatus = CleaningStatus.PENDING;
                createdAt = LocalDateTime.now();
            }

            RoomType roomType = typeMap.get(room.getRoomTypeId());
            String roomTypeName = roomType != null ? roomType.getName() : "Không xác định";

            String assignedToName = null;
            if (assignedTo != null && userMap.containsKey(assignedTo)) {
                User u = userMap.get(assignedTo);
                assignedToName = u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getUsername();
            }

            String formattedTime = createdAt != null ? createdAt.format(DATE_TIME_FORMATTER) : "";

            RoomToCleanDto dto = RoomToCleanDto.builder()
                    .roomId(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .roomTypeId(room.getRoomTypeId())
                    .roomTypeName(roomTypeName)
                    .roomStatus(room.getStatus())
                    .taskId(taskId)
                    .cleaningStatus(cStatus)
                    .assignedTo(assignedTo)
                    .assignedToName(assignedToName)
                    .createdAt(createdAt)
                    .formattedCreatedAt(formattedTime)
                    .build();

            dtoList.add(dto);
        }

        // Apply AssignedTo Filter (Housekeeping user only sees tasks assigned to themselves)
        if (assignedToFilter != null) {
            dtoList = dtoList.stream()
                    .filter(dto -> dto.getAssignedTo() != null && dto.getAssignedTo().equals(assignedToFilter))
                    .collect(Collectors.toList());
        }

        // Apply Status Filter
        String filter = statusFilter != null ? statusFilter.trim().toUpperCase() : "ALL_ACTIVE";
        List<RoomToCleanDto> filtered = dtoList.stream().filter(dto -> {
            if ("PENDING".equals(filter)) {
                return dto.getCleaningStatus() == CleaningStatus.PENDING;
            } else if ("IN_PROGRESS".equals(filter)) {
                return dto.getCleaningStatus() == CleaningStatus.IN_PROGRESS;
            } else if ("DONE".equals(filter)) {
                return dto.getCleaningStatus() == CleaningStatus.DONE;
            } else if ("CLEANING".equals(filter)) {
                return dto.getRoomStatus() == RoomStatus.CLEANING;
            } else if ("ALL".equals(filter)) {
                return true;
            } else { // ALL_ACTIVE
                return dto.getCleaningStatus() == CleaningStatus.PENDING ||
                       dto.getCleaningStatus() == CleaningStatus.IN_PROGRESS ||
                       dto.getRoomStatus() == RoomStatus.CLEANING;
            }
        }).collect(Collectors.toList());

        // Apply Room Number Search
        if (searchRoomNumber != null && !searchRoomNumber.trim().isEmpty()) {
            String query = searchRoomNumber.trim().toLowerCase();
            filtered = filtered.stream()
                    .filter(dto -> dto.getRoomNumber() != null && dto.getRoomNumber().toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }

        // Sort: PENDING first, then IN_PROGRESS, then DONE, then room number
        filtered.sort((a, b) -> {
            int scoreA = statusScore(a.getCleaningStatus());
            int scoreB = statusScore(b.getCleaningStatus());
            if (scoreA != scoreB) {
                return Integer.compare(scoreA, scoreB);
            }
            return Objects.toString(a.getRoomNumber(), "").compareToIgnoreCase(Objects.toString(b.getRoomNumber(), ""));
        });

        return filtered;
    }

    private int statusScore(CleaningStatus status) {
        if (status == CleaningStatus.PENDING) return 1;
        if (status == CleaningStatus.IN_PROGRESS) return 2;
        if (status == CleaningStatus.DONE) return 3;
        return 4;
    }

    /**
     * Receptionist tao nhiem vu don phong va phan cong nhan vien
     */
    public RoomToCleanDto createCleaningTask(CreateTaskRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Dữ liệu yêu cầu không được để trống");
        }
        if (req.getRoomId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn phòng cần dọn dẹp");
        }
        if (req.getAssignedTo() == null) {
            throw new IllegalArgumentException("Vui lòng chọn nhân viên buồng phòng để phân công");
        }

        Room room = roomRepository.findById(req.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng với ID: " + req.getRoomId()));

        User staff = userRepository.findById(req.getAssignedTo())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên với ID: " + req.getAssignedTo()));

        // Tim xem phong co nhiem vu nao dang ton tai chua hoan thanh khong
        List<CleaningTask> existingTasks = cleaningTaskRepository.findByRoomId(room.getId());
        CleaningTask activeTask = existingTasks.stream()
                .filter(t -> t.getStatus() == CleaningStatus.PENDING || t.getStatus() == CleaningStatus.IN_PROGRESS)
                .findFirst()
                .orElse(null);

        CleaningTask savedTask;
        if (activeTask != null) {
            activeTask.setAssignedTo(staff.getId());
            activeTask.setStatus(CleaningStatus.PENDING);
            activeTask.setCreatedAt(LocalDateTime.now());
            savedTask = cleaningTaskRepository.save(activeTask);
        } else {
            CleaningTask newTask = CleaningTask.builder()
                    .roomId(room.getId())
                    .assignedTo(staff.getId())
                    .status(CleaningStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            savedTask = cleaningTaskRepository.save(newTask);
        }

        // Cap nhat trang thai phong sang CLEANING (neu phong khong dang gap su co bao tri)
        if (room.getStatus() != RoomStatus.MAINTENANCE) {
            room.setStatus(RoomStatus.CLEANING);
            roomRepository.save(room);
        }

        return toDto(savedTask, room);
    }

    /**
     * Housekeeping nhan nhiem vu don phong (Accept Cleaning Task: PENDING -> IN_PROGRESS)
     */
    public RoomToCleanDto acceptCleaningTask(Long taskId, Long currentUserId) {
        return acceptCleaningTask(taskId, currentUserId, null);
    }

    public RoomToCleanDto acceptCleaningTask(Long taskId, Long currentUserId, Long roomId) {
        CleaningTask task = null;
        if (taskId != null && taskId > 0) {
            task = cleaningTaskRepository.findById(taskId).orElse(null);
        }

        if (task == null && roomId != null) {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng với ID: " + roomId));

            List<CleaningTask> existingTasks = cleaningTaskRepository.findByRoomId(roomId);
            task = existingTasks.stream()
                    .filter(t -> t.getStatus() == CleaningStatus.PENDING || t.getStatus() == CleaningStatus.IN_PROGRESS)
                    .findFirst()
                    .orElse(null);

            if (task == null) {
                task = CleaningTask.builder()
                        .roomId(room.getId())
                        .assignedTo(currentUserId)
                        .status(CleaningStatus.IN_PROGRESS)
                        .createdAt(LocalDateTime.now())
                        .build();
                task = cleaningTaskRepository.save(task);

                if (room.getStatus() != RoomStatus.MAINTENANCE) {
                    room.setStatus(RoomStatus.CLEANING);
                    roomRepository.save(room);
                }
                return toDto(task, room);
            }
        }

        if (task == null) {
            throw new IllegalArgumentException("Không tìm thấy nhiệm vụ với ID: " + taskId);
        }

        // Kiem tra quyen neu la Housekeeping
        if (task.getAssignedTo() != null && currentUserId != null && !task.getAssignedTo().equals(currentUserId)) {
            throw new IllegalStateException("Nhiệm vụ này đã được phân công cho nhân viên khác.");
        }

        if (task.getAssignedTo() == null && currentUserId != null) {
            task.setAssignedTo(currentUserId);
            cleaningTaskRepository.save(task);
        }

        return updateTaskStatus(task.getId(), CleaningStatus.IN_PROGRESS, currentUserId, "ROLE_HOUSEKEEPING");
    }

    public RoomToCleanDto updateTaskStatus(Long taskId, Long roomId, CleaningStatus newStatus, Long currentUserId, String userRole) {
        Long targetTaskId = taskId;
        if ((targetTaskId == null || targetTaskId <= 0) && roomId != null) {
            List<CleaningTask> existingTasks = cleaningTaskRepository.findByRoomId(roomId);
            CleaningTask activeTask = existingTasks.stream()
                    .filter(t -> t.getStatus() == CleaningStatus.PENDING || t.getStatus() == CleaningStatus.IN_PROGRESS)
                    .findFirst()
                    .orElse(null);
            if (activeTask != null) {
                targetTaskId = activeTask.getId();
            } else {
                // Neu chua co task nao duoc tao trong database cho phong nay, tao moi va cap nhat
                Room room = roomRepository.findById(roomId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng với ID: " + roomId));
                CleaningTask newTask = CleaningTask.builder()
                        .roomId(room.getId())
                        .assignedTo(currentUserId)
                        .status(newStatus)
                        .createdAt(LocalDateTime.now())
                        .build();
                newTask = cleaningTaskRepository.save(newTask);
                if (newStatus == CleaningStatus.DONE) {
                    room.setStatus(RoomStatus.READY);
                    roomRepository.save(room);
                } else if (newStatus == CleaningStatus.IN_PROGRESS) {
                    if (room.getStatus() != RoomStatus.CLEANING && room.getStatus() != RoomStatus.MAINTENANCE) {
                        room.setStatus(RoomStatus.CLEANING);
                        roomRepository.save(room);
                    }
                }
                return toDto(newTask, room);
            }
        }
        return updateTaskStatus(targetTaskId, newStatus, currentUserId, userRole);
    }

    /**
     * Cap nhat trang thai nhiem vu don phong (Update Cleaning Status)
     * Bat buoc theo dung quy trinh: PENDING -> IN_PROGRESS -> DONE
     * KHONG CHO PHEP DANH DAU PHONG SAN SANG (AVAILABLE) NEU PHONG VAN DANG CAN BAO TRI
     */
    public RoomToCleanDto updateTaskStatus(Long taskId, CleaningStatus newStatus, Long currentUserId, String userRole) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Vui lòng chọn trạng thái mới cần cập nhật.");
        }

        CleaningTask task = cleaningTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhiệm vụ dọn dẹp với ID: " + taskId));

        // Housekeeping chi duoc phep cap nhat nhiem vu cua chinh minh
        if ("ROLE_HOUSEKEEPING".equals(userRole) && task.getAssignedTo() != null && currentUserId != null) {
            if (!task.getAssignedTo().equals(currentUserId)) {
                throw new IllegalStateException("Bạn không được phép cập nhật nhiệm vụ được phân công cho nhân viên khác.");
            }
        }

        if (task.getAssignedTo() == null && currentUserId != null) {
            task.setAssignedTo(currentUserId);
        }

        CleaningStatus currentStatus = task.getStatus();

        if (currentStatus == newStatus) {
            throw new IllegalArgumentException("Nhiệm vụ hiện đã ở trạng thái " + currentStatus + ", không cần cập nhật lại.");
        }

        // KIEM TRA QUY TRINH: PENDING -> IN_PROGRESS hoac DONE (neu hoan thanh luon)
        if (currentStatus == CleaningStatus.PENDING) {
            if (newStatus != CleaningStatus.IN_PROGRESS && newStatus != CleaningStatus.DONE) {
                throw new IllegalStateException("Quy trình không hợp lệ: Nhiệm vụ đang ở trạng thái 'Chờ dọn', chỉ có thể chuyển sang 'Đang dọn' hoặc 'Đã hoàn thành'.");
            }
        } else if (currentStatus == CleaningStatus.IN_PROGRESS) {
            if (newStatus != CleaningStatus.DONE) {
                throw new IllegalStateException("Quy trình không hợp lệ: Nhiệm vụ đang ở trạng thái 'Đang dọn (IN_PROGRESS)', chỉ có thể chuyển sang 'Đã dọn xong (DONE)'. Không thể quay lại trạng thái 'Chờ dọn (PENDING)'.");
            }
        } else if (currentStatus == CleaningStatus.DONE) {
            throw new IllegalStateException("Quy trình không hợp lệ: Nhiệm vụ đã hoàn thành 'Đã dọn xong (DONE)', quy trình dọn dẹp đã hoàn tất và không thể thay đổi trạng thái.");
        }

        Room room = roomRepository.findById(task.getRoomId()).orElse(null);

        // QUY TAC: KHONG CHO PHEP DANH DAU PHONG SAN SANG (READY) NEU PHONG VAN DANG CAN BAO TRI
        if (newStatus == CleaningStatus.DONE && room != null) {
            List<MaintenanceRequest> activeMaintenance = maintenanceRequestRepository.findByRoomId(room.getId()).stream()
                    .filter(m -> "PENDING".equalsIgnoreCase(m.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(m.getStatus()))
                    .collect(Collectors.toList());

            if (!activeMaintenance.isEmpty() || room.getStatus() == RoomStatus.MAINTENANCE) {
                throw new IllegalStateException("Không thể đánh dấu phòng " + room.getRoomNumber() + " sẵn sàng đón khách (READY) vì phòng đang có sự cố hư hỏng cần bảo trì chưa được xử lý!");
            }
        }

        task.setStatus(newStatus);
        cleaningTaskRepository.save(task);

        if (room != null) {
            if (newStatus == CleaningStatus.DONE) {
                // Khi don xong va khong co su co bao tri, cap nhat phong thanh READY
                room.setStatus(RoomStatus.READY);
                roomRepository.save(room);
            } else if (newStatus == CleaningStatus.IN_PROGRESS) {
                // Khi Housekeeping bat dau don, cap nhat trang thai phong thanh CLEANING
                if (room.getStatus() != RoomStatus.CLEANING && room.getStatus() != RoomStatus.MAINTENANCE) {
                    room.setStatus(RoomStatus.CLEANING);
                    roomRepository.save(room);
                }
            }
        }

        return toDto(task, room);
    }

    public RoomToCleanDto updateTaskStatus(Long taskId, CleaningStatus newStatus) {
        return updateTaskStatus(taskId, newStatus, null, null);
    }

    public RoomToCleanDto assignTask(AssignTaskRequest req) {
        CreateTaskRequest createReq = new CreateTaskRequest(req.getRoomId(), req.getAssignedTo(), req.getNote());
        return createCleaningTask(createReq);
    }

    /**
     * Chuc nang Report Maintenance Issue: Housekeeping bao cao phong bi hu hong
     */
    public MaintenanceResponseDto reportMaintenance(ReportMaintenanceRequest req, Long reportedBy) {
        if (req == null || req.getRoomId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn phòng đang gặp sự cố");
        }
        if (req.getDescription() == null || req.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung mô tả hư hỏng của sự cố");
        }

        Room room = roomRepository.findById(req.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng với ID: " + req.getRoomId()));

        String severity = req.getSeverity() != null && !req.getSeverity().isBlank() ? req.getSeverity().trim().toUpperCase() : "MEDIUM";
        if (!List.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(severity)) {
            severity = "MEDIUM";
        }

        MaintenanceRequest mr = MaintenanceRequest.builder()
                .roomId(room.getId())
                .reportedBy(reportedBy)
                .description(req.getDescription().trim())
                .severity(severity)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        MaintenanceRequest saved = maintenanceRequestRepository.save(mr);

        // Chuyen trang thai phong sang MAINTENANCE
        room.setStatus(RoomStatus.MAINTENANCE);
        roomRepository.save(room);

        return toMaintenanceDto(saved, room);
    }

    /**
     * Receptionist & Housekeeping xem danh sach bao cao su co
     */
    @Transactional(readOnly = true)
    public List<MaintenanceResponseDto> getMaintenanceRequests(Long roomId, String status) {
        List<MaintenanceRequest> requests = maintenanceRequestRepository.findAll();
        Map<Long, Room> roomMap = roomRepository.findAll().stream()
                .collect(Collectors.toMap(Room::getId, Function.identity(), (a, b) -> a));
        Map<Long, RoomType> typeMap = roomTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RoomType::getId, Function.identity(), (a, b) -> a));
        Map<Long, User> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        if (roomId != null) {
            requests = requests.stream().filter(r -> r.getRoomId().equals(roomId)).collect(Collectors.toList());
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            String filterStatus = status.trim().toUpperCase();
            requests = requests.stream().filter(r -> filterStatus.equalsIgnoreCase(r.getStatus())).collect(Collectors.toList());
        }

        // Sort descending by id (newest first)
        requests.sort(Comparator.comparing(MaintenanceRequest::getId).reversed());

        return requests.stream().map(r -> {
            Room room = roomMap.get(r.getRoomId());
            String roomNumber = room != null ? room.getRoomNumber() : "N/A";
            String typeName = "N/A";
            if (room != null && typeMap.containsKey(room.getRoomTypeId())) {
                typeName = typeMap.get(room.getRoomTypeId()).getName();
            }

            String reportedByName = "Nhân viên";
            if (r.getReportedBy() != null && userMap.containsKey(r.getReportedBy())) {
                User u = userMap.get(r.getReportedBy());
                reportedByName = u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getUsername();
            }

            String formattedTime = r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_TIME_FORMATTER) : "";

            return MaintenanceResponseDto.builder()
                    .id(r.getId())
                    .roomId(r.getRoomId())
                    .roomNumber(roomNumber)
                    .roomTypeName(typeName)
                    .reportedBy(r.getReportedBy())
                    .reportedByName(reportedByName)
                    .description(r.getDescription())
                    .severity(r.getSeverity())
                    .status(r.getStatus())
                    .createdAt(r.getCreatedAt())
                    .formattedCreatedAt(formattedTime)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Cap nhat trang thai su co bao tri (vd: Receptionist danh dau da sua xong RESOLVED / CLOSED)
     */
    public MaintenanceResponseDto updateMaintenanceStatus(Long id, String newStatus) {
        MaintenanceRequest mr = maintenanceRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy báo cáo sự cố với ID: " + id));

        mr.setStatus(newStatus != null ? newStatus.toUpperCase() : "RESOLVED");
        maintenanceRequestRepository.save(mr);

        Room room = roomRepository.findById(mr.getRoomId()).orElse(null);
        if (room != null && ("RESOLVED".equalsIgnoreCase(newStatus) || "CLOSED".equalsIgnoreCase(newStatus))) {
            // Kiem tra xem phong con su co nao khac chua giai quyet khong
            List<MaintenanceRequest> remainingActive = maintenanceRequestRepository.findByRoomId(room.getId()).stream()
                    .filter(m -> !m.getId().equals(id) && ("PENDING".equalsIgnoreCase(m.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(m.getStatus())))
                    .collect(Collectors.toList());

            if (remainingActive.isEmpty() && room.getStatus() == RoomStatus.MAINTENANCE) {
                // Neu het su co, tra ve trang thai CLEANING de don phong truoc khi don khach
                room.setStatus(RoomStatus.CLEANING);
                roomRepository.save(room);
            }
        }

        return toMaintenanceDto(mr, room);
    }

    /**
     * Xoa su co bao tri va tu dong cap nhat lai trang thai phong neu khong con su co nao khac
     */
    public void deleteMaintenanceRequest(Long id) {
        MaintenanceRequest mr = maintenanceRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự cố bảo trì với ID: " + id));

        Long roomId = mr.getRoomId();
        maintenanceRequestRepository.delete(mr);

        if (roomId != null) {
            Room room = roomRepository.findById(roomId).orElse(null);
            if (room != null && room.getStatus() == RoomStatus.MAINTENANCE) {
                // Kiem tra xem phong con su co nao khac chua giai quyet khong
                List<MaintenanceRequest> remainingActive = maintenanceRequestRepository.findByRoomId(roomId).stream()
                        .filter(m -> "PENDING".equalsIgnoreCase(m.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(m.getStatus()))
                        .collect(Collectors.toList());

                if (remainingActive.isEmpty()) {
                    // Neu khong con su co nao, kiem tra co nhiem vu don phong khong
                    List<CleaningTask> activeCleaning = cleaningTaskRepository.findByRoomId(roomId).stream()
                            .filter(t -> t.getStatus() == CleaningStatus.PENDING || t.getStatus() == CleaningStatus.IN_PROGRESS)
                            .collect(Collectors.toList());

                    if (!activeCleaning.isEmpty()) {
                        room.setStatus(RoomStatus.CLEANING);
                    } else {
                        room.setStatus(RoomStatus.AVAILABLE);
                    }
                    roomRepository.save(room);
                }
            }
        }
    }

    /**
     * Xoa nhiem vu don phong va dong bo trang thai phong ve AVAILABLE neu khong con nhiem vu active nao khac
     */
    public void deleteCleaningTask(Long id, Long roomId) {
        Long targetRoomId = roomId;
        if (id != null && id > 0) {
            CleaningTask task = cleaningTaskRepository.findById(id).orElse(null);
            if (task != null) {
                if (targetRoomId == null) {
                    targetRoomId = task.getRoomId();
                }
                cleaningTaskRepository.delete(task);
            }
        }

        if (targetRoomId != null) {
            Room room = roomRepository.findById(targetRoomId).orElse(null);
            if (room != null && room.getStatus() == RoomStatus.CLEANING) {
                List<CleaningTask> remaining = cleaningTaskRepository.findByRoomId(targetRoomId).stream()
                        .filter(t -> t.getStatus() == CleaningStatus.PENDING || t.getStatus() == CleaningStatus.IN_PROGRESS)
                        .collect(Collectors.toList());

                if (remaining.isEmpty()) {
                    room.setStatus(RoomStatus.AVAILABLE);
                    roomRepository.save(room);
                }
            }
        }
    }

    private MaintenanceResponseDto toMaintenanceDto(MaintenanceRequest mr, Room room) {
        String typeName = "N/A";
        if (room != null && room.getRoomTypeId() != null) {
            typeName = roomTypeRepository.findById(room.getRoomTypeId()).map(RoomType::getName).orElse("N/A");
        }
        String reportedName = "Nhân viên";
        if (mr.getReportedBy() != null) {
            reportedName = userRepository.findById(mr.getReportedBy())
                    .map(u -> u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getUsername())
                    .orElse("Nhân viên");
        }
        String formattedTime = mr.getCreatedAt() != null ? mr.getCreatedAt().format(DATE_TIME_FORMATTER) : "";

        return MaintenanceResponseDto.builder()
                .id(mr.getId())
                .roomId(mr.getRoomId())
                .roomNumber(room != null ? room.getRoomNumber() : "N/A")
                .roomTypeName(typeName)
                .reportedBy(mr.getReportedBy())
                .reportedByName(reportedName)
                .description(mr.getDescription())
                .severity(mr.getSeverity())
                .status(mr.getStatus())
                .createdAt(mr.getCreatedAt())
                .formattedCreatedAt(formattedTime)
                .build();
    }

    @Transactional(readOnly = true)
    public List<HousekeepingStaffDto> getHousekeepingStaff() {
        return userRepository.findAll().stream()
                .filter(u -> "ROLE_HOUSEKEEPING".equals(u.getRole()) || "ROLE_ADMIN".equals(u.getRole()))
                .map(u -> HousekeepingStaffDto.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .fullName(u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getUsername())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomSimpleDto> getAllRoomsForTaskCreation() {
        Map<Long, String> typeNames = roomTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RoomType::getId, RoomType::getName, (a, b) -> a));

        return roomRepository.findAll().stream()
                .sorted(Comparator.comparing(Room::getRoomNumber))
                .map(r -> RoomSimpleDto.builder()
                        .id(r.getId())
                        .roomNumber(r.getRoomNumber())
                        .roomTypeName(typeNames.getOrDefault(r.getRoomTypeId(), "Tiêu chuẩn"))
                        .status(r.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    private RoomToCleanDto toDto(CleaningTask task, Room room) {
        String typeName = "Không xác định";
        if (room != null && room.getRoomTypeId() != null) {
            typeName = roomTypeRepository.findById(room.getRoomTypeId())
                    .map(RoomType::getName)
                    .orElse("Không xác định");
        }

        String assignedName = null;
        if (task.getAssignedTo() != null) {
            assignedName = userRepository.findById(task.getAssignedTo())
                    .map(u -> u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getUsername())
                    .orElse(null);
        }

        String formattedTime = task.getCreatedAt() != null ? task.getCreatedAt().format(DATE_TIME_FORMATTER) : "";

        return RoomToCleanDto.builder()
                .roomId(room != null ? room.getId() : task.getRoomId())
                .roomNumber(room != null ? room.getRoomNumber() : null)
                .roomTypeId(room != null ? room.getRoomTypeId() : null)
                .roomTypeName(typeName)
                .roomStatus(room != null ? room.getStatus() : null)
                .taskId(task.getId())
                .cleaningStatus(task.getStatus())
                .assignedTo(task.getAssignedTo())
                .assignedToName(assignedName)
                .createdAt(task.getCreatedAt())
                .formattedCreatedAt(formattedTime)
                .build();
    }
}
