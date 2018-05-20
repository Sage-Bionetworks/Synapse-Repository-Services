package org.sagebionetworks.repo.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
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
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalSyncSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.RequesterPaysSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.AmazonErrorCodes;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.internal.Constants;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class ProjectSettingsManagerImpl implements ProjectSettingsManager {

	public static final int MIN_SECRET_KEY_CHARS = 36;

	private static final String EXTERNAL_S3_HELP = "http://docs.synapse.org/articles/custom_storage_location.html for more information on how to create a new external s3 upload destination";

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
	private AmazonS3 s3client;

	@Autowired
	private UserProfileManager userProfileManager;

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
	public <T extends ProjectSetting> T getProjectSettingForNode(UserInfo userInfo, String nodeId, ProjectSettingsType type,
			Class<T> expectedType) throws DatastoreException, UnauthorizedException, NotFoundException {
		List<EntityHeader> nodePath = nodeManager.getNodePathAsAdmin(nodeId);
		// the root of the node path should be the project
		if (nodePath.isEmpty()) {
			throw new DatastoreException("No path for this parentId could be found");
		}
		List<Long> nodePathIds = Lists.transform(nodePath, new Function<EntityHeader, Long>() {
			@Override
			public Long apply(EntityHeader input) {
				return KeyFactory.stringToKey(input.getId());
			}
		});

		// get the first available project setting of the correct type
		ProjectSetting projectSetting = projectSettingsDao.get(nodePathIds, type);
		if (projectSetting != null && !expectedType.isInstance(projectSetting)) {
			throw new IllegalArgumentException("Settings type for '" + type + "' is not of type " + expectedType.getName());
		}
		return (T) projectSetting;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ProjectSetting> List<T> getNodeSettingsByType(ProjectSettingsType projectSettingsType, Class<T> expectedType) {
		List<ProjectSetting> projectSettings = projectSettingsDao.getByType(projectSettingsType);
		Iterator<ProjectSetting> iter = projectSettings.iterator();
		while (iter.hasNext()) {
			ProjectSetting projectSetting = iter.next();
			if (!expectedType.isInstance(projectSetting)) {
				// report error if wrong type, but don't throw error. This is called by worker who just wants to handle
				// all correct cases
				log.error("The project setting for type " + projectSettingsType + " and node " + projectSetting.getProjectId()
						+ " is not of the expected type " + expectedType.getName() + " but instead is " + projectSetting.getClass().getName());
				iter.remove();
			}
		}
		return (List<T>) projectSettings;
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
		if (EntityTypeUtils.getClassForType(nodeType) != Project.class && EntityTypeUtils.getClassForType(nodeType) != Folder.class) {
			throw new IllegalArgumentException("The id is not the id of a project or folder entity");
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
			UserProfile userProfile = userProfileManager.getUserProfile(userInfo.getId().toString());
			ExternalS3StorageLocationSetting externalS3StorageLocationSetting = (ExternalS3StorageLocationSetting) storageLocationSetting;
			if (!StringUtils.isEmpty(externalS3StorageLocationSetting.getEndpointUrl())
					&& !externalS3StorageLocationSetting.getEndpointUrl().equals("https://" + Constants.S3_HOSTNAME)) {
				throw new NotImplementedException("Synapse does not yet support external S3 buckets in non-us-east-1 locations");
			}
			validateOwnership(externalS3StorageLocationSetting, userProfile);
		} else if (storageLocationSetting instanceof ExternalStorageLocationSetting) {
			ExternalStorageLocationSetting externalStorageLocationSetting = (ExternalStorageLocationSetting) storageLocationSetting;
			ValidateArgument.required(externalStorageLocationSetting.getUrl(), "url");
			ValidateArgument.validUrl(externalStorageLocationSetting.getUrl());
		}else if (storageLocationSetting instanceof ExternalObjectStorageLocationSetting){
			ExternalObjectStorageLocationSetting externalObjectS3StorageLocationSetting = (ExternalObjectStorageLocationSetting) storageLocationSetting;
			//strip leading and trailing slashes and whitespace from the endpointUrl and bucket
			String strippedEndpoint = StringUtils.strip(externalObjectS3StorageLocationSetting.getEndpointUrl(), "/ \t");
			String bucket = externalObjectS3StorageLocationSetting.getBucket();

			//validate
			ValidateArgument.validUrl(strippedEndpoint);
			ValidateArgument.requirement(StringUtils.isNotBlank(bucket) && !bucket.contains("/"), "bucket can not contain slashes('/') or be blank");

			//passed validation, set endpoint as the stripped version
			externalObjectS3StorageLocationSetting.setEndpointUrl(strippedEndpoint);
		}else if (storageLocationSetting instanceof ProxyStorageLocationSettings){
			ProxyStorageLocationSettings proxySettings = (ProxyStorageLocationSettings)storageLocationSetting;
			ValidateArgument.required(proxySettings.getProxyUrl(), "proxyUrl");
			ValidateArgument.required(proxySettings.getSecretKey(), "secretKey");
			ValidateArgument.required(proxySettings.getUploadType(), "uploadType");
			if(proxySettings.getSecretKey().length() < MIN_SECRET_KEY_CHARS){
				throw new IllegalArgumentException("SecretKey must be at least: "+MIN_SECRET_KEY_CHARS+" characters but was: "+proxySettings.getSecretKey().length());
			}
			try {
				URL proxyUrl = new URL(proxySettings.getProxyUrl());
				if(!"https".equals(proxyUrl.getProtocol())){
					throw new IllegalArgumentException("proxyUrl protocol must be be HTTPS");
				}
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException("porxyUrl is malformed: "+e.getMessage());
			}
		}

		storageLocationSetting.setCreatedBy(userInfo.getId());
		storageLocationSetting.setCreatedOn(new Date());
		Long uploadId = storageLocationDAO.create(storageLocationSetting);
		return (T) storageLocationDAO.get(uploadId);
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
		} else if (setting instanceof ExternalSyncSetting) {
			validateExternalSyncSetting((ExternalSyncSetting) setting);
		} else if (setting instanceof RequesterPaysSetting) {
			ValidateArgument.required(((RequesterPaysSetting) setting).getRequesterPays(), "RequesterPaysSetting.requesterPays");
		} else {
			ValidateArgument.failRequirement("Cannot handle project setting of type " + setting.getClass().getName());
		}
	}

	private void validateExternalSyncSetting(ExternalSyncSetting setting) {
		ValidateArgument.required(setting.getAutoSync(), "ExternalSyncSetting.autoSync");
		ValidateArgument.required(setting.getLocationId(), "ExternalSyncSetting.locationId");
		// check for empty node
		if (!nodeDAO.getChildrenIds(setting.getProjectId()).isEmpty()) {
			throw new IllegalArgumentException("You cannot apply autosync to a folder or project that has any children in it");
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
						UserProfile owner = userProfileManager.getUserProfile(storageLocationSetting.getCreatedBy().toString());
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

		if (!checkForCorrectName(userProfile, userName)) {
			throw new IllegalArgumentException("The username " + userName + " found under key " + key + " from bucket " + bucket
					+ " is not what was expected. " + getExplanation(userProfile, bucket, key));
		}
	}

	private boolean checkForCorrectName(UserProfile userProfile, String userName) {
		if (userName.equals(userProfile.getUserName())) {
			return true;
		}
		if (userName.equalsIgnoreCase(userProfile.getEmail())) {
			return true;
		}
		if (userProfile.getEmails() != null) {
			for (String email : userProfile.getEmails()) {
				if (userName.equalsIgnoreCase(email)) {
					return true;
				}
			}
		}
		return false;
	}

	private static final String SECURITY_EXPLANATION = "For security purposes, Synapse needs to establish that %s has permission to write to the bucket. Please create an S3 object in bucket '%s' with key '%s' that contains the user name '%s'. Also see "
			+ EXTERNAL_S3_HELP;

	private static String getExplanation(UserProfile userProfile, String bucket, String key) {
		return String.format(SECURITY_EXPLANATION, userProfile.getUserName(), bucket, key, userProfile.getUserName());
	}
}
