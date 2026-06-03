package com.railway.reservation.controller;

import com.railway.reservation.entity.Passenger;
import com.railway.reservation.entity.Reservation;
import com.railway.reservation.entity.Train;
import com.railway.reservation.exception.NoSeatsAvailableException;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.repository.PassengerRepository;
import com.railway.reservation.repository.ReservationRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.service.FareCalculator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationRepository reservationRepository;
    private final TrainRepository trainRepository;
    private final PassengerRepository passengerRepository;
    private final FareCalculator fareCalculator;

    public ReservationController(
            ReservationRepository reservationRepository,
            TrainRepository trainRepository,
            PassengerRepository passengerRepository,
            FareCalculator fareCalculator) {

        this.reservationRepository = reservationRepository;
        this.trainRepository = trainRepository;
        this.passengerRepository = passengerRepository;
        this.fareCalculator = fareCalculator;
    }

    @PostMapping("/book")
    public Reservation bookTicket(
            @RequestParam Long trainId,
            @RequestParam Long passengerId,
            @RequestParam String trainClass) {

        Train train =
                trainRepository
                        .findById(trainId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Train",
                                        trainId
                                )
                        );

        Passenger passenger =
                passengerRepository
                        .findById(passengerId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Passenger",
                                        passengerId
                                )
                        );

        if (train.getAvailableSeats() <= 0) {
            throw new NoSeatsAvailableException(trainId);
        }

        Reservation reservation = new Reservation();

        String pnr =
                "PNR" + System.currentTimeMillis();

        long count =
                reservationRepository.count();

        String seatNo =
                "S1-" + String.format("%02d", count + 1);

        double fare =
                fareCalculator.calculateFare(train, trainClass);

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
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Reservation",
                                        id
                                )
                        );

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
