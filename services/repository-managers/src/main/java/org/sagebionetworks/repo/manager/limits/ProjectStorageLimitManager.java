package org.sagebionetworks.repo.manager.limits;

import java.time.Duration;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.limits.ProjectStorageLimitsDao;
import org.sagebionetworks.repo.model.limits.ProjectStorageData;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.Clock;
import org.springframework.stereotype.Service;

@Service
public class ProjectStorageLimitManager {
	
	private static final Duration CACHE_UPDATE_FREQUENCY = Duration.ofMinutes(2);
		
	private ProjectStorageLimitsDao storageUsageDao;
	
	private TableIndexDAO replicationDao;
	
	private Clock clock;
	
	public ProjectStorageLimitManager(ProjectStorageLimitsDao storageUsageDao, TableIndexDAO replicationDao, Clock clock) {
		this.storageUsageDao = storageUsageDao;
		this.replicationDao = replicationDao;
		this.clock = clock;
	}
	
	@WriteTransaction
	public void refreshProjectStorageData(Long projectId) {
		if (storageUsageDao.isStorageDataModifiedOnAfter(projectId, clock.now().toInstant().minus(CACHE_UPDATE_FREQUENCY))) {
			return;
		}
		
		ProjectStorageData data = replicationDao.computeProjectStorageData(projectId);
	
		storageUsageDao.setStorageData(List.of(data));
	}

}
