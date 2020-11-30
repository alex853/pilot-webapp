package net.simforge.pilot.webapp;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;

@org.springframework.stereotype.Controller
public class Controller {

    private Flight flight = null;

    @GetMapping("/")
    public String root(Model model) {
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
        flight = new Flight();
        flight.setStatus(Flight.Status.Preflight);
        return new RedirectView("/");
    }

    @PostMapping("/blocks-off")
    public RedirectView blocksOff(@RequestParam(name="departedFrom", required=true) String departedFrom, Model model) {
        // todo check flight existence & its status
        flight.setStatus(Flight.Status.TaxiOut);
        flight.setDepartedFrom(departedFrom);
        flight.setTimeOut(LocalDateTime.now());
        return new RedirectView("/");
    }

    @PostMapping("/takeoff")
    public RedirectView takeoff(Model model) {
        // todo check flight existence & its status
        flight.setStatus(Flight.Status.Flying);
        flight.setTimeOff(LocalDateTime.now());
        return new RedirectView("/");
    }

    @PostMapping("/landing")
    public RedirectView landing(@RequestParam(name="landedAt", required=true) String landedAt, Model model) {
        // todo check flight existence & its status
        flight.setStatus(Flight.Status.TaxiIn);
        flight.setLandedAt(landedAt);
        flight.setTimeOn(LocalDateTime.now());
        return new RedirectView("/");
    }

    @PostMapping("/blocks-on")
    public RedirectView blocksOn(Model model) {
        // todo check flight existence & its status
        flight.setStatus(Flight.Status.Arrived);
        flight.setTimeIn(LocalDateTime.now());
        return new RedirectView("/");
    }

    @PostMapping("/file-logbook")
    public String finish(Model model) {
        // todo check flight existence & its status
        // todo save flight record to logbook
        flight = null;
        return "filed";
    }

    @GetMapping("/greeting")
    public String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
        model.addAttribute("name", name);
        return "preflight";
    }

}
