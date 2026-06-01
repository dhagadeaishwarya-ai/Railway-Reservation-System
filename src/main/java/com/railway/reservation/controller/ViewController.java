package com.railway.reservation.controller;

import com.railway.reservation.repository.PassengerRepository;
import com.railway.reservation.repository.ReservationRepository;
import com.railway.reservation.repository.TrainRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.railway.reservation.entity.Train;
import com.railway.reservation.entity.Passenger;
import org.springframework.web.bind.annotation.PathVariable;
import com.railway.reservation.entity.Reservation;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@Controller
public class ViewController {

    private final TrainRepository trainRepository;
    private final PassengerRepository passengerRepository;
    private final ReservationRepository reservationRepository;

    public ViewController(
            TrainRepository trainRepository,
            PassengerRepository passengerRepository,
            ReservationRepository reservationRepository) {

        this.trainRepository = trainRepository;
        this.passengerRepository = passengerRepository;
        this.reservationRepository = reservationRepository;
    }

    @GetMapping("/")
    public String dashboard(Model model) {

        model.addAttribute(
                "totalTrains",
                trainRepository.count()
        );

        model.addAttribute(
                "totalPassengers",
                passengerRepository.count()
        );

        model.addAttribute(
                "totalReservations",
                reservationRepository.count()
        );

        model.addAttribute(
                "totalRevenue",
                reservationRepository.getTotalRevenue()
        );

        long totalAvailableSeats =
                trainRepository.findAll()
                        .stream()
                        .mapToLong(Train::getAvailableSeats)
                        .sum();

        model.addAttribute(
                "availableSeats",
                totalAvailableSeats
        );

        return "dashboard";
    }
    

    @GetMapping("/logout")
    public String logout() {

        return "redirect:/login";
    }

    @GetMapping("/trains-page")
    public String trainsPage(Model model) {

        model.addAttribute(
                "trains",
                trainRepository.findAll()
        );

        model.addAttribute(
                "train",
                new com.railway.reservation.entity.Train()
        );

        return "trains";
    }

    @PostMapping("/save-train")
    public String saveTrain(
            Train train) {

        if(trainRepository.existsByTrainNumber(
                train.getTrainNumber())){

            return "redirect:/trains-page";
        }

        trainRepository.save(train);

        return "redirect:/trains-page";
    }

    @GetMapping("/passengers-page")
    public String passengersPage(Model model) {

        model.addAttribute(
                "passengers",
                passengerRepository.findAll()
        );

        model.addAttribute(
                "passenger",
                new Passenger()
        );

        return "passengers";
    }

    @PostMapping("/save-passenger")
    public String savePassenger(
            Passenger passenger) {

        passengerRepository.save(passenger);

        return "redirect:/passengers-page";
    }

    @GetMapping("/reservations-page")
    public String reservationsPage(Model model) {

        model.addAttribute(
                "reservations",
                reservationRepository.findAll()
        );

        return "reservations";
    }

    @GetMapping("/cancel-reservation/{id}")
    public String cancelReservation(
            @PathVariable Long id) {

        Reservation reservation =
                reservationRepository
                        .findById(id)
                        .orElseThrow();

        Train train = reservation.getTrain();

        train.setAvailableSeats(
                train.getAvailableSeats() + 1
        );

        trainRepository.save(train);

        reservationRepository.delete(reservation);

        return "redirect:/reservations-page";
    }

    @GetMapping("/search-page")
    public String searchPage() {
        return "search";
    }

    @GetMapping("/search-trains")
    public String searchTrains(
            @RequestParam String source,
            @RequestParam String destination,
            Model model) {

        List<Train> trains =
                trainRepository.findBySourceAndDestination(
                        source,
                        destination
                );

        if(trains.isEmpty()){

            model.addAttribute(
                    "error",
                    "No trains found for selected route"
            );
        }

        model.addAttribute(
                "trains",
                trains
        );

        return "search";
    }

    @GetMapping("/book-page")
    public String bookPage(Model model) {

        model.addAttribute(
                "trains",
                trainRepository.findAll()
        );

        model.addAttribute(
                "passengers",
                passengerRepository.findAll()
        );

        return "booking";
    }

    @PostMapping("/book-ticket-ui")
    public String bookTicketUI(
            @RequestParam Long trainId,
            @RequestParam Long passengerId,
            @RequestParam String trainClass,
            @RequestParam String journeyDate){

        Train train =
                trainRepository.findById(trainId)
                        .orElseThrow();

        Passenger passenger =
                passengerRepository.findById(passengerId)
                        .orElseThrow();

        if(train.getAvailableSeats() <= 0){
            throw new RuntimeException(
                    "No Seats Available");
        }

        Reservation reservation =
                new Reservation();

        String pnr =
                "PNR" + System.currentTimeMillis();

        long count =
                reservationRepository.count();

        String seatNo =
                "S1-" + String.format("%02d",
                        count + 1);

        double fare = 0;

        switch(trainClass){

            case "GENERAL":
                fare =
                        train.getDistanceKm() * 0.5;
                break;

            case "SLEEPER":
                fare =
                        train.getDistanceKm() * 1;
                break;

            case "AC_3":
                fare =
                        train.getDistanceKm() * 2;
                break;

            case "AC_2":
                fare =
                        train.getDistanceKm() * 3;
                break;

            case "BUSINESS":
                fare =
                        train.getDistanceKm() * 5;
                break;
        }

        reservation.setPnr(pnr);

        reservation.setTrainClass(
                trainClass);

        reservation.setTicketPrice(
                fare);

        reservation.setSeatNumber(
                seatNo);

        reservation.setJourneyDate(
                journeyDate);

        reservation.setStatus(
                "CONFIRMED");

        reservation.setTrain(train);

        reservation.setPassenger(
                passenger);

        train.setAvailableSeats(
                train.getAvailableSeats()-1);

        trainRepository.save(train);

        reservationRepository.save(
                reservation);

        return "redirect:/reservations-page";
    }

    @GetMapping("/pnr-page")
    public String pnrPage() {
        return "pnr";
    }

    @GetMapping("/search-pnr")
    public String searchPnr(
            @RequestParam String pnr,
            Model model) {

        Reservation reservation =
                reservationRepository
                        .findByPnr(pnr)
                        .orElse(null);

        if (reservation == null) {

            model.addAttribute(
                    "error",
                    "PNR Not Found"
            );

            return "pnr";
        }

        model.addAttribute(
                "reservation",
                reservation
        );

        return "pnr";
    }

    @GetMapping("/delete-train/{id}")
    public String deleteTrain(
            @PathVariable Long id) {

        trainRepository.deleteById(id);

        return "redirect:/trains-page";
    }

    @GetMapping("/delete-passenger/{id}")
    public String deletePassenger(
            @PathVariable Long id) {

        passengerRepository.deleteById(id);

        return "redirect:/passengers-page";
    }

    @GetMapping("/login")
    public String loginPage() {

        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            Model model) {

        if(username.equals("admin")
                && password.equals("admin123")) {

            return "redirect:/";
        }

        if(username.equals("user")
                && password.equals("user123")) {

            return "redirect:/search-page";
        }

        model.addAttribute(
                "error",
                "Invalid Username or Password"
        );

        return "login";
    }

}