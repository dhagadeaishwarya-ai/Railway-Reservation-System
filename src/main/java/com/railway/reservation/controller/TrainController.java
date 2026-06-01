package com.railway.reservation.controller;

import com.railway.reservation.entity.Train;
import com.railway.reservation.repository.TrainRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trains")
public class TrainController {

    private final TrainRepository trainRepository;

    public TrainController(TrainRepository trainRepository) {
        this.trainRepository = trainRepository;
    }

    @GetMapping
    public List<Train> getAllTrains() {
        return trainRepository.findAll();
    }

    @PostMapping
    public Train addTrain(@RequestBody Train train) {
        return trainRepository.save(train);
    }

    @GetMapping("/search")
    public List<Train> searchTrain(
            @RequestParam String source,
            @RequestParam String destination) {

        return trainRepository
                .findBySourceAndDestination(source, destination);
    }
}