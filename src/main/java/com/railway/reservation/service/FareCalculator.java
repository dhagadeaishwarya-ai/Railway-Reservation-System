package com.railway.reservation.service;

import com.railway.reservation.entity.Train;
import com.railway.reservation.exception.InvalidTrainClassException;
import org.springframework.stereotype.Service;

@Service
public class FareCalculator {

    public double calculateFare(Train train, String trainClass) {

        return switch (trainClass) {

            case "GENERAL" -> train.getDistanceKm() * 0.5;

            case "SLEEPER" -> train.getDistanceKm();

            case "AC_3" -> train.getDistanceKm() * 2;

            case "AC_2" -> train.getDistanceKm() * 3;

            case "BUSINESS" -> train.getDistanceKm() * 5;

            default -> throw new InvalidTrainClassException(trainClass);
        };
    }
}
