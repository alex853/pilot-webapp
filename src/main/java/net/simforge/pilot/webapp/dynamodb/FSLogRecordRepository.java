package net.simforge.pilot.webapp.dynamodb;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

@EnableScan
public interface FSLogRecordRepository extends CrudRepository<FSLogRecord, FSLogRecord.RecordKey> {
}
