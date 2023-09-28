package org.sagebionetworks.warehouse;

import java.time.Instant;

public interface WarehouseTestHelper {

	/**
	 * Assert that the given count query will return a value greater than one. The
	 * given query is not executed immediately. Instead, the query is saved to S3
	 * with a key that matches the caller's stack frame plus an expiration time with
	 * a value of now plus the provided maxNumberOfHours. Each time this method is
	 * called, it will attempt to find an execute all previously submitted queries
	 * that have expired that match this stack frame. After expired query results
	 * are tested, all expired queries are deleted from S3.
	 * 
	 * @param queryString
	 * @throws Exception
	 */
	void assertWarehouseQuery(String queryString, int maxNumberOfHours) throws Exception;

	/**
	 * Generate a between predicate for the given Instant. For example, given an
	 * input of '2022-12-31T23:59:59.605Z' the results will be: 'between
	 * date('2022-12-31') and date('2023-01-01')'
	 * 
	 * @param instant
	 * @return
	 */
	String toDateStringBetweenPlusAndMinusFiveSeconds(Instant instant);

	/**
	 * Generate a between predicate for the given Instant. For example, given an
	 * input of '2022-12-31T23:59:59.605Z' the results will be: 'between
	 * from_iso8601_timestamp('2022-12-31T23:59:54.605Z') and
	 * from_iso8601_timestamp('2023-01-01T00:00:04.605Z')'
	 * 
	 * @param instant
	 * @return
	 */
	String toIsoTimestampStringBetweenPlusAndMinusFiveSeconds(Instant instant);
}
