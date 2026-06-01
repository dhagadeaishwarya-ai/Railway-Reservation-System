package com.railway.reservation.controller;

import com.railway.reservation.entity.Passenger;
import com.railway.reservation.entity.Reservation;
import com.railway.reservation.entity.Train;
import com.railway.reservation.repository.PassengerRepository;
import com.railway.reservation.repository.ReservationRepository;
import com.railway.reservation.repository.TrainRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationRepository reservationRepository;
    private final TrainRepository trainRepository;
    private final PassengerRepository passengerRepository;

    public ReservationController(
            ReservationRepository reservationRepository,
            TrainRepository trainRepository,
            PassengerRepository passengerRepository) {

        this.reservationRepository = reservationRepository;
        this.trainRepository = trainRepository;
        this.passengerRepository = passengerRepository;
    }

    @PostMapping("/book")
    public Reservation bookTicket(
            @RequestParam Long trainId,
            @RequestParam Long passengerId,
            @RequestParam String trainClass) {

        Train train =
                trainRepository.findById(trainId).orElseThrow();

        Passenger passenger =
                passengerRepository.findById(passengerId).orElseThrow();

        if (train.getAvailableSeats() <= 0) {
            throw new RuntimeException("No seats available");
        }

        Reservation reservation = new Reservation();

        String pnr =
                "PNR" + System.currentTimeMillis();

        long count =
                reservationRepository.count();

        String seatNo =
                "S1-" + String.format("%02d", count + 1);

        double fare = 0;

        switch (trainClass) {

            case "GENERAL":
                fare = train.getDistanceKm() * 0.5;
                break;

            case "SLEEPER":
                fare = train.getDistanceKm() * 1;
                break;

            case "AC_3":
                fare = train.getDistanceKm() * 2;
                break;

            case "AC_2":
                fare = train.getDistanceKm() * 3;
                break;

            case "BUSINESS":
                fare = train.getDistanceKm() * 5;
                break;
        }

        reservation.setPnr(pnr);
        reservation.setTrainClass(trainClass);
        reservation.setTicketPrice(fare);

        reservation.setSeatNumber(seatNo);

        reservation.setJourneyDate("2026-06-10");

        reservation.setStatus("CONFIRMED");

        reservation.setTrain(train);

        reservation.setPassenger(passenger);

        train.setAvailableSeats(
                train.getAvailableSeats() - 1
        );

        trainRepository.save(train);

        return reservationRepository.save(reservation);
    }

    @DeleteMapping("/{id}")
    public String cancelTicket(
            @PathVariable Long id) {

        Reservation reservation =
                reservationRepository
                        .findById(id)
                        .orElseThrow();

        Train train =
                reservation.getTrain();

        train.setAvailableSeats(
                train.getAvailableSeats() + 1
        );

        trainRepository.save(train);

        reservationRepository.deleteById(id);

        return "Reservation Cancelled Successfully";
    }
}