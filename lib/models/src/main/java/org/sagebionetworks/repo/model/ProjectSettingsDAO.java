package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ProjectSettingsDAO {

	public String create(ProjectSetting settings) throws DatastoreException, InvalidModelException;

	public ProjectSetting get(String id) throws DatastoreException, NotFoundException;

	public Optional<ProjectSetting> get(String projectId, ProjectSettingsType projectSettingsType) throws DatastoreException;

	public List<ProjectSetting> getAllForProject(String projectId) throws DatastoreException, NotFoundException;

	/**
	 * Walks up the entity hierarchy and returns the first ProjectSetting of the specified type for this entity, or
	 * null if no ProjectSettings of the specified type are defined in the entity hierarchy.
	 */
	ProjectSetting getInheritedForEntity(String entityId, ProjectSettingsType type);

	public ProjectSetting update(ProjectSetting settings) throws DatastoreException, InvalidModelException, NotFoundException,
			ConflictingUpdateException;

	public void delete(String id) throws DatastoreException, NotFoundException;
}
