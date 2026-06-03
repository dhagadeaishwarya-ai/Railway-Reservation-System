package com.railway.reservation.exception;

public class InvalidTrainClassException extends RuntimeException {

    public InvalidTrainClassException(String trainClass) {
        super("Invalid train class: " + trainClass);
    }
}
