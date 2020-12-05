package net.simforge.pilot.webapp;

import com.amazonaws.util.StringUtils;
import net.simforge.pilot.webapp.dynamodb.*;
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

    private final FSLogUserRepository fsLogUserRepository;
    private final FSLogPilotAppFlightRepository fsLogPilotAppFlightRepository;
    private final FSLogRecordRepository fsLogRecordRepository;

    @Value("${my.user.id}")
    private String _my_user_id; // todo read it from session

    public Controller(FSLogUserRepository fsLogUserRepository,
                      FSLogPilotAppFlightRepository fsLogPilotAppFlightRepository,
                      FSLogRecordRepository fsLogRecordRepository) {
        this.fsLogUserRepository = fsLogUserRepository;
        this.fsLogPilotAppFlightRepository = fsLogPilotAppFlightRepository;
        this.fsLogRecordRepository = fsLogRecordRepository;
    }

    @GetMapping("/")
    public String root(Model model) {
        FSLogUser user = loadUser();
        FSLogPilotAppFlight flight = loadCurrentFlight(user);

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
    public RedirectView startFlight() {
        FSLogUser user = loadUser();
        FSLogPilotAppFlight flight = loadCurrentFlight(user);
        if (flight != null) {
            throw new IllegalStateException("Could not start new flight - there is another flight");
        }
        flight = new FSLogPilotAppFlight();

        flight.setFlightID(UUID.randomUUID().toString());
        flight.setStatus(FlightStatus.Preflight);

        fsLogPilotAppFlightRepository.save(flight);

        user.setCurrentFlightID(flight.getFlightID());
        fsLogUserRepository.save(user);

        return new RedirectView("/");
    }

    @PostMapping("/terminate-flight")
    public RedirectView terminateFlight() {
        FSLogUser user = loadUser();
        FSLogPilotAppFlight flight = loadCurrentFlight(user);
        if (flight == null) {
            throw new IllegalStateException("Could not terminate flight - there is no flight to terminate");
        }

        user.setCurrentFlightID(null);
        fsLogUserRepository.save(user);

        return new RedirectView("/");
    }

    @PostMapping("/blocks-off")
    public RedirectView blocksOff(@RequestParam(name="departedFrom") String departedFrom) {
        FSLogUser user = loadUser();
        FSLogPilotAppFlight flight = loadCurrentFlight(user);
        if (flight == null) {
            throw new IllegalStateException("Could not find current flight, action cancelled");
        }
        if (flight.getStatus() != FlightStatus.Preflight) {
            throw new IllegalStateException("Preflight status is expected for Blocks Off action, actual status is " + flight.getStatus());
        }

        flight.setStatus(FlightStatus.TaxiOut);
        flight.setDepartedFrom(departedFrom);
        flight.setTimeOut(LocalDateTime.now(ZoneOffset.UTC));

        fsLogPilotAppFlightRepository.save(flight);

        return new RedirectView("/");
    }

    @PostMapping("/takeoff")
    public RedirectView takeoff() {
        FSLogUser user = loadUser();
        FSLogPilotAppFlight flight = loadCurrentFlight(user);
        if (flight == null) {
            throw new IllegalStateException("Could not find current flight, action cancelled");
        }
        if (flight.getStatus() != FlightStatus.TaxiOut) {
            throw new IllegalStateException("TaxiOut status is expected for Takeoff action, actual status is " + flight.getStatus());
        }

        flight.setStatus(FlightStatus.Flying);
        flight.setTimeOff(LocalDateTime.now(ZoneOffset.UTC));

        fsLogPilotAppFlightRepository.save(flight);

        return new RedirectView("/");
    }

    @PostMapping("/landing")
    public RedirectView landing(@RequestParam(name="landedAt") String landedAt) {
        FSLogUser user = loadUser();
        FSLogPilotAppFlight flight = loadCurrentFlight(user);
        if (flight == null) {
            throw new IllegalStateException("Could not find current flight, action cancelled");
        }
        if (flight.getStatus() != FlightStatus.Flying) {
            throw new IllegalStateException("Flying status is expected for Landing action, actual status is " + flight.getStatus());
        }

        flight.setStatus(FlightStatus.TaxiIn);
        flight.setLandedAt(landedAt);
        flight.setTimeOn(LocalDateTime.now(ZoneOffset.UTC));

        fsLogPilotAppFlightRepository.save(flight);

        return new RedirectView("/");
    }

    @PostMapping("/blocks-on")
    public RedirectView blocksOn() {
        FSLogUser user = loadUser();
        FSLogPilotAppFlight flight = loadCurrentFlight(user);
        if (flight == null) {
            throw new IllegalStateException("Could not find current flight, action cancelled");
        }
        if (flight.getStatus() != FlightStatus.TaxiIn) {
            throw new IllegalStateException("TaxiIn status is expected for BlockOn action, actual status is " + flight.getStatus());
        }

        flight.setStatus(FlightStatus.Arrived);
        flight.setTimeIn(LocalDateTime.now(ZoneOffset.UTC));

        fsLogPilotAppFlightRepository.save(flight);

        return new RedirectView("/");
    }

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter hhmmFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @PostMapping("/file-logbook")
    public String finish() {
        FSLogUser user = loadUser();
        FSLogPilotAppFlight flight = loadCurrentFlight(user);
        if (flight == null) {
            throw new IllegalStateException("Could not find current flight, action cancelled");
        }
        if (flight.getStatus() != FlightStatus.Arrived) {
            throw new IllegalStateException("Arrived status is expected for Finish action, actual status is " + flight.getStatus());
        }

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

    private FSLogUser loadUser() {
        return fsLogUserRepository.findByUserID(_my_user_id);
    }

    private FSLogPilotAppFlight loadCurrentFlight(FSLogUser user) {
        return !StringUtils.isNullOrEmpty(user.getCurrentFlightID())
                ? fsLogPilotAppFlightRepository.findByFlightID(user.getCurrentFlightID())
                : null;
    }

}
