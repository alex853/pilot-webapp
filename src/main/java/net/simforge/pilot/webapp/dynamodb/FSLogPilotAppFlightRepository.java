package net.simforge.pilot.webapp.dynamodb;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

@EnableScan
public interface FSLogPilotAppFlightRepository extends CrudRepository<FSLogPilotAppFlight, String> {
    FSLogPilotAppFlight findByFlightID(String flightID);
}
