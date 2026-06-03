package com.railway.reservation.exception;

public class NoSeatsAvailableException extends RuntimeException {

    public NoSeatsAvailableException(Long trainId) {
        super("No seats available for train id: " + trainId);
    }
}
