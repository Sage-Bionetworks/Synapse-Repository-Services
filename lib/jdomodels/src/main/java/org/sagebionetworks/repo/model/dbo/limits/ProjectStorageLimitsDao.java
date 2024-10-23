package org.sagebionetworks.repo.model.dbo.limits;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.limits.ProjectStorageData;
import org.sagebionetworks.repo.model.limits.ProjectStorageLocationLimit;

public interface ProjectStorageLimitsDao {

	/**
	 * 
	 * @param projectId
	 * @param instant
	 * @return True if the project storage usage data was updated after the given instant. Returns false
	 *         if no data for the given project exists.
	 */
	boolean isStorageDataModifiedOnAfter(Long projectId, Instant instant);

	/**
	 * Sets the storage usage data for the projects in the given batch, the locations in each project can be null in which case an empty list is stored
	 * 
	 * @param projectStorageData
	 */
	void setStorageData(List<ProjectStorageData> projectStorageData);

	/**
	 * 
	 * @param projectId
	 * @return The storage usage data for the project with the given id
	 */
	Optional<ProjectStorageData> getStorageData(Long projectId);
	
	/**
	 * Sets the limit for a single storage location
	 * 
	 * @param limit
	 * @return
	 */
	ProjectStorageLocationLimit setStorageLocationLimit(long userId, ProjectStorageLocationLimit limit);
	
	/**
	 * 
	 * @param projectId
	 * @param storageLocationId
	 * @return The limit for the given project and storage location ids pair
	 */
	Optional<ProjectStorageLocationLimit> getStorageLocationLimit(Long projectId, Long storageLocationId);
	
	/**
	 * 
	 * @param projectId
	 * @return All the storage location limits for the given project
	 */
	List<ProjectStorageLocationLimit> getStorageLocationLimits(Long projectId);

	void truncateAll();
}
