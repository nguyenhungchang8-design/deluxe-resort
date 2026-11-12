package com.deluxe.repository;

import com.deluxe.entity.Booking;
import com.deluxe.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByOrderByCreatedAtDesc();
    List<Booking> findByCustomerId(Long customerId);
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Booking> findByRoomId(Long roomId);
    List<Booking> findByStatus(BookingStatus status);
    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);
    boolean existsByRoomIdAndStatusIn(Long roomId, Collection<BookingStatus> statuses);

    /**
     * Kiểm tra phòng có bị trùng lịch không (ngoại trừ bookingId đang xét khi cập nhật).
     * Hai khoảng [checkIn, checkOut) giao nhau khi: checkIn_mới < checkOut_cũ AND checkOut_mới > checkIn_cũ
     */
    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
           "WHERE b.roomId = :roomId " +
           "AND b.id <> :excludeId " +
           "AND b.status NOT IN (com.deluxe.enums.BookingStatus.CANCELLED, com.deluxe.enums.BookingStatus.CHECKED_OUT) " +
           "AND b.checkIn < :checkOut " +
           "AND b.checkOut > :checkIn")
    boolean existsConflict(@Param("roomId") Long roomId,
                           @Param("checkIn") LocalDate checkIn,
                           @Param("checkOut") LocalDate checkOut,
                           @Param("excludeId") Long excludeId);

    /** Danh sách đặt phòng hôm nay cần check-in (CONFIRMED và checkIn = hôm nay) */
    @Query("SELECT b FROM Booking b WHERE b.checkIn = :today AND b.status = com.deluxe.enums.BookingStatus.CONFIRMED")
    List<Booking> findTodayCheckIn(@Param("today") LocalDate today);

    /** Danh sách đang ở cần check-out hôm nay */
    @Query("SELECT b FROM Booking b WHERE b.checkOut = :today AND b.status = com.deluxe.enums.BookingStatus.CHECKED_IN")
    List<Booking> findTodayCheckOut(@Param("today") LocalDate today);

    long countByStatus(BookingStatus status);
}
