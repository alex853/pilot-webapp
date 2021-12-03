package net.simforge.pilot.webapp;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.util.StringUtils;
import net.simforge.pilot.webapp.dynamodb.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@org.springframework.stereotype.Controller
public class Controller {

    private final FSLogUserRepository fsLogUserRepository;
    private final FSLogPilotAppFlightRepository fsLogPilotAppFlightRepository;
    private final FSLogRecordRepository fsLogRecordRepository;
    private final AmazonDynamoDB amazonDynamoDB;

    @Value("${my.user.id}")
    private String _my_user_id; // todo read it from session

    public Controller(FSLogUserRepository fsLogUserRepository,
                      FSLogPilotAppFlightRepository fsLogPilotAppFlightRepository,
                      FSLogRecordRepository fsLogRecordRepository, AmazonDynamoDB amazonDynamoDB) {
        this.fsLogUserRepository = fsLogUserRepository;
        this.fsLogPilotAppFlightRepository = fsLogPilotAppFlightRepository;
        this.fsLogRecordRepository = fsLogRecordRepository;
        this.amazonDynamoDB = amazonDynamoDB;
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
    public RedirectView blocksOff() {
        FSLogUser user = loadUser();
        FSLogPilotAppFlight flight = loadCurrentFlight(user);
        if (flight == null) {
            throw new IllegalStateException("Could not find current flight, action cancelled");
        }
        if (flight.getStatus() != FlightStatus.Preflight) {
            throw new IllegalStateException("Preflight status is expected for Blocks Off action, actual status is " + flight.getStatus());
        }

        flight.setStatus(FlightStatus.TaxiOut);
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
    public RedirectView landing() {
        FSLogUser user = loadUser();
        FSLogPilotAppFlight flight = loadCurrentFlight(user);
        if (flight == null) {
            throw new IllegalStateException("Could not find current flight, action cancelled");
        }
        if (flight.getStatus() != FlightStatus.Flying) {
            throw new IllegalStateException("Flying status is expected for Landing action, actual status is " + flight.getStatus());
        }

        flight.setStatus(FlightStatus.TaxiIn);
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

    @PostMapping("/set")
    @ResponseBody
    public ResponseEntity<Void> set(@RequestParam(name="property") String property, @RequestParam(name="value") String value) {
        FSLogUser user = loadUser();
        FSLogPilotAppFlight flight = loadCurrentFlight(user);
        if (flight == null) {
            throw new IllegalStateException("Could not find current flight, action cancelled");
        }

        switch (property) {
            case "departed-from":
                flight.setDepartedFrom(value);
                break;
            case "landed-at":
                flight.setLandedAt(value);
                break;
            case "time-out":
                flight.setTimeOut(updateTime(flight.getTimeOut(), value));
                break;
            case "time-off":
                flight.setTimeOff(updateTime(flight.getTimeOff(), value));
                break;
            case "time-on":
                flight.setTimeOn(updateTime(flight.getTimeOn(), value));
                break;
            case "time-in":
                flight.setTimeIn(updateTime(flight.getTimeIn(), value));
                break;
            default:
                throw new IllegalArgumentException("Unknown property '" + property + "'");
        }

        fsLogPilotAppFlightRepository.save(flight);

        return ResponseEntity.ok().build();
    }

    private LocalDateTime updateTime(LocalDateTime originalDateTime, String value) {
        if (originalDateTime == null) {
            throw new IllegalArgumentException("Original date/time is not set");
        }

        final String[] strs = value.split(":");
        return originalDateTime.withHour(Integer.parseInt(strs[0])).withMinute(Integer.parseInt(strs[1]));
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
        record.getFlight().setTotalTime(formatDuration(Duration.between(flight.getTimeOut(), flight.getTimeIn())));
        record.getFlight().setAirTime(formatDuration(Duration.between(flight.getTimeOff(), flight.getTimeOn())));

        record.getFlight().setCallsign(null);
        record.getFlight().setFlightNumber(null);
        record.getFlight().setAircraftType(null);
        record.getFlight().setAircraftRegistration(null);

        fsLogRecordRepository.save(record);

        user.setCurrentFlightID(null);

        fsLogUserRepository.save(user);

        return "filed";
    }

    private FSLogUser loadUser() {
        return fsLogUserRepository.findByUserID(_my_user_id);
    }

    private FSLogPilotAppFlight loadCurrentFlight(FSLogUser user) {
        return !StringUtils.isNullOrEmpty(user.getCurrentFlightID())
                ? fsLogPilotAppFlightRepository.findByFlightID(user.getCurrentFlightID())
                : null;
    }

/*    private void importData() {
        DateTimeFormatter fullDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        Table src = dynamoDB.getTable("fslog-poc-flight-report");
        ItemCollection<ScanOutcome> collection = src.scan();
        for (Item item : collection) {
            String type = item.getString("Type");
            if ("flight".equals(type)) {
                LocalDateTime beginningDt = LocalDateTime.parse(item.getString("BeginningDT"), fullDateTimeFormatter);

                FSLogRecord record = new FSLogRecord();
                record.setUserID(_my_user_id);
                record.setBeginningDT(dateTimeFormatter.format(beginningDt));
                record.setRecordID(UUID.randomUUID().toString());
                record.setDate(dateFormatter.format(beginningDt));
                record.setType("flight");
                record.setComment(item.getString("Comment"));
                record.setRemarks(item.getString("Remarks"));

                record.setFlight(new FSLogRecord.Flight());
                record.getFlight().setDeparture(item.getString("Departure"));
                record.getFlight().setDestination(item.getString("Destination"));
                record.getFlight().setTimeOut(item.getString("TimeOut"));
                record.getFlight().setTimeOff(item.getString("TimeOff"));
                record.getFlight().setTimeOn(item.getString("TimeOn"));
                record.getFlight().setTimeIn(item.getString("TimeIn"));
                record.getFlight().setDistance(!item.isNull("Distance") ? item.getInt("Distance") : null);
                record.getFlight().setTotalTime(calcDuration(record.getFlight().getTimeOut(), record.getFlight().getTimeIn()));
                record.getFlight().setAirTime(calcDuration(record.getFlight().getTimeOff(), record.getFlight().getTimeOn()));
                record.getFlight().setCallsign(item.getString("Callsign"));
                record.getFlight().setFlightNumber(item.getString("FlightNumber"));
                record.getFlight().setAircraftType(item.getString("AircraftType"));
                record.getFlight().setAircraftRegistration(item.getString("AircraftRegistration"));

                fsLogRecordRepository.save(record);
            } else if ("transfer".equals(type)) {
                LocalDateTime beginningDt = LocalDateTime.parse(item.getString("BeginningDT"), fullDateTimeFormatter);

                FSLogRecord record = new FSLogRecord();
                record.setUserID(_my_user_id);
                record.setBeginningDT(dateTimeFormatter.format(beginningDt));
                record.setRecordID(UUID.randomUUID().toString());
                record.setDate(dateFormatter.format(beginningDt));
                record.setType("transfer");
                record.setComment(item.getString("Comment"));
                record.setRemarks(item.getString("Remarks"));

                record.setTransfer(new FSLogRecord.Transfer());
                record.getTransfer().setDeparture(item.getString("Departure"));
                record.getTransfer().setDestination(item.getString("Destination"));
                record.getTransfer().setTimeOut(item.getString("TimeOut"));
                record.getTransfer().setTimeIn(item.getString("TimeIn"));
                record.getTransfer().setMethod(item.getString("Method"));

                fsLogRecordRepository.save(record);
            } else if ("discontinuity".equals(type)) {
                LocalDateTime beginningDt = LocalDateTime.parse(item.getString("BeginningDT"), fullDateTimeFormatter);

                FSLogRecord record = new FSLogRecord();
                record.setUserID(_my_user_id);
                record.setBeginningDT(dateTimeFormatter.format(beginningDt));
                record.setRecordID(UUID.randomUUID().toString());
                record.setDate(dateFormatter.format(beginningDt));
                record.setType("discontinuity");
                record.setComment(item.getString("Comment"));
                record.setRemarks(item.getString("Remarks"));

                record.setDiscontinuity(new FSLogRecord.Discontinuity());
                record.getDiscontinuity().setTime(hhmmFormatter.format(beginningDt));

                fsLogRecordRepository.save(record);
            } else {
                throw new IllegalArgumentException();
            }
        }
        System.out.println();
    }*/

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        String positive = String.format(
                "%d:%02d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60);
        return seconds < 0 ? "-" + positive : positive;
    }

    private String calcDuration(String time1, String time2) {
        if (time1 == null || time2 == null) {
            return null;
        }
        LocalTime localTime1 = LocalTime.parse(time1, hhmmFormatter);
        LocalTime localTime2 = LocalTime.parse(time2, hhmmFormatter);
        Duration duration = Duration.between(localTime1, localTime2);
        return formatDuration(duration);
    }

}
