package net.simforge.pilot.webapp;

import java.time.LocalDateTime;

public class Flight {
    private Status status;
    private LocalDateTime timeOut;
    private LocalDateTime timeOff;
    private LocalDateTime timeOn;
    private LocalDateTime timeIn;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(LocalDateTime timeOut) {
        this.timeOut = timeOut;
    }

    public LocalDateTime getTimeOff() {
        return timeOff;
    }

    public void setTimeOff(LocalDateTime timeOff) {
        this.timeOff = timeOff;
    }

    public LocalDateTime getTimeOn() {
        return timeOn;
    }

    public void setTimeOn(LocalDateTime timeOn) {
        this.timeOn = timeOn;
    }

    public LocalDateTime getTimeIn() {
        return timeIn;
    }

    public void setTimeIn(LocalDateTime timeIn) {
        this.timeIn = timeIn;
    }

    enum Status {
        Preflight, TaxiOut, Flying, TaxiIn, Arrived
    }
}
