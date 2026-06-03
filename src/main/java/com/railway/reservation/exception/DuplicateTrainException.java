package com.railway.reservation.exception;

public class DuplicateTrainException extends RuntimeException {

    public DuplicateTrainException(String trainNumber) {
        super("Train number already exists: " + trainNumber);
    }
}
