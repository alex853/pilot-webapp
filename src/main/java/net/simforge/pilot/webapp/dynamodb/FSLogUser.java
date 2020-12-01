package net.simforge.pilot.webapp.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "fslog-user")
public class FSLogUser {
    private String userID;
    private String currentFlightID;

    @DynamoDBHashKey(attributeName = "UserID")
    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    @DynamoDBAttribute(attributeName = "CurrentFlightID")
    public String getCurrentFlightID() {
        return currentFlightID;
    }

    public void setCurrentFlightID(String currentFlightID) {
        this.currentFlightID = currentFlightID;
    }
}
