package org.sagebionetworks.repo.model.dbo.auth;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface UserStatusDao {

	void setLastSeenOn(List<Long> principalIds, Date lastSeenOn);
	
	Optional<Date> getLastSeenOn(long principalId);
	
	void setDisabled(long principalId, boolean disabled);
	
	boolean isDisabled(long principalId);
	
	List<Long> getInactiveUsersBatch(Date lastSeenOnThreshold, int batchSize);
}

