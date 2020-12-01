package net.simforge.pilot.webapp.dynamodb;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

@EnableScan
public interface FSLogUserRepository extends CrudRepository<FSLogUser, String> {
    FSLogUser findByUserID(String userID);
}
