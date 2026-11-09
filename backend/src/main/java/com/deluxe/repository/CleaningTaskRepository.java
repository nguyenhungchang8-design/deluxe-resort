package com.deluxe.repository;

import com.deluxe.enums.CleaningStatus;
import com.deluxe.entity.CleaningTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CleaningTaskRepository extends JpaRepository<CleaningTask, Long> {
    List<CleaningTask> findByStatus(CleaningStatus status);
    List<CleaningTask> findByRoomId(Long roomId);
    List<CleaningTask> findByAssignedTo(Long assignedTo);
}
