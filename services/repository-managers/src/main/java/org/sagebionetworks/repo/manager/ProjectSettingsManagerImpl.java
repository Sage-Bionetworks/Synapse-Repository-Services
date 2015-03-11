package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UploadDestinationLocationDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.project.ExternalS3UploadDestinationLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.s3.AmazonS3Client;

public class ProjectSettingsManagerImpl implements ProjectSettingsManager {

	static private Logger log = LogManager.getLogger(ProjectSettingsManagerImpl.class);

	@Autowired
	private ProjectSettingsDAO projectSettingsDao;

	@Autowired
	private UploadDestinationLocationDAO uploadDestinationLocationDAO;

	@Autowired
	private AuthorizationManager authorizationManager;

	@Autowired
	private NodeDAO nodeDAO;

	@Autowired
	private NodeManager nodeManager;

	@Autowired
	private AmazonS3Client s3client;

	@Autowired
	private UserProfileManager userProfileManager;

	private EntityType PROJECT_ENTITY_TYPE;

	@PostConstruct
	private void getProjectEntityType() {
		PROJECT_ENTITY_TYPE = EntityType.getNodeTypeForClass(Project.class);
	}

	@Override
	public ProjectSetting getProjectSetting(UserInfo userInfo, String id) throws DatastoreException, NotFoundException {
		ProjectSetting projectSetting = projectSettingsDao.get(id);
		if (projectSetting != null
				&& !authorizationManager.canAccess(userInfo, projectSetting.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ).getAuthorized()) {
			throw new UnauthorizedException("Cannot read information from this project");
		}
		return projectSetting;
	}

	@Override
	public ProjectSetting getProjectSettingByProjectAndType(UserInfo userInfo, String projectId, ProjectSettingsType type)
			throws DatastoreException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ).getAuthorized()) {
			throw new UnauthorizedException("Cannot read information from this project");
		}
		ProjectSetting projectSetting = projectSettingsDao.get(projectId, type);
		return projectSetting;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ProjectSetting> T getProjectSettingForParent(UserInfo userInfo, String parentId, ProjectSettingsType type,
			Class<T> expectedType) throws DatastoreException, UnauthorizedException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, parentId, ObjectType.ENTITY, ACCESS_TYPE.READ).getAuthorized()) {
			throw new UnauthorizedException("Cannot read information for this parent entity");
		}
		List<EntityHeader> nodePath = nodeManager.getNodePath(userInfo, parentId);
		// the root of the node path should be the project
		if (nodePath.isEmpty()) {
			throw new DatastoreException("No path for this parentId could be found");
		}
		// walk the path from the top (the top is probably root and the next one should be project)
		String projectId = null;
		for (EntityHeader node : nodePath) {
			if (node.getType().equals(PROJECT_ENTITY_TYPE.getEntityType())) {
				projectId = node.getId();
				break;
			}
		}
		if (projectId == null) {
			throw new IllegalArgumentException("This parentId is not contained in a project");
		}
		ProjectSetting projectSetting = projectSettingsDao.get(projectId, type);
		if (projectSetting != null && !expectedType.isInstance(projectSetting)) {
			throw new IllegalArgumentException("Settings type for '" + type + "' is not of type " + expectedType.getName());
		}
		return (T) projectSetting;
	}

	@Override
	public List<UploadDestinationLocation> getUploadDestinationLocations(UserInfo userInfo, List<Long> locations) {
		return uploadDestinationLocationDAO.getUploadDestinationLocations(locations);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public ProjectSetting createProjectSetting(UserInfo userInfo, ProjectSetting projectSetting) throws DatastoreException, NotFoundException {
		// make sure the project id is a project
		EntityType nodeType = nodeManager.getNodeType(userInfo, projectSetting.getProjectId());
		if (nodeType.getClassForType() != Project.class) {
			throw new IllegalArgumentException("The id is not the id of a project entity");
		}
		if (!authorizationManager.canAccess(userInfo, projectSetting.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.CREATE).getAuthorized()) {
			throw new UnauthorizedException("Cannot create settings for this project");
		}
		ProjectSettingsUtil.validateProjectSetting(projectSetting, userInfo, userProfileManager, uploadDestinationLocationDAO);
		String id = projectSettingsDao.create(projectSetting);
		return projectSettingsDao.get(id);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateProjectSetting(UserInfo userInfo, ProjectSetting projectSetting) throws DatastoreException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, projectSetting.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE).getAuthorized()) {
			throw new UnauthorizedException("Cannot update settings on this project");
		}
		ProjectSettingsUtil.validateProjectSetting(projectSetting, userInfo, userProfileManager, uploadDestinationLocationDAO);
		projectSettingsDao.update(projectSetting);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteProjectSetting(UserInfo userInfo, String id) throws DatastoreException, NotFoundException {
		ProjectSetting projectSetting = projectSettingsDao.get(id);
		if (projectSetting != null
				&& !authorizationManager.canAccess(userInfo, projectSetting.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.DELETE).getAuthorized()) {
			throw new UnauthorizedException("Cannot delete settings from this project");
		}
		projectSettingsDao.delete(id);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends UploadDestinationLocationSetting> T createUploadDestinationLocationSetting(UserInfo userInfo,
			T uploadDestinationLocationSetting) throws DatastoreException, NotFoundException, IOException {
		if (uploadDestinationLocationSetting instanceof ExternalS3UploadDestinationLocationSetting) {
			UserProfile userProfile = userProfileManager.getUserProfile(userInfo, userInfo.getId().toString());
			ProjectSettingsUtil.validateOwnership((ExternalS3UploadDestinationLocationSetting) uploadDestinationLocationSetting, userProfile,
					s3client);
		}

		uploadDestinationLocationSetting.setCreatedBy(userInfo.getId());
		uploadDestinationLocationSetting.setCreatedOn(new Date());
		Long uploadId = uploadDestinationLocationDAO.create(uploadDestinationLocationSetting);
		return (T) uploadDestinationLocationDAO.get(uploadId);
	}

	@Override
	public <T extends UploadDestinationLocationSetting> T updateUploadDestinationLocationSetting(UserInfo userInfo,
			T uploadDestinationLocationSetting) throws DatastoreException, NotFoundException {
		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("Cannot update settings on this project");
		}
		return uploadDestinationLocationDAO.update(uploadDestinationLocationSetting);
	}
}
