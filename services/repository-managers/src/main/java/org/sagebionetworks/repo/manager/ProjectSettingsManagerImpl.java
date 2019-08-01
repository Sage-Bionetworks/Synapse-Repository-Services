package org.sagebionetworks.repo.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.migration.MergeStorageLocationsResponse;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.AmazonErrorCodes;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.S3Object;
import com.google.cloud.storage.StorageException;
import com.google.common.net.InternetDomainName;

public class ProjectSettingsManagerImpl implements ProjectSettingsManager {

	public static final int MIN_SECRET_KEY_CHARS = 36;
	
	public static final int MAX_LOCATIONS_PER_PROJECT = 10;

	private static final String EXTERNAL_STORAGE_HELP = "http://docs.synapse.org/articles/custom_storage_location.html for more information on how to create a new external upload destination.";

	@Autowired
	private ProjectSettingsDAO projectSettingsDao;

	@Autowired
	private StorageLocationDAO storageLocationDAO;

	@Autowired
	private AuthorizationManager authorizationManager;

	@Autowired
	private NodeManager nodeManager;

	@Autowired
	private SynapseS3Client s3client;

	@Autowired
	private SynapseGoogleCloudStorageClient googleCloudStorageClient;

	@Autowired
	private UserProfileManager userProfileManager;
	
	@Autowired
	private FileHandleDao fileHandleDao;

	@Override
	public ProjectSetting getProjectSetting(UserInfo userInfo, String id) throws DatastoreException, NotFoundException {
		ProjectSetting projectSetting = projectSettingsDao.get(id);
		if (projectSetting != null
				&& !authorizationManager.canAccess(userInfo, projectSetting.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized()) {
			throw new UnauthorizedException("Cannot read information from this project");
		}
		return projectSetting;
	}

	@Override
	public ProjectSetting getProjectSettingByProjectAndType(UserInfo userInfo, String projectId, ProjectSettingsType type)
			throws DatastoreException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized()) {
			throw new UnauthorizedException("Cannot read information from this project");
		}
		return projectSettingsDao.get(projectId, type);
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
		List<Long> nodePathIds = nodePath.stream().map(input -> KeyFactory.stringToKey(input.getId())).collect(Collectors.toList());

		// get the first available project setting of the correct type
		ProjectSetting projectSetting = projectSettingsDao.get(nodePathIds, type);
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
		if (EntityTypeUtils.getClassForType(nodeType) != Project.class && EntityTypeUtils.getClassForType(nodeType) != Folder.class) {
			throw new IllegalArgumentException("The id is not the id of a project or folder entity");
		}
		if (!authorizationManager.canAccess(userInfo, projectSetting.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.CREATE).isAuthorized()) {
			throw new UnauthorizedException("Cannot create settings for this project");
		}
		validateProjectSetting(projectSetting, userInfo);
		String id = projectSettingsDao.create(projectSetting);
		return projectSettingsDao.get(id);
	}

	@Override
	@WriteTransaction
	public void updateProjectSetting(UserInfo userInfo, ProjectSetting projectSetting) throws DatastoreException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, projectSetting.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE).isAuthorized()) {
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
				&& !authorizationManager.canAccess(userInfo, projectSetting.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.DELETE).isAuthorized()) {
			throw new UnauthorizedException("Cannot delete settings from this project");
		}
		projectSettingsDao.delete(id);
	}

	@Override
	public <T extends StorageLocationSetting> T createStorageLocationSetting(UserInfo userInfo, T storageLocationSetting)
			throws DatastoreException, NotFoundException, IOException {
		if (storageLocationSetting instanceof ExternalS3StorageLocationSetting) {
			UserProfile userProfile = userProfileManager.getUserProfile(userInfo.getId().toString());
			ExternalS3StorageLocationSetting externalS3StorageLocationSetting = (ExternalS3StorageLocationSetting) storageLocationSetting;

			//A valid bucket name must also be a valid domain name
			ValidateArgument.requirement(InternetDomainName.isValid(externalS3StorageLocationSetting.getBucket()), "Invalid Bucket Name");

			validateS3BucketAccess(externalS3StorageLocationSetting);
			validateS3BucketOwnership(externalS3StorageLocationSetting, userProfile);
		} else if (storageLocationSetting instanceof ExternalGoogleCloudStorageLocationSetting) {
			UserProfile userProfile = userProfileManager.getUserProfile(userInfo.getId().toString());
			ExternalGoogleCloudStorageLocationSetting externalGoogleCloudStorageLocationSetting = (ExternalGoogleCloudStorageLocationSetting) storageLocationSetting;
			//A valid bucket name must also be a valid domain name
			ValidateArgument.requirement(InternetDomainName.isValid(externalGoogleCloudStorageLocationSetting.getBucket()), "Invalid Bucket Name");

			externalGoogleCloudStorageLocationSetting.setUploadType(UploadType.GOOGLECLOUDSTORAGE);
			validateGoogleCloudBucketOwnership(externalGoogleCloudStorageLocationSetting, userProfile);
		} else if (storageLocationSetting instanceof ExternalStorageLocationSetting) {
			ExternalStorageLocationSetting externalStorageLocationSetting = (ExternalStorageLocationSetting) storageLocationSetting;
			ValidateArgument.required(externalStorageLocationSetting.getUrl(), "url");
			ValidateArgument.validUrl(externalStorageLocationSetting.getUrl());
		}else if (storageLocationSetting instanceof ExternalObjectStorageLocationSetting){
			ExternalObjectStorageLocationSetting externalObjectStorageLocationSetting = (ExternalObjectStorageLocationSetting) storageLocationSetting;

			//strip leading and trailing slashes and whitespace from the endpointUrl and bucket
			String strippedEndpoint = StringUtils.strip(externalObjectStorageLocationSetting.getEndpointUrl(), "/ \t");

			//validate url
			ValidateArgument.validUrl(strippedEndpoint);
			//A valid bucket name must also be a valid domain name
			ValidateArgument.requirement(InternetDomainName.isValid(externalObjectStorageLocationSetting.getBucket()), "Invalid Bucket Name");

			//passed validation, set endpoint as the stripped version
			externalObjectStorageLocationSetting.setEndpointUrl(strippedEndpoint);
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
	
	@Override
	public MergeStorageLocationsResponse mergeDuplicateStorageLocations(UserInfo userInfo) throws DatastoreException, UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		
		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("Only administrators can invoke this service");
		}
		
		// Maps a duplicate storage location id to the id of the storage location we keep
		Map<Long, Long> duplicatesMap = new HashMap<>();
		
		for (Long id : storageLocationDAO.findAllWithDuplicates()) {
			
			Set<Long> duplicates = storageLocationDAO.findDuplicates(id);
	
			// Updates all the file handles
			if (!duplicates.isEmpty()) {
				fileHandleDao.updateStorageLocationBatch(duplicates, id);
			}
			
			duplicates.forEach(duplicateId  -> duplicatesMap.put(duplicateId, id));
		}
		
		Long updatedProjectsCount = 0l;

		if (!duplicatesMap.isEmpty()) {
			// Updates the projects referencing the duplicate ids
			updatedProjectsCount = removeDuplicateStorageLocationsFromProjects(duplicatesMap);
			
			// Finally drop the unused storage location settings
			storageLocationDAO.deleteBatch(duplicatesMap.keySet());
		}
		
		MergeStorageLocationsResponse response = new MergeStorageLocationsResponse();
		
		response.setDuplicateLocationsCount((long) duplicatesMap.size());
		response.setUpdatedProjectsCount(updatedProjectsCount);

		return response;
	}
	
	Long removeDuplicateStorageLocationsFromProjects(Map<Long, Long> duplicatesMap) {
		Long updatedProjectsCount = 0l;
		
		Iterator<ProjectSetting> projectSettings = projectSettingsDao.getByType(ProjectSettingsType.upload);
		
		while (projectSettings.hasNext()) {
			ProjectSetting projectSetting = projectSettings.next();
			
			if (!(projectSetting instanceof UploadDestinationListSetting)) {
				continue;
			}
			
			UploadDestinationListSetting projectLocationSetting = (UploadDestinationListSetting) projectSetting;
			
			List<Long> currentLocations = projectLocationSetting.getLocations();
			List<Long> updatedLocations = new ArrayList<>(projectLocationSetting.getLocations().size());
			
			boolean updated = false;
			
			for (Long projectLocation : currentLocations) {
				Long keptLocation = duplicatesMap.get(projectLocation);
				if (keptLocation == null) {
					updatedLocations.add(projectLocation);
				} else {
					// Makes sure that we are not adding a duplicate
					if (!currentLocations.contains(keptLocation)) {
						updatedLocations.add(keptLocation);
					}
					updated = true;
				}
			}
			
			if (updated) {
				projectLocationSetting.setLocations(updatedLocations);
				projectSettingsDao.update(projectLocationSetting);
				updatedProjectsCount++;
			}
			
		}
		
		return updatedProjectsCount;
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
		ValidateArgument.required(setting.getLocations(), "settings.locations");
		ValidateArgument.requirement(setting.getLocations().size() >= 1, "settings.locations must at least have one entry");
		ValidateArgument.requirement(setting.getLocations().size() <= MAX_LOCATIONS_PER_PROJECT, "The maximum number of settings.locations is limited to " + MAX_LOCATIONS_PER_PROJECT);
		
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
										+ EXTERNAL_STORAGE_HELP);
					}
				}
			} catch (NotFoundException e) {
				ValidateArgument.failRequirement("uploadId " + uploadId + " is not a valid upload destination location");
			}
		}
	}

	private void validateS3BucketAccess(ExternalS3StorageLocationSetting externalS3StorageLocationSetting) {
		s3client.getRegionForBucket(externalS3StorageLocationSetting.getBucket());
	}

	private void validateS3BucketOwnership(ExternalS3StorageLocationSetting externalS3StorageLocationSetting, UserProfile userProfile)
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
				if (key.equals(OWNER_MARKER)) {
					throw new IllegalArgumentException("Did not find S3 object at key " + key + " from bucket " + bucket + ". "
							+ getExplanation(userProfile, bucket, key));
				} else {
					throw new IllegalArgumentException("Did not find S3 object at key " + key + " from bucket " + bucket + ". If the S3 object is in a folder, please make sure you specify a trailing '/' in the base key. " + getExplanation(userProfile, bucket, key));
				}
			} else {
				throw new IllegalArgumentException("Could not read S3 object at key " + key + " from bucket " + bucket + ": "
						+ e.getMessage() + ". " + getExplanation(userProfile, bucket, key));
			}
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(s3object.getObjectContent()));
		inspectUsername(reader, userProfile, bucket, key);
	}

	private void validateGoogleCloudBucketOwnership(ExternalGoogleCloudStorageLocationSetting externalGoogleCloudStorageLocationSetting, UserProfile userProfile)
			throws IOException, NotFoundException {
		String bucket = externalGoogleCloudStorageLocationSetting.getBucket();
		String key = (externalGoogleCloudStorageLocationSetting.getBaseKey() == null ? "" : externalGoogleCloudStorageLocationSetting.getBaseKey())
				+ OWNER_MARKER;

		try {
			if (!googleCloudStorageClient.bucketExists(bucket)) {
				throw new IllegalArgumentException("Did not find Google Cloud bucket " + bucket + ". " + getExplanation(userProfile, bucket, key));
			}
		} catch (StorageException e) {
			if (e.getMessage().contains("does not have storage.buckets.get access to")) {
				throw new IllegalArgumentException("Synapse does not have access to the Google Cloud bucket " + bucket + ". See " + EXTERNAL_STORAGE_HELP);
			} else {
				throw e;
			}
		}

		if (googleCloudStorageClient.getObject(bucket, key) == null) {
			throw new IllegalArgumentException("Did not find Google Cloud object at key " + key + " from bucket " + bucket + ". If the object is in a folder, please make sure you specify a trailing '/' in the base key. " + getExplanation(userProfile, bucket, key));
		}


		inspectUsername(googleCloudStorageClient.getObjectContent(bucket, key), userProfile, bucket, key);
	}


	private void inspectUsername(BufferedReader reader, UserProfile userProfile, String bucket, String key) throws IOException {
		String userName;
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

	private static final String SECURITY_EXPLANATION = "For security purposes, Synapse needs to establish that %s has permission to write to the bucket. Please create an object in bucket '%s' with key '%s' that contains the user name '%s'. Also see "
			+ EXTERNAL_STORAGE_HELP;

	private static String getExplanation(UserProfile userProfile, String bucket, String key) {
		return String.format(SECURITY_EXPLANATION, userProfile.getUserName(), bucket, key, userProfile.getUserName());
	}
}
