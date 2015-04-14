package org.sagebionetworks.repo.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
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
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.AmazonErrorCodes;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.util.CollectionUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.Constants;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class ProjectSettingsManagerImpl implements ProjectSettingsManager {

	private static final String OWNER_MARKER = "owner.txt";
	private static final String EXTERNAL_S3_HELP = "www.synapse.org//#!HelpPages:ExternalS3Buckets for more information on how to create a new external s3 upload destination";

	static private Logger log = LogManager.getLogger(ProjectSettingsManagerImpl.class);

	@Autowired
	private ProjectSettingsDAO projectSettingsDao;

	@Autowired
	private StorageLocationDAO storageLocationDAO;

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
	public List<UploadDestinationLocation> getUploadDestinationLocations(UserInfo userInfo, List<Long> locations) throws DatastoreException,
			NotFoundException {
		return storageLocationDAO.getUploadDestinationLocations(locations);
	}

	@Override
	@WriteTransaction
	public ProjectSetting createProjectSetting(UserInfo userInfo, ProjectSetting projectSetting) throws DatastoreException, NotFoundException {
		// make sure the project id is a project
		EntityType nodeType = nodeManager.getNodeType(userInfo, projectSetting.getProjectId());
		if (nodeType.getClassForType() != Project.class) {
			throw new IllegalArgumentException("The id is not the id of a project entity");
		}
		if (!authorizationManager.canAccess(userInfo, projectSetting.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.CREATE).getAuthorized()) {
			throw new UnauthorizedException("Cannot create settings for this project");
		}
		validateProjectSetting(projectSetting, userInfo);
		String id = projectSettingsDao.create(projectSetting);
		return projectSettingsDao.get(id);
	}

	@Override
	@WriteTransaction
	public void updateProjectSetting(UserInfo userInfo, ProjectSetting projectSetting) throws DatastoreException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, projectSetting.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE).getAuthorized()) {
			throw new UnauthorizedException("Cannot update settings on this project");
		}
		validateProjectSetting(projectSetting, userInfo);
		projectSettingsDao.update(projectSetting);
	}

	@Override
	@WriteTransaction
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
	public <T extends StorageLocationSetting> T createStorageLocationSetting(UserInfo userInfo, T storageLocationSetting)
			throws DatastoreException, NotFoundException, IOException {
		if (storageLocationSetting instanceof ExternalS3StorageLocationSetting) {
			UserProfile userProfile = userProfileManager.getUserProfile(userInfo, userInfo.getId().toString());
			ExternalS3StorageLocationSetting externalS3StorageLocationSetting = (ExternalS3StorageLocationSetting) storageLocationSetting;
			if (!StringUtils.isEmpty(externalS3StorageLocationSetting.getEndpointUrl())
					&& !externalS3StorageLocationSetting.getEndpointUrl().equals("https://" + Constants.S3_HOSTNAME)) {
				throw new NotImplementedException("Synapse does not yet support external S3 buckets in non-us-east-1 locations");
			}
			validateOwnership(externalS3StorageLocationSetting, userProfile);
		}

		storageLocationSetting.setCreatedBy(userInfo.getId());
		storageLocationSetting.setCreatedOn(new Date());
		Long uploadId = storageLocationDAO.create(storageLocationSetting);
		return (T) storageLocationDAO.get(uploadId);
	}

	@Override
	public <T extends StorageLocationSetting> T updateStorageLocationSetting(UserInfo userInfo, T storageLocationSetting)
			throws DatastoreException, NotFoundException {
		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("Cannot update settings on this project");
		}
		return storageLocationDAO.update(storageLocationSetting);
	}

	@Override
	public List<StorageLocationSetting> getMyStorageLocationSettings(UserInfo userInfo) throws DatastoreException, NotFoundException {
		return storageLocationDAO.getByOwner(userInfo.getId());
	}

	@Override
	public StorageLocationSetting getMyStorageLocationSetting(UserInfo userInfo, Long storageLocationId) throws DatastoreException,
			NotFoundException {
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
		} else {
			ValidateArgument.failRequirement("Cannot handle project setting of type " + setting.getClass().getName());
		}
	}

	private void validateUploadDestinationListSetting(UploadDestinationListSetting setting, UserInfo currentUser) {
		ValidateArgument.requirement(CollectionUtils.isEmpty(setting.getDestinations()), "setting.getDestinations() cannot have a value.");
		ValidateArgument.required(setting.getLocations(), "settings.locations");
		ValidateArgument.requirement(setting.getLocations().size() >= 1, "settings.locations must at least have one entry");
		for (Long uploadId : setting.getLocations()) {
			try {
				StorageLocationSetting storageLocationSetting = storageLocationDAO.get(uploadId);
				if (storageLocationSetting instanceof ExternalS3StorageLocationSetting) {
					// only the owner or an admin can add this setting to a project
					if (!currentUser.isAdmin() && !currentUser.getId().equals(storageLocationSetting.getCreatedBy())) {
						UserProfile owner = userProfileManager.getUserProfile(currentUser, storageLocationSetting.getCreatedBy().toString());
						throw new UnauthorizedException(
								"Only the owner of the external s3 upload destination (user "
										+ owner.getUserName()
										+ ") can add this upload destination to a project. Either ask that user to perform this operation or follow the steps to create a new external s3 upload destination (see "
										+ EXTERNAL_S3_HELP);
					}
				}
			} catch (NotFoundException e) {
				ValidateArgument.failRequirement("uploadId " + uploadId + " is not a valid upload destination location");
			}
		}
	}

	private void validateOwnership(ExternalS3StorageLocationSetting externalS3StorageLocationSetting, UserProfile userProfile)
			throws IOException, NotFoundException {
		// check the ownership of the S3 bucket against the user
		String bucket = externalS3StorageLocationSetting.getBucket();
		String key = (externalS3StorageLocationSetting.getBaseKey() == null ? "" : externalS3StorageLocationSetting.getBaseKey())
				+ OWNER_MARKER;

		S3Object s3object;
		try {
			s3object = s3client.getObject(bucket, key);
		} catch (AmazonServiceException e) {
			if (AmazonErrorCodes.S3_BUCKET_NOT_FOUND.equals(e.getErrorCode())) {
				throw new IllegalArgumentException("Did not find S3 bucket " + bucket + ". " + getExplanation(userProfile, bucket, key));
			} else if (AmazonErrorCodes.S3_NOT_FOUND.equals(e.getErrorCode()) || AmazonErrorCodes.S3_KEY_NOT_FOUND.equals(e.getErrorCode())) {
				throw new IllegalArgumentException("Did not find S3 object at key " + key + " from bucket " + bucket + ". "
						+ getExplanation(userProfile, bucket, key));
			} else {
				throw new IllegalArgumentException("Could not read S3 object at key " + key + " from bucket " + bucket + ": "
						+ e.getMessage() + ". " + getExplanation(userProfile, bucket, key));
			}
		}

		String userName;
		BufferedReader reader = new BufferedReader(new InputStreamReader(s3object.getObjectContent()));
		try {
			userName = reader.readLine();
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not read username from key " + key + " from bucket " + bucket + ". "
					+ getExplanation(userProfile, bucket, key));
		} finally {
			reader.close();
		}

		if (StringUtils.isBlank(userName)) {
			throw new IllegalArgumentException("No username found under key " + key + " from bucket " + bucket + ". "
					+ getExplanation(userProfile, bucket, key));
		}

		if (!userName.equals(userProfile.getUserName())) {
			throw new IllegalArgumentException("The username " + userName + " found under key " + key + " from bucket " + bucket
					+ " is not what was expected. " + getExplanation(userProfile, bucket, key));
		}
	}

	private static final String SECURITY_EXPLANATION = "For security purposes, Synapse needs to establish that %s has persmission to write to the bucket. Please create an S3 object in bucket '%s' with key '%s' that contains the user name '%s'. Also see "
			+ EXTERNAL_S3_HELP;

	private static String getExplanation(UserProfile userProfile, String bucket, String key) {
		return String.format(SECURITY_EXPLANATION, userProfile.getUserName(), bucket, key, userProfile.getUserName());
	}
}
