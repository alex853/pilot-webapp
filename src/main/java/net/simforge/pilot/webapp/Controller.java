package net.simforge.pilot.webapp;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.util.StringUtils;
import net.simforge.pilot.webapp.dynamodb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@org.springframework.stereotype.Controller
public class Controller {

    private DynamoDBMapper dynamoDBMapper;

    @Autowired
    private AmazonDynamoDB amazonDynamoDB;

    @Autowired
    private FSLogUserRepository fsLogUserRepository;

    @Autowired
    private FSLogPilotAppFlightRepository fsLogPilotAppFlightRepository;

    @Autowired
    private FSLogRecordRepository fsLogRecordRepository;

    @Value("${my.user.id}")
    private String _my_user_id; // todo read it from session

    @GetMapping("/")
    public String root(Model model) {
        FSLogUser user = fsLogUserRepository.findByUserID(_my_user_id);
        FSLogPilotAppFlight flight = !StringUtils.isNullOrEmpty(user.getCurrentFlightID())
                ? fsLogPilotAppFlightRepository.findByFlightID(user.getCurrentFlightID())
                : null;

        if (flight == null) {
            return "resting";
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
        flight.setTimeOut(LocalDateTime.now(ZoneOffset.UTC));

        fsLogPilotAppFlightRepository.save(flight);

        return new RedirectView("/");
    }

    @PostMapping("/takeoff")
    public RedirectView takeoff(Model model) {
        // todo check flight existence & its status

        FSLogUser user = fsLogUserRepository.findByUserID(_my_user_id);
        FSLogPilotAppFlight flight = fsLogPilotAppFlightRepository.findByFlightID(user.getCurrentFlightID());

        flight.setStatus(FlightStatus.Flying);
        flight.setTimeOff(LocalDateTime.now(ZoneOffset.UTC));

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
        flight.setTimeOn(LocalDateTime.now(ZoneOffset.UTC));

        fsLogPilotAppFlightRepository.save(flight);

        return new RedirectView("/");
    }

    @PostMapping("/blocks-on")
    public RedirectView blocksOn(Model model) {
        // todo check flight existence & its status

        FSLogUser user = fsLogUserRepository.findByUserID(_my_user_id);
        FSLogPilotAppFlight flight = fsLogPilotAppFlightRepository.findByFlightID(user.getCurrentFlightID());

        flight.setStatus(FlightStatus.Arrived);
        flight.setTimeIn(LocalDateTime.now(ZoneOffset.UTC));

        fsLogPilotAppFlightRepository.save(flight);

        return new RedirectView("/");
    }

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter hhmmFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @PostMapping("/file-logbook")
    public String finish(Model model) {
        // todo check flight existence & its status

        FSLogUser user = fsLogUserRepository.findByUserID(_my_user_id);
        FSLogPilotAppFlight flight = fsLogPilotAppFlightRepository.findByFlightID(user.getCurrentFlightID());

        FSLogRecord record = new FSLogRecord();
        record.setUserID(user.getUserID());
        record.setBeginningDT(flight.getTimeOut().format(dateTimeFormatter));
        record.setRecordID(UUID.randomUUID().toString());
        record.setDate(flight.getTimeOut().format(dateFormatter));
        record.setType("flight");
        record.setComment("TODO - comment"); // todo
        record.setRemarks("TODO - remarks, tracked by pilotapp-webapp"); // todo

        record.setFlight(new FSLogRecord.Flight());
        record.getFlight().setDeparture(flight.getDepartedFrom());
        record.getFlight().setDestination(flight.getLandedAt());

        record.getFlight().setTimeOut(flight.getTimeOut().format(hhmmFormatter));
        record.getFlight().setTimeOff(flight.getTimeOff().format(hhmmFormatter));
        record.getFlight().setTimeOn(flight.getTimeOn().format(hhmmFormatter));
        record.getFlight().setTimeIn(flight.getTimeIn().format(hhmmFormatter));

        record.getFlight().setDistance(null);
        record.getFlight().setTotalTime(format(Duration.between(flight.getTimeOut(), flight.getTimeIn())));
        record.getFlight().setAirTime(format(Duration.between(flight.getTimeOff(), flight.getTimeOn())));

        record.getFlight().setCallsign(null);
        record.getFlight().setFlightNumber(null);
        record.getFlight().setAircraftType(null);
        record.getFlight().setAircraftRegistration(null);

        fsLogRecordRepository.save(record);

        user.setCurrentFlightID(null);

        fsLogUserRepository.save(user);

        return "filed";
    }

    private String format(Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        String positive = String.format(
                "%d:%02d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60);
        return seconds < 0 ? "-" + positive : positive;
    }

    @GetMapping("/greeting")
    public String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
        model.addAttribute("name", name);
        return "preflight";
    }

}
