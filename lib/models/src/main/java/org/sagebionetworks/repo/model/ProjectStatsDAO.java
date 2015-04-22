package org.sagebionetworks.repo.model;

import java.util.List;

public interface ProjectStatsDAO {

	public void update(ProjectStat projectStat);

	public List<ProjectStat> getProjectStatsForUser(Long userId);
}
