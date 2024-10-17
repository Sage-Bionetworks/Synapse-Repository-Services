package org.sagebionetworks.repo.model.dbo.statistics;

import java.time.Instant;
import java.util.Map;

public interface ProjectStorageUsageCacheDao {
	
	boolean isUpdatedOnAfter(long projectId, Instant instant);
	
	void setStorageUsageMap(long projectId, Map<String, Long> storageLocationSize);
	
	Map<String, Long> getStorageUsageMap(long projectId);

}
