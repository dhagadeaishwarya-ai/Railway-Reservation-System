package com.railway.reservation.controller;

import com.railway.reservation.entity.Passenger;
import com.railway.reservation.repository.PassengerRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/passengers")
public class PassengerController {

    private final PassengerRepository passengerRepository;

    public PassengerController(PassengerRepository passengerRepository) {
        this.passengerRepository = passengerRepository;
    }

    @PostMapping
    public Passenger addPassenger(@RequestBody Passenger passenger) {
        return passengerRepository.save(passenger);
    }

    @GetMapping
    public List<Passenger> getAllPassengers() {
        return passengerRepository.findAll();
    }
}