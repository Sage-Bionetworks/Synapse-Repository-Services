package org.sagebionetworks.repo.manager.statistics;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;

/**
 * Common interface for {@link AwsKinesisLogRecord} that are related to statistics
 * 
 * @author Marco
 *
 */
public interface StatisticsEventLogRecord extends AwsKinesisLogRecord {

	/**
	 * @return The timestamp of the event for this record
	 */
	Long getTimestamp();

	/**
	 * @param timestamp Sets the timestamp for the record
	 * @return
	 */
	StatisticsEventLogRecord withTimestamp(Long timestamp);

	/**
	 * @return The id of the user that triggered the event
	 */
	Long getUserId();

	/**
	 * @param userId Sets the id of the user for the record
	 * @return
	 */
	StatisticsEventLogRecord withUserId(Long userId);

}
