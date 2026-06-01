package com.railway.reservation.repository;

import java.util.Optional;
import com.railway.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("""
SELECT COALESCE(
SUM(r.ticketPrice),0)
FROM Reservation r
""")
    Double getTotalRevenue();

    Optional<Reservation> findByPnr(String pnr);
}