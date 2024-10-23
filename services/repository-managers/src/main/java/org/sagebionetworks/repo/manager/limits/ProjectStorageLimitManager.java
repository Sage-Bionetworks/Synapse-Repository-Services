package org.sagebionetworks.repo.manager.limits;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.limits.ProjectStorageLimitsDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.limits.ProjectStorageData;
import org.sagebionetworks.repo.model.limits.ProjectStorageEvent;
import org.sagebionetworks.repo.model.limits.ProjectStorageLocationUsage;
import org.sagebionetworks.repo.model.limits.ProjectStorageUsage;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class ProjectStorageLimitManager {
	
	private static final Duration CACHE_UPDATE_FREQUENCY = Duration.ofMinutes(2);
		
	private static final Comparator<ProjectStorageLocationUsage> LOCATION_COMPARATOR = Comparator.comparing(ProjectStorageLocationUsage::getStorageLocationId);
	
	private TransactionalMessenger messenger;
	
	private ProjectStorageLimitsDao storageUsageDao;
	
	private TableIndexDAO replicationDao;
	
	private NodeDAO nodeDao;
	
	private Clock clock;
	
	private Set<Long> accessedProjects;
	
	public ProjectStorageLimitManager(TransactionalMessenger messenger, ProjectStorageLimitsDao storageUsageDao, TableIndexDAO replicationDao, NodeDAO nodeDao, Clock clock) {
		this.messenger = messenger;
		this.storageUsageDao = storageUsageDao;
		this.replicationDao = replicationDao;
		this.nodeDao = nodeDao;
		this.clock = clock;
		this.accessedProjects = ConcurrentHashMap.newKeySet();
	}
	
	public ProjectStorageUsage gerProjectStorageUsage(String projectId) {
		ValidateArgument.requiredNotBlank(projectId, "The projectId");
		ValidateArgument.requirement(EntityType.project.equals(nodeDao.getNodeTypeById(projectId)), "The entity with the given id is not a project.");
		
		Long projectIdLong = KeyFactory.stringToKey(projectId);
		
		Map<String, ProjectStorageLocationUsage> locations = new HashMap<>();
		
		// First sets the limits
		storageUsageDao.getStorageLocationLimits(projectIdLong).forEach(limit -> {
			locations.put(limit.getStorageLocationId(), new ProjectStorageLocationUsage()
				.setStorageLocationId(limit.getStorageLocationId())
				.setMaxAllowedFileBytes(limit.getMaxAllowedFileBytes())
				.setSumFileBytes(0L)
				.setIsOverLimit(false)
			);
		});
		
		// Now updates the usage
		storageUsageDao.getStorageData(projectIdLong)
			.map(ProjectStorageData::getStorageLocationData)
			.orElseGet(() -> Collections.emptyMap())
			.forEach((storageLocationId, currentUsage) -> {
				ProjectStorageLocationUsage usage = locations.get(storageLocationId);
				
				// No limit defined for this location, sets the usage
				if (usage == null) {
					locations.put(storageLocationId, new ProjectStorageLocationUsage()
						.setStorageLocationId(storageLocationId)
						.setSumFileBytes(currentUsage)
						.setIsOverLimit(false)
						.setMaxAllowedFileBytes(null)
					);
				// A limit is defined, sets the usage accordingly
				} else {
					usage.setSumFileBytes(currentUsage)
						.setIsOverLimit(currentUsage > usage.getMaxAllowedFileBytes());
				}
			});

		accessedProjects.add(projectIdLong);
						
		return new ProjectStorageUsage()
			.setProjectId(KeyFactory.keyToString(projectIdLong))
			.setLocations(locations.values().stream().sorted(LOCATION_COMPARATOR).collect(Collectors.toList()));
	}
	
	@WriteTransaction
	public void refreshProjectStorageData(Long projectId) {
		if (storageUsageDao.isStorageDataModifiedOnAfter(projectId, clock.now().toInstant().minus(CACHE_UPDATE_FREQUENCY))) {
			return;
		}
		
		ProjectStorageData data = replicationDao.computeProjectStorageData(projectId);
	
		storageUsageDao.setStorageData(List.of(data));
	}
	
	// On a timer this is invoked to send the notifications for each accessed project
	public void sendProjectStorageNotifications() {
		accessedProjects.forEach( projectId -> {
			messenger.publishMessageAfterCommit(new ProjectStorageEvent()
				.setObjectType(ObjectType.PROJECT_STORAGE_EVENT)
				.setObjectId(projectId.toString())
				.setProjectId(projectId)
			);
			accessedProjects.remove(projectId);
		});
	}

}
