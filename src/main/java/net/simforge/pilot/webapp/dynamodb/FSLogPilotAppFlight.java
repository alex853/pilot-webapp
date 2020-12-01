package net.simforge.pilot.webapp.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@DynamoDBTable(tableName = "fslog-pilotapp-flight")
public class FSLogPilotAppFlight {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private String flightID;
    private String awsStatus;
    private String awsTimeOut;
    private String awsTimeOff;
    private String awsTimeOn;
    private String awsTimeIn;
    private String departedFrom;
    private String landedAt;

    @DynamoDBHashKey(attributeName = "FlightID")
    public String getFlightID() {
        return flightID;
    }

    public void setFlightID(String flightID) {
        this.flightID = flightID;
    }

    @DynamoDBAttribute(attributeName = "Status")
    public String getAwsStatus() {
        return awsStatus;
    }

    public void setAwsStatus(String awsStatus) {
        this.awsStatus = awsStatus;
    }

    @DynamoDBIgnore
    public FlightStatus getStatus() {
        return awsStatus != null ? FlightStatus.valueOf(awsStatus) : null;
    }

    public void setStatus(FlightStatus status) {
        this.awsStatus = status != null ? status.name() : null;
    }

    @DynamoDBAttribute(attributeName = "TimeOut")
    public String getAwsTimeOut() {
        return awsTimeOut;
    }

    public void setAwsTimeOut(String awsTimeOut) {
        this.awsTimeOut = awsTimeOut;
    }

    @DynamoDBIgnore
    public LocalDateTime getTimeOut() {
        return awsTimeOut != null ? LocalDateTime.parse(awsTimeOut, dateTimeFormatter) : null;
    }

    public void setTimeOut(LocalDateTime timeOut) {
        this.awsTimeOut = timeOut != null ? timeOut.format(dateTimeFormatter) : null;
    }

    @DynamoDBAttribute(attributeName = "TimeOff")
    public String getAwsTimeOff() {
        return awsTimeOff;
    }

    public void setAwsTimeOff(String awsTimeOff) {
        this.awsTimeOff = awsTimeOff;
    }

    @DynamoDBIgnore
    public LocalDateTime getTimeOff() {
        return awsTimeOff != null ? LocalDateTime.parse(awsTimeOff, dateTimeFormatter) : null;
    }

    public void setTimeOff(LocalDateTime timeOff) {
        this.awsTimeOff = timeOff != null ? timeOff.format(dateTimeFormatter) : null;
    }

    @DynamoDBAttribute(attributeName = "TimeOn")
    public String getAwsTimeOn() {
        return awsTimeOn;
    }

    public void setAwsTimeOn(String awsTimeOn) {
        this.awsTimeOn = awsTimeOn;
    }

    @DynamoDBIgnore
    public LocalDateTime getTimeOn() {
        return awsTimeOn != null ? LocalDateTime.parse(awsTimeOn, dateTimeFormatter) : null;
    }

    public void setTimeOn(LocalDateTime timeOn) {
        this.awsTimeOn = timeOn != null ? timeOn.format(dateTimeFormatter) : null;
    }

    @DynamoDBAttribute(attributeName = "TimeIn")
    public String getAwsTimeIn() {
        return awsTimeIn;
    }

    public void setAwsTimeIn(String awsTimeIn) {
        this.awsTimeIn = awsTimeIn;
    }

    @DynamoDBIgnore
    public LocalDateTime getTimeIn() {
        return awsTimeIn != null ? LocalDateTime.parse(awsTimeIn, dateTimeFormatter) : null;
    }

    public void setTimeIn(LocalDateTime timeIn) {
        this.awsTimeIn = timeIn != null ? timeIn.format(dateTimeFormatter) : null;
    }

    @DynamoDBAttribute(attributeName = "DepartedFrom")
    public String getDepartedFrom() {
        return departedFrom;
    }

    public void setDepartedFrom(String departedFrom) {
        this.departedFrom = departedFrom;
    }

    @DynamoDBAttribute(attributeName = "LandedAt")
    public String getLandedAt() {
        return landedAt;
    }

    public void setLandedAt(String landedAt) {
        this.landedAt = landedAt;
    }
}
