package com.railway.reservation.controller;

import com.railway.reservation.repository.PassengerRepository;
import com.railway.reservation.repository.ReservationRepository;
import com.railway.reservation.repository.TrainRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class DashboardController {

    private final TrainRepository trainRepository;
    private final PassengerRepository passengerRepository;
    private final ReservationRepository reservationRepository;

    public DashboardController(
            TrainRepository trainRepository,
            PassengerRepository passengerRepository,
            ReservationRepository reservationRepository) {

        this.trainRepository = trainRepository;
        this.passengerRepository = passengerRepository;
        this.reservationRepository = reservationRepository;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "totalTrains",
                trainRepository.count()
        );

        data.put(
                "totalPassengers",
                passengerRepository.count()
        );

        data.put(
                "totalReservations",
                reservationRepository.count()
        );

        data.put(
                "totalRevenue",
                reservationRepository
                        .getTotalRevenue()
        );

        return data;


    }

}