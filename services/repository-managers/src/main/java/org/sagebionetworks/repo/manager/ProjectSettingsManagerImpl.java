package org.sagebionetworks.repo.manager;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sagebionetworks.repo.manager.storagelocation.StorageLocationProcessor;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectCertificationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StsStorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableMap;

public class ProjectSettingsManagerImpl implements ProjectSettingsManager {

	private static final String EXTERNAL_STORAGE_HELP = "http://docs.synapse.org/articles/custom_storage_location.html for more information on how to create a new external upload destination.";

	public static final int MAX_LOCATIONS_PER_PROJECT = 10;

	@Autowired
	private ProjectSettingsDAO projectSettingsDao;

	@Autowired
	private StorageLocationDAO storageLocationDAO;

	@Autowired
	private PrincipalAliasDAO principalAliasDAO;

	@Autowired
	private AuthorizationManager authorizationManager;

	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private TrashManager trashManager;
	
	private static final Map<Class<? extends ProjectSetting>, ProjectSettingsType> TYPE_MAP = ImmutableMap.of(
		UploadDestinationListSetting.class, ProjectSettingsType.upload,
		ProjectCertificationSetting.class, ProjectSettingsType.certification
	);

	private List<StorageLocationProcessor<? extends StorageLocationSetting>> storageLocationProcessors;
	
	@Autowired
	public void setStorageLocationProcessors(List<StorageLocationProcessor<? extends StorageLocationSetting>> storageLocationProcessors) {
		this.storageLocationProcessors = storageLocationProcessors;
	}

	@Override
	public ProjectSetting getProjectSetting(UserInfo userInfo, String id) throws DatastoreException, NotFoundException {
		ProjectSetting projectSetting = projectSettingsDao.get(id);
		if (!authorizationManager.canAccess(userInfo, projectSetting.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized()) {
			throw new UnauthorizedException("The current user does not have READ access on the project this setting applies to.");
		}
		return projectSetting;
	}

	@Override
	public Optional<ProjectSetting> getProjectSettingByProjectAndType(UserInfo userInfo, String projectId, ProjectSettingsType type)
			throws DatastoreException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized()) {
			throw new UnauthorizedException("The current user does not have READ access on the project " + projectId + ".");
		}
		return projectSettingsDao.get(projectId, type);
	}

	@Override
	public <T extends ProjectSetting> Optional<T> getProjectSettingForNode(UserInfo userInfo, String nodeId, ProjectSettingsType type,
			Class<T> expectedType) throws DatastoreException, UnauthorizedException, NotFoundException {
		
		ProjectSetting projectSetting = null;
		
		// The certification setting can be applied only at the project level, no need to walk up the hierarchy of settings
		if (ProjectSettingsType.certification == type) {
			String projectId = nodeDao.getProjectId(nodeId);
			projectSetting = projectSettingsDao.get(projectId, ProjectSettingsType.certification).orElse(null);
		} else {
			String projectSettingId = projectSettingsDao.getInheritedProjectSetting(nodeId, type);
			
			if (projectSettingId != null) {
				// Note that get throws NotFoundException if the project setting somehow doesn't exist.
				projectSetting = projectSettingsDao.get(projectSettingId);
			}	
		}
		
		if (projectSetting == null) {
			// Not having a setting is normal.
			return Optional.empty();
		}
		
		if (!expectedType.isInstance(projectSetting)) {
			throw new IllegalArgumentException("Settings type for '" + type + "' is not of type " + expectedType.getName());
		}
		
		return Optional.of(expectedType.cast(projectSetting));
	}

	@Override
	public List<UploadDestinationLocation> getUploadDestinationLocations(UserInfo userInfo, List<Long> storageLocationIds)
			throws DatastoreException, NotFoundException {
		return storageLocationDAO.getUploadDestinationLocations(storageLocationIds);
	}

	@Override
	@WriteTransaction
	public ProjectSetting createProjectSetting(UserInfo userInfo, ProjectSetting projectSetting)
			throws DatastoreException, NotFoundException {
		String parentId = projectSetting.getProjectId();

		// make sure the project id is a project
		EntityType nodeType = nodeManager.getNodeType(userInfo, parentId);
		Class<? extends Entity> nodeClass = EntityTypeUtils.getClassForType(nodeType);
		
		// A project certification setting can only be applied to a project and by an ACT member
		if (projectSetting instanceof ProjectCertificationSetting) {
			if (nodeClass != Project.class) {
				throw new IllegalArgumentException("The certification setting can be applied only to projects");
			}
			validateACTAccessForCertificationSetting(userInfo);
		}
		
		if (nodeClass != Project.class && nodeClass != Folder.class) {
			throw new IllegalArgumentException("The id is not the id of a project or folder entity");
		}
		
		if (!authorizationManager.canAccess(userInfo, parentId, ObjectType.ENTITY, ACCESS_TYPE.CREATE).isAuthorized()) {
			throw new UnauthorizedException("Cannot create settings for this project");
		}
	
		
		// Can't create project settings if a parent has an StsStorageLocation.
		Optional<ProjectSetting> parentSetting = getProjectSettingForNode(userInfo, parentId, ProjectSettingsType.upload,
				ProjectSetting.class);
		if (parentSetting.isPresent() && isStsStorageLocationSetting(parentSetting.get())) {
			throw new IllegalArgumentException("Can't override project settings in an STS-enabled folder path");
		}
		
		// Auto-fill the setting type to avoid inconsistencies in the database
		projectSetting.setSettingsType(TYPE_MAP.get(projectSetting.getClass()));

		validateProjectSetting(projectSetting, userInfo);

		// Can't add an StsStorageLocation to a non-empty entity.
		if (!isEntityEmptyWithTrash(parentId) && isStsStorageLocationSetting(projectSetting)) {
			throw new IllegalArgumentException("Can't enable STS in a non-empty folder");
		}

		String id = projectSettingsDao.create(projectSetting);
		return projectSettingsDao.get(id);
	}

	@Override
	@WriteTransaction
	public void updateProjectSetting(UserInfo userInfo, ProjectSetting projectSetting) throws DatastoreException, NotFoundException {
		ValidateArgument.required(projectSetting.getId(), "The id");
		ValidateArgument.required(projectSetting.getProjectId(), "The project id");
		
		// A project certification setting can only be applied to by an ACT member
		if (projectSetting instanceof ProjectCertificationSetting) {
			validateACTAccessForCertificationSetting(userInfo);
		}
	
		if (!authorizationManager.canAccess(userInfo, projectSetting.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE).isAuthorized()) {
			throw new UnauthorizedException("Cannot update settings on this project");
		}
		
		// Auto-fill the setting type to avoid inconsistencies in the database
		projectSetting.setSettingsType(TYPE_MAP.get(projectSetting.getClass()));
		
		validateProjectSetting(projectSetting, userInfo);

		// Can't add or modify an StsStorageLocation on a non-empty entity.
		if (!isEntityEmptyWithTrash(projectSetting.getProjectId())) {
			if (isStsStorageLocationSetting(projectSetting)) {
				throw new IllegalArgumentException("Can't enable STS in a non-empty folder");
			}

			ProjectSetting oldSetting = projectSettingsDao.get(projectSetting.getId());
			if (isStsStorageLocationSetting(oldSetting)) {
				throw new IllegalArgumentException("Can't disable STS in a non-empty folder");
			}
		}

		projectSettingsDao.update(projectSetting);
	}

	@Override
	@WriteTransaction
	public void deleteProjectSetting(UserInfo userInfo, String id) throws DatastoreException, NotFoundException {
		// Note: projectSettingsDao.get() ensures that projectSetting is not null, or throws a NotFoundException.
		ProjectSetting projectSetting = projectSettingsDao.get(id);
		
		// A project certification setting can only be applied to by an ACT member
		if (projectSetting instanceof ProjectCertificationSetting) {
			validateACTAccessForCertificationSetting(userInfo);
		}
	
		if (!authorizationManager.canAccess(userInfo, projectSetting.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.DELETE)
				.isAuthorized()) {
			throw new UnauthorizedException("Cannot delete settings from this project");
		}
		
		// Can't delete an StsStorageLocation on a non-empty entity.
		if (!isEntityEmptyWithTrash(projectSetting.getProjectId()) &&
				isStsStorageLocationSetting(projectSetting)) {
			throw new IllegalArgumentException("Can't disable STS in a non-empty folder");
		}

		projectSettingsDao.delete(id);
	}
	
	private void validateACTAccessForCertificationSetting(UserInfo userInfo) {
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("The user must be an ACT member in order to customize the certification requirement");
		}
	}

	// Helper method to check that the given entity has no children (either in the node hierarchy or in the trash can).
	boolean isEntityEmptyWithTrash(String entityId) {
		return !nodeManager.doesNodeHaveChildren(entityId) && !trashManager.doesEntityHaveTrashedChildren(entityId);
	}

	@Override
	public <T extends StorageLocationSetting> T createStorageLocationSetting(UserInfo userInfo, T storageLocationSetting)
			throws DatastoreException, NotFoundException {
		ValidateArgument.required(userInfo, "The user");
		ValidateArgument.required(storageLocationSetting, "The storage location");

		this.processStorageLocation(userInfo, storageLocationSetting);
		
		// Default UploadType to null.
		if (storageLocationSetting.getUploadType() == null) {
			storageLocationSetting.setUploadType(UploadType.NONE);
		}

		storageLocationSetting.setCreatedBy(userInfo.getId());
		storageLocationSetting.setCreatedOn(new Date());

		Long storageLocationId = storageLocationDAO.create(storageLocationSetting);

		return (T) storageLocationDAO.get(storageLocationId);
	}

	@Override
	public List<StorageLocationSetting> getMyStorageLocationSettings(UserInfo userInfo) throws DatastoreException, NotFoundException {
		return storageLocationDAO.getByOwner(userInfo.getId());
	}

	@Override
	public StorageLocationSetting getMyStorageLocationSetting(UserInfo userInfo, Long storageLocationId)
			throws DatastoreException, NotFoundException {
		ValidateArgument.required(storageLocationId, "storageLocationId");
		StorageLocationSetting setting = storageLocationDAO.get(storageLocationId);
		if (!userInfo.getId().equals(setting.getCreatedBy())) {
			throw new UnauthorizedException("Only the creator can access storage location settings");
		}
		return setting;
	}

	@Override
	public StorageLocationSetting getStorageLocationSetting(Long storageLocationId) throws DatastoreException, NotFoundException {
		if (storageLocationId == null) {
			return null;
		}
		return storageLocationDAO.get(storageLocationId);
	}

	// package private for testing only
	void validateProjectSetting(ProjectSetting setting, UserInfo currentUser) {
		ValidateArgument.required(setting.getProjectId(), "projectId");
		ValidateArgument.required(setting.getSettingsType(), "settingsType");
		if (setting instanceof UploadDestinationListSetting) {
			validateUploadDestinationListSetting((UploadDestinationListSetting) setting, currentUser);
		} else if (setting instanceof ProjectCertificationSetting) {
			ValidateArgument.required(((ProjectCertificationSetting) setting).getCertificationRequired(), "certificationRequired");
		} else {
			ValidateArgument.failRequirement("Cannot handle project setting of type " + setting.getClass().getName());
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void processStorageLocation(UserInfo userInfo, StorageLocationSetting storageLocation) {
		for (StorageLocationProcessor processor : storageLocationProcessors) {
			Class<? extends StorageLocationSetting> clazz = storageLocation.getClass();
			if (processor.supports(clazz)) {
				processor.beforeCreate(userInfo, storageLocation);
			}
		}
	}

	private void validateUploadDestinationListSetting(UploadDestinationListSetting setting, UserInfo currentUser) {
		ValidateArgument.required(setting.getLocations(), "settings.locations");
		ValidateArgument.requirement(setting.getLocations().size() >= 1, "settings.locations must at least have one entry");
		ValidateArgument.requirement(setting.getLocations().size() <= MAX_LOCATIONS_PER_PROJECT,
				"The maximum number of settings.locations is limited to " + MAX_LOCATIONS_PER_PROJECT);

		for (Long uploadId : setting.getLocations()) {
			try {
				StorageLocationSetting storageLocationSetting = storageLocationDAO.get(uploadId);
				if (storageLocationSetting instanceof ExternalS3StorageLocationSetting) {
					// only the owner or an admin can add this setting to a project
					if (!currentUser.isAdmin() && !currentUser.getId().equals(storageLocationSetting.getCreatedBy())) {
						String ownerUsername = principalAliasDAO.getUserName(storageLocationSetting.getCreatedBy());
						throw new UnauthorizedException("Only the owner of the external S3 upload destination (user " + ownerUsername
								+ ") can add this upload destination to a project. Either ask that user to perform this operation or follow the steps to create a new external s3 upload destination (see "
								+ EXTERNAL_STORAGE_HELP);
					}
				}

				// STS storage locations have additional restrictions.
				if (storageLocationSetting instanceof StsStorageLocationSetting
						&& Boolean.TRUE.equals(((StsStorageLocationSetting) storageLocationSetting).getStsEnabled())) {
					// Can only be applied to folders.
					EntityType nodeType = nodeManager.getNodeType(currentUser, setting.getProjectId());
					if (EntityType.folder != nodeType) {
						throw new IllegalArgumentException("Can only enable STS on a folder");
					}

					// Cannot be applied with other storage locations.
					if (setting.getLocations().size() != 1) {
						throw new IllegalArgumentException("An STS-enabled folder cannot add other upload " + "destinations");
					}
				}
			} catch (NotFoundException e) {
				ValidateArgument.failRequirement("uploadId " + uploadId + " is not a valid upload destination location");
			}
		}
	}

	@Override
	public boolean isStsStorageLocationSetting(StorageLocationSetting storageLocationSetting) {
		return storageLocationSetting instanceof StsStorageLocationSetting &&
				Boolean.TRUE.equals(((StsStorageLocationSetting) storageLocationSetting).getStsEnabled());
	}

	@Override
	public boolean isStsStorageLocationSetting(ProjectSetting projectSetting) {
		if (!(projectSetting instanceof UploadDestinationListSetting)) {
			// Impossible code path, but add this check here to future-proof this against ClassCastExceptions.
			return false;
		}

		// Short-cut: Only check the first Storage Location ID. Entities with an StsStorageLocation can't have other
		// storage locations.
		List<Long> storageLocationIdList = ((UploadDestinationListSetting) projectSetting).getLocations();
		long storageLocationId = storageLocationIdList.get(0);
		try {
			StorageLocationSetting storageLocationSetting = storageLocationDAO.get(storageLocationId);
			return isStsStorageLocationSetting(storageLocationSetting);
		} catch (NotFoundException e) {
			// If the storage location somehow doesn't exist, then it's not an StsStorageLocation.
			return false;
		}
	}

}
