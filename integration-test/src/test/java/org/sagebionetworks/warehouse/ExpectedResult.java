package org.sagebionetworks.warehouse;

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class ExpectedResult {
	
	private final long timestamp;
	private final String expectedQuery;
	
	public ExpectedResult(long timestamp, String expectedQuery) {
		super();
		this.timestamp = timestamp;
		this.expectedQuery = expectedQuery;
		Period.between(null, null);
		
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(expectedQuery, timestamp);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExpectedResult other = (ExpectedResult) obj;
		return Objects.equals(expectedQuery, other.expectedQuery) && timestamp == other.timestamp;
	}

	@Override
	public String toString() {
		return "ExpectedResult [timestamp=" + timestamp + ", expectedQuery=" + expectedQuery + "]";
	}

}
