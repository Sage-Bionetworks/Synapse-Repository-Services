package org.sagebionetworks.repo.model;

import java.util.List;

public interface ProjectStatsDAO {

	/**
	 * If there is no activity for the given user and project, a new ProjectStat
	 * will be created. If the given user already has activity for the given
	 * project, then the existing ProjectStat will be update if the given
	 * activity date occurs after the existing activity date.
	 * 
	 * @param projectStats
	 */
	public void updateProjectStat(ProjectStat...projectStats);
	

	public List<ProjectStat> getProjectStatsForUser(Long userId);

}
