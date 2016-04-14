package org.sagebionetworks.repo.manager;

import java.util.Date;

import org.sagebionetworks.repo.model.ObjectType;

public interface ProjectStatsManager {

	/**
	 * Update the projects statistics for the given user and object.
	 * 
	 * This method will first lookup the projectID associated with the given
	 * objectId-type. If this is the first time this users has activity for this
	 * project a new ProjectStat will be added. If a ProjectStat already exists
	 * for this user and project then the ProjectStat will be updated if the
	 * given activity is newer than the existing activity.
	 * 
	 * @param userId
	 *            The user that performed the activity.
	 * @param objectId
	 *            The ID of the object that was was created or updated.
	 * @param objectType
	 *            The type of the object that was created or updated.
	 * @param activityDate
	 *            The date/time when the activity occurred.
	 */
	void updateProjectStats(Long userId, String objectId,
			ObjectType objectType, Date activityDate);

	/**
	 * Get the project that contains the given object.
	 * 
	 * @param objectId
	 * @param objectType
	 * @return The projectId that contains the given object. Null if the object
	 *         does not belong to a project.
	 */
	String getProjectForObject(String objectId, ObjectType objectType);

}
