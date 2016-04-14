package org.sagebionetworks.repo.model;

import java.util.List;

public interface ProjectStatsDAO {

	/**
	 * If there is no activity for the given user and project, a new ProjectStat
	 * will be created. If the given user already has activity for the given
	 * project, then the existing ProjectStat will be update if the given
	 * activity data occurs after the existing activity date.
	 * 
	 * @param projectStat
	 */
	public void update(ProjectStat projectStat);

	public List<ProjectStat> getProjectStatsForUser(Long userId);

}
