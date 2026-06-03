package com.railway.reservation.controller;

import com.railway.reservation.repository.PassengerRepository;
import com.railway.reservation.repository.ReservationRepository;
import com.railway.reservation.repository.TrainRepository;
import com.railway.reservation.exception.DuplicateTrainException;
import com.railway.reservation.exception.InvalidTrainClassException;
import com.railway.reservation.exception.InvalidInputException;
import com.railway.reservation.exception.NoSeatsAvailableException;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.service.FareCalculator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.railway.reservation.entity.Train;
import com.railway.reservation.entity.Passenger;
import org.springframework.web.bind.annotation.PathVariable;
import com.railway.reservation.entity.Reservation;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ViewController {

    private final TrainRepository trainRepository;
    private final PassengerRepository passengerRepository;
    private final ReservationRepository reservationRepository;
    private final FareCalculator fareCalculator;

    public ViewController(
            TrainRepository trainRepository,
            PassengerRepository passengerRepository,
            ReservationRepository reservationRepository,
            FareCalculator fareCalculator) {

        this.trainRepository = trainRepository;
        this.passengerRepository = passengerRepository;
        this.reservationRepository = reservationRepository;
        this.fareCalculator = fareCalculator;
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
            Train train,
            RedirectAttributes redirectAttributes) {

        try {

            validateTrain(train);

            if(trainRepository.existsByTrainNumber(
                    train.getTrainNumber())){

                throw new DuplicateTrainException(
                        train.getTrainNumber());
            }

            trainRepository.save(train);

        } catch (DuplicateTrainException
                 | InvalidInputException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

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
            Passenger passenger,
            RedirectAttributes redirectAttributes) {

        try {

            validatePassenger(passenger);

            passengerRepository.save(passenger);

        } catch (InvalidInputException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

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
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            Reservation reservation =
                    reservationRepository
                            .findById(id)
                            .orElseThrow(
                                    () -> new ResourceNotFoundException(
                                            "Reservation",
                                            id
                                    )
                            );

            Train train = reservation.getTrain();

            train.setAvailableSeats(
                    train.getAvailableSeats() + 1
            );

            trainRepository.save(train);

            reservationRepository.delete(reservation);

        } catch (ResourceNotFoundException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

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
            @RequestParam String journeyDate,
            Model model){

        try {

            if (isBlank(journeyDate)) {
                throw new InvalidInputException(
                        "Journey date is required");
            }

            Train train =
                    trainRepository.findById(trainId)
                            .orElseThrow(
                                    () -> new ResourceNotFoundException(
                                            "Train",
                                            trainId
                                    )
                            );

            Passenger passenger =
                    passengerRepository.findById(passengerId)
                            .orElseThrow(
                                    () -> new ResourceNotFoundException(
                                            "Passenger",
                                            passengerId
                                    )
                            );

            if(train.getAvailableSeats() <= 0){
                throw new NoSeatsAvailableException(trainId);
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

            double fare =
                    fareCalculator.calculateFare(train, trainClass);

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

        } catch (InvalidInputException
                 | InvalidTrainClassException
                 | NoSeatsAvailableException
                 | ResourceNotFoundException exception) {

            addBookingData(model);

            model.addAttribute(
                    "error",
                    exception.getMessage()
            );

            return "booking";
        }

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
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            if(!trainRepository.existsById(id)){

                throw new ResourceNotFoundException(
                        "Train",
                        id
                );
            }

            trainRepository.deleteById(id);

        } catch (ResourceNotFoundException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );

        } catch (DataIntegrityViolationException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Train cannot be deleted because reservations exist"
            );
        }

        return "redirect:/trains-page";
    }

    @GetMapping("/delete-passenger/{id}")
    public String deletePassenger(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            if(!passengerRepository.existsById(id)){

                throw new ResourceNotFoundException(
                        "Passenger",
                        id
                );
            }

            passengerRepository.deleteById(id);

        } catch (ResourceNotFoundException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );

        } catch (DataIntegrityViolationException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Passenger cannot be deleted because reservations exist"
            );
        }

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

    private void addBookingData(Model model) {

        model.addAttribute(
                "trains",
                trainRepository.findAll()
        );

        model.addAttribute(
                "passengers",
                passengerRepository.findAll()
        );
    }

    private void validateTrain(Train train) {

        if (isBlank(train.getTrainNumber())) {
            throw new InvalidInputException(
                    "Train number is required");
        }

        if (isBlank(train.getTrainName())) {
            throw new InvalidInputException(
                    "Train name is required");
        }

        if (isBlank(train.getSource())
                || isBlank(train.getDestination())) {
            throw new InvalidInputException(
                    "Source and destination are required");
        }

        if (train.getDistanceKm() <= 0) {
            throw new InvalidInputException(
                    "Distance must be greater than zero");
        }

        if (train.getTotalSeats() <= 0) {
            throw new InvalidInputException(
                    "Total seats must be greater than zero");
        }

        if (train.getAvailableSeats() < 0
                || train.getAvailableSeats() > train.getTotalSeats()) {
            throw new InvalidInputException(
                    "Available seats must be between zero and total seats");
        }
    }

    private void validatePassenger(Passenger passenger) {

        if (isBlank(passenger.getName())) {
            throw new InvalidInputException(
                    "Passenger name is required");
        }

        if (passenger.getAge() <= 0) {
            throw new InvalidInputException(
                    "Passenger age must be greater than zero");
        }

        if (isBlank(passenger.getGender())) {
            throw new InvalidInputException(
                    "Gender is required");
        }

        if (isBlank(passenger.getMobile())) {
            throw new InvalidInputException(
                    "Mobile number is required");
        }
    }

    private boolean isBlank(String value) {

        return value == null || value.trim().isEmpty();
    }

}
