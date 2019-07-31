package org.sagebionetworks.repo.model;

import java.util.Iterator;
import java.util.List;

import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.TemporaryCode;

public interface ProjectSettingsDAO {

	public String create(ProjectSetting settings) throws DatastoreException, InvalidModelException;

	public ProjectSetting get(String id) throws DatastoreException, NotFoundException;

	public ProjectSetting get(String projectId, ProjectSettingsType projectSettingsType) throws DatastoreException;

	public ProjectSetting get(List<Long> parentIds, ProjectSettingsType projectSettingsType) throws DatastoreException;

	public List<ProjectSetting> getAllForProject(String projectId) throws DatastoreException, NotFoundException;

	@TemporaryCode(author="marco.marasca@sagebase.org")
	public Iterator<ProjectSetting> getByType(ProjectSettingsType projectSettingsType) throws DatastoreException, NotFoundException;

	public ProjectSetting update(ProjectSetting settings) throws DatastoreException, InvalidModelException, NotFoundException,
			ConflictingUpdateException;

	public void delete(String id) throws DatastoreException, NotFoundException;
}
