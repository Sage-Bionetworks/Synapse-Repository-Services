package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ProjectSettingsDAO {

	String create(ProjectSetting settings) throws DatastoreException, InvalidModelException;

	ProjectSetting get(String id) throws DatastoreException, NotFoundException;

	Optional<ProjectSetting> get(String projectId, ProjectSettingsType projectSettingsType) throws DatastoreException;

	List<ProjectSetting> getAllForProject(String projectId) throws DatastoreException, NotFoundException;

	/**
	 * Walks up the entity hierarchy and returns the ID of the first ProjectSetting of the given type, or null if no ProjectSettings are
	 * defined in the entity hierarchy for the given type.
	 */
	String getInheritedProjectSetting(String entityId, ProjectSettingsType settingType);

	ProjectSetting update(ProjectSetting settings) throws DatastoreException, InvalidModelException, NotFoundException,
			ConflictingUpdateException;

	void delete(String id) throws DatastoreException, NotFoundException;
}
