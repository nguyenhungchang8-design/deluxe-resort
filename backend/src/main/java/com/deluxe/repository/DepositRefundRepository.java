package com.deluxe.repository;

import com.deluxe.entity.DepositRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepositRefundRepository extends JpaRepository<DepositRefund, Long> {
    List<DepositRefund> findByBookingId(Long bookingId);
}
