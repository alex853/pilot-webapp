package net.simforge.pilot.webapp;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.util.StringUtils;
import net.simforge.pilot.webapp.dynamodb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;
import java.util.UUID;

@org.springframework.stereotype.Controller
public class Controller {

    private static final String _my_user_id = "afe78778-39d3-494d-b366-e696e75b96a4";

    private DynamoDBMapper dynamoDBMapper;

    @Autowired
    private AmazonDynamoDB amazonDynamoDB;

    @Autowired
    private FSLogUserRepository fsLogUserRepository;

    @Autowired
    private FSLogPilotAppFlightRepository fsLogPilotAppFlightRepository;

    @GetMapping("/")
    public String root(Model model) {
        FSLogUser user = fsLogUserRepository.findByUserID(_my_user_id);
        FSLogPilotAppFlight flight = !StringUtils.isNullOrEmpty(user.getCurrentFlightID())
                ? fsLogPilotAppFlightRepository.findByFlightID(user.getCurrentFlightID())
                : null;

        if (flight == null) {
            return "no-flight";
        }

        model.addAttribute("flight", flight);
        switch (flight.getStatus()) {
            case Preflight:
                return "preflight";
            case TaxiOut:
                return "taxi-out";
            case Flying:
                return "flying";
            case TaxiIn:
                return "taxi-in";
            case Arrived:
                return "arrived";
            default:
                throw new IllegalStateException();
        }
    }

    @PostMapping("/start-flight")
    public RedirectView startFlight(Model model) {
        // todo check flight existence & its status

        FSLogUser user = fsLogUserRepository.findByUserID(_my_user_id);
        FSLogPilotAppFlight flight = new FSLogPilotAppFlight();

        flight.setFlightID(UUID.randomUUID().toString());
        flight.setStatus(FlightStatus.Preflight);

        fsLogPilotAppFlightRepository.save(flight);

        user.setCurrentFlightID(flight.getFlightID());
        fsLogUserRepository.save(user);

        return new RedirectView("/");
    }

    @PostMapping("/blocks-off")
    public RedirectView blocksOff(@RequestParam(name="departedFrom", required=true) String departedFrom, Model model) {
        // todo check flight existence & its status

        FSLogUser user = fsLogUserRepository.findByUserID(_my_user_id);
        FSLogPilotAppFlight flight = fsLogPilotAppFlightRepository.findByFlightID(user.getCurrentFlightID());

        flight.setStatus(FlightStatus.TaxiOut);
        flight.setDepartedFrom(departedFrom);
        flight.setTimeOut(LocalDateTime.now());

        fsLogPilotAppFlightRepository.save(flight);

        return new RedirectView("/");
    }

    @PostMapping("/takeoff")
    public RedirectView takeoff(Model model) {
        // todo check flight existence & its status

        FSLogUser user = fsLogUserRepository.findByUserID(_my_user_id);
        FSLogPilotAppFlight flight = fsLogPilotAppFlightRepository.findByFlightID(user.getCurrentFlightID());

        flight.setStatus(FlightStatus.Flying);
        flight.setTimeOff(LocalDateTime.now());

        fsLogPilotAppFlightRepository.save(flight);

        return new RedirectView("/");
    }

    @PostMapping("/landing")
    public RedirectView landing(@RequestParam(name="landedAt", required=true) String landedAt, Model model) {
        // todo check flight existence & its status

        FSLogUser user = fsLogUserRepository.findByUserID(_my_user_id);
        FSLogPilotAppFlight flight = fsLogPilotAppFlightRepository.findByFlightID(user.getCurrentFlightID());

        flight.setStatus(FlightStatus.TaxiIn);
        flight.setLandedAt(landedAt);
        flight.setTimeOn(LocalDateTime.now());

        fsLogPilotAppFlightRepository.save(flight);

        return new RedirectView("/");
    }

    @PostMapping("/blocks-on")
    public RedirectView blocksOn(Model model) {
        // todo check flight existence & its status

        FSLogUser user = fsLogUserRepository.findByUserID(_my_user_id);
        FSLogPilotAppFlight flight = fsLogPilotAppFlightRepository.findByFlightID(user.getCurrentFlightID());

        flight.setStatus(FlightStatus.Arrived);
        flight.setTimeIn(LocalDateTime.now());

        fsLogPilotAppFlightRepository.save(flight);

        return new RedirectView("/");
    }

    @PostMapping("/file-logbook")
    public String finish(Model model) {
        // todo check flight existence & its status

        FSLogUser user = fsLogUserRepository.findByUserID(_my_user_id);
        FSLogPilotAppFlight flight = fsLogPilotAppFlightRepository.findByFlightID(user.getCurrentFlightID());

        // todo save flight record to logbook

        user.setCurrentFlightID(null);

        fsLogUserRepository.save(user);

        return "filed";
    }

    @GetMapping("/greeting")
    public String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
        model.addAttribute("name", name);
        return "preflight";
    }

}
