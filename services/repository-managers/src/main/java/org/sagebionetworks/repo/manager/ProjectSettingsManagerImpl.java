package org.sagebionetworks.repo.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StsStorageLocationSetting;
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
	private SynapseS3Client s3client;

	@Autowired
	private SynapseGoogleCloudStorageClient googleCloudStorageClient;

	@Autowired
	private FileHandleDao fileHandleDao;

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
	public <T extends ProjectSetting> T getProjectSettingForNode(UserInfo userInfo, String nodeId, ProjectSettingsType type,
			Class<T> expectedType) throws DatastoreException, UnauthorizedException, NotFoundException {
		String projectSettingId = projectSettingsDao.getInheritedProjectSetting(nodeId);
		if (projectSettingId == null) {
			// Not having a setting is normal.
			return null;
		}

		// Note that get throws NotFoundException if the project setting somehow doesn't exist.
		ProjectSetting projectSetting = projectSettingsDao.get(projectSettingId);
		if (!expectedType.isInstance(projectSetting)) {
			throw new IllegalArgumentException("Settings type for '" + type + "' is not of type " + expectedType.getName());
		}
		return expectedType.cast(projectSetting);
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
		if (EntityTypeUtils.getClassForType(nodeType) != Project.class
				&& EntityTypeUtils.getClassForType(nodeType) != Folder.class) {
			throw new IllegalArgumentException("The id is not the id of a project or folder entity");
		}
		if (!authorizationManager
				.canAccess(userInfo, parentId, ObjectType.ENTITY, ACCESS_TYPE.CREATE)
				.isAuthorized()) {
			throw new UnauthorizedException("Cannot create settings for this project");
		}

		// Can't create project settings if a parent has an StsStorageLocation.
		ProjectSetting parentSetting = getProjectSettingForNode(userInfo, parentId, ProjectSettingsType.upload,
				ProjectSetting.class);
		if (isStsStorageLocationSetting(parentSetting)) {
			throw new IllegalArgumentException("Can't override project settings in an STS-enabled folder path");
		}

		validateProjectSetting(projectSetting, userInfo);

		// Can't add an StsStorageLocation to a non-empty entity.
		if (!nodeManager.isEntityEmpty(parentId) && isStsStorageLocationSetting(projectSetting)) {
			throw new IllegalArgumentException("Can't enable STS in a non-empty folder");
		}

		String id = projectSettingsDao.create(projectSetting);
		return projectSettingsDao.get(id);
	}

	@Override
	@WriteTransaction
	public void updateProjectSetting(UserInfo userInfo, ProjectSetting projectSetting)
			throws DatastoreException, NotFoundException {
		if (!authorizationManager
				.canAccess(userInfo, projectSetting.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)
				.isAuthorized()) {
			throw new UnauthorizedException("Cannot update settings on this project");
		}
		validateProjectSetting(projectSetting, userInfo);

		// Can't add or modify an StsStorageLocation on a non-empty entity.
		if (!nodeManager.isEntityEmpty(projectSetting.getProjectId())) {
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
		if (!authorizationManager
				.canAccess(userInfo, projectSetting.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.DELETE)
				.isAuthorized()) {
			throw new UnauthorizedException("Cannot delete settings from this project");
		}

		// Can't delete an StsStorageLocation on a non-empty entity.
		if (!nodeManager.isEntityEmpty(projectSetting.getProjectId()) && isStsStorageLocationSetting(projectSetting)) {
			throw new IllegalArgumentException("Can't disable STS in a non-empty folder");
		}

		projectSettingsDao.delete(id);
	}

	@Override
	public <T extends StorageLocationSetting> T createStorageLocationSetting(UserInfo userInfo,
			T storageLocationSetting) throws DatastoreException, NotFoundException, IOException {
		if (storageLocationSetting instanceof ExternalS3StorageLocationSetting) {
			ExternalS3StorageLocationSetting externalS3StorageLocationSetting = (ExternalS3StorageLocationSetting) storageLocationSetting;
			// A valid bucket name must also be a valid domain name
			ValidateArgument.requirement(InternetDomainName.isValid(externalS3StorageLocationSetting.getBucket()),
					"Invalid Bucket Name");

			externalS3StorageLocationSetting.setUploadType(UploadType.S3);
			validateS3BucketAccess(externalS3StorageLocationSetting);
			validateS3BucketOwnership(externalS3StorageLocationSetting, getBucketOwnerAliases(userInfo.getId()));
		} else if (storageLocationSetting instanceof ExternalGoogleCloudStorageLocationSetting) {
			ExternalGoogleCloudStorageLocationSetting externalGoogleCloudStorageLocationSetting = (ExternalGoogleCloudStorageLocationSetting) storageLocationSetting;
			// A valid bucket name must also be a valid domain name
			ValidateArgument.requirement(
					InternetDomainName.isValid(externalGoogleCloudStorageLocationSetting.getBucket()),
					"Invalid Bucket Name");

			externalGoogleCloudStorageLocationSetting.setUploadType(UploadType.GOOGLECLOUDSTORAGE);
			validateGoogleCloudBucketOwnership(externalGoogleCloudStorageLocationSetting, getBucketOwnerAliases(userInfo.getId()));
		} else if (storageLocationSetting instanceof ExternalStorageLocationSetting) {
			ExternalStorageLocationSetting externalStorageLocationSetting = (ExternalStorageLocationSetting) storageLocationSetting;
			ValidateArgument.required(externalStorageLocationSetting.getUrl(), "url");
			ValidateArgument.validExternalUrl(externalStorageLocationSetting.getUrl());
		} else if (storageLocationSetting instanceof ExternalObjectStorageLocationSetting) {
			ExternalObjectStorageLocationSetting externalObjectStorageLocationSetting = (ExternalObjectStorageLocationSetting) storageLocationSetting;

			// strip leading and trailing slashes and whitespace from the endpointUrl and bucket
			String strippedEndpoint = StringUtils.strip(externalObjectStorageLocationSetting.getEndpointUrl(), "/ \t");

			// validate url
			ValidateArgument.validExternalUrl(strippedEndpoint);
			// A valid bucket name must also be a valid domain name
			ValidateArgument.requirement(InternetDomainName.isValid(externalObjectStorageLocationSetting.getBucket()),
					"Invalid Bucket Name");

			// passed validation, set endpoint as the stripped version
			externalObjectStorageLocationSetting.setEndpointUrl(strippedEndpoint);
		} else if (storageLocationSetting instanceof ProxyStorageLocationSettings) {
			ProxyStorageLocationSettings proxySettings = (ProxyStorageLocationSettings) storageLocationSetting;
			ValidateArgument.required(proxySettings.getProxyUrl(), "proxyUrl");
			ValidateArgument.required(proxySettings.getSecretKey(), "secretKey");
			if (proxySettings.getSecretKey().length() < MIN_SECRET_KEY_CHARS) {
				throw new IllegalArgumentException("SecretKey must be at least: " + MIN_SECRET_KEY_CHARS
						+ " characters but was: " + proxySettings.getSecretKey().length());
			}
			try {
				URL proxyUrl = new URL(proxySettings.getProxyUrl());
				if (!"https".equals(proxyUrl.getProtocol())) {
					throw new IllegalArgumentException("proxyUrl protocol must be be HTTPS");
				}
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException("proxyUrl is malformed: " + e.getMessage());
			}
		} else if (storageLocationSetting instanceof S3StorageLocationSetting) {
			S3StorageLocationSetting synapseS3StorageLocationSetting = (S3StorageLocationSetting) storageLocationSetting;
			if (synapseS3StorageLocationSetting.getBaseKey() != null) {
				throw new IllegalArgumentException("Cannot specify baseKey when creating an S3StorageLocationSetting");
			}

			if (Boolean.TRUE.equals(synapseS3StorageLocationSetting.getStsEnabled())) {
				// This is the S3 bucket we own, so we need to auto-generate the base key.
				String baseKey = userInfo.getId() + "/" + UUID.randomUUID();
				synapseS3StorageLocationSetting.setBaseKey(baseKey);
			} else {
				// If STS is not enabled, clear the base key.
				synapseS3StorageLocationSetting.setBaseKey(null);
			}

			storageLocationSetting.setUploadType(UploadType.S3);
		}

		// Default UploadType to null.
		if (storageLocationSetting.getUploadType() == null) {
			storageLocationSetting.setUploadType(UploadType.NONE);
		}

		storageLocationSetting.setCreatedBy(userInfo.getId());
		storageLocationSetting.setCreatedOn(new Date());
		Long uploadId = storageLocationDAO.create(storageLocationSetting);
		return (T) storageLocationDAO.get(uploadId);
	}

	@Override
	public List<StorageLocationSetting> getMyStorageLocationSettings(UserInfo userInfo)
			throws DatastoreException, NotFoundException {
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
	public StorageLocationSetting getStorageLocationSetting(Long storageLocationId)
			throws DatastoreException, NotFoundException {
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
		ValidateArgument.required(setting.getLocations(), "settings.locations");
		ValidateArgument.requirement(setting.getLocations().size() >= 1,
				"settings.locations must at least have one entry");
		ValidateArgument.requirement(setting.getLocations().size() <= MAX_LOCATIONS_PER_PROJECT,
				"The maximum number of settings.locations is limited to " + MAX_LOCATIONS_PER_PROJECT);

		for (Long uploadId : setting.getLocations()) {
			try {
				StorageLocationSetting storageLocationSetting = storageLocationDAO.get(uploadId);
				if (storageLocationSetting instanceof ExternalS3StorageLocationSetting) {
					// only the owner or an admin can add this setting to a project
					if (!currentUser.isAdmin() && !currentUser.getId().equals(storageLocationSetting.getCreatedBy())) {
						String ownerUsername = principalAliasDAO.getUserName(storageLocationSetting.getCreatedBy());
						throw new UnauthorizedException(
								"Only the owner of the external S3 upload destination (user "
										+ ownerUsername
										+ ") can add this upload destination to a project. Either ask that user to perform this operation or follow the steps to create a new external s3 upload destination (see "
										+ EXTERNAL_STORAGE_HELP);
					}
				}

				// STS storage locations have additional restrictions.
				if (storageLocationSetting instanceof StsStorageLocationSetting &&
						Boolean.TRUE.equals(((StsStorageLocationSetting) storageLocationSetting).getStsEnabled())) {
					// Can only be applied to folders.
					EntityType nodeType = nodeManager.getNodeType(currentUser, setting.getProjectId());
					if (EntityType.folder != nodeType) {
						throw new IllegalArgumentException("Can only enable STS on a folder");
					}

					// Cannot be applied with other storage locations.
					if (setting.getLocations().size() != 1) {
						throw new IllegalArgumentException("An STS-enabled folder cannot add other upload " +
								"destinations");
					}
				}
			} catch (NotFoundException e) {
				ValidateArgument
						.failRequirement("uploadId " + uploadId + " is not a valid upload destination location");
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

	private void validateS3BucketAccess(ExternalS3StorageLocationSetting externalS3StorageLocationSetting) {
		s3client.getRegionForBucket(externalS3StorageLocationSetting.getBucket());
	}

	private void validateS3BucketOwnership(ExternalS3StorageLocationSetting externalS3StorageLocationSetting, List<PrincipalAlias> ownerAliases)
			throws IOException, NotFoundException {
		// PLFM-5769: Check that the key is valid
		if (externalS3StorageLocationSetting.getBaseKey() != null && externalS3StorageLocationSetting.getBaseKey().endsWith("/")) {
			throw new IllegalArgumentException("Base keys cannot end with trailing slashes");
		}

		// check the ownership of the S3 bucket against the user
		String bucket = externalS3StorageLocationSetting.getBucket();
		// If there is no base key, look for "owner.txt"; otherwise, look for "baseKey/owner.txt"
		String ownerKey = (externalS3StorageLocationSetting.getBaseKey() == null ? ""
				: (externalS3StorageLocationSetting.getBaseKey() + "/")) + OWNER_MARKER;

		S3Object s3object;
		try {
			s3object = s3client.getObject(bucket, ownerKey);
		} catch (AmazonServiceException e) {
			if (AmazonErrorCodes.S3_BUCKET_NOT_FOUND.equals(e.getErrorCode())) {
				throw new IllegalArgumentException(
						"Did not find S3 bucket " + bucket + ". " + getExplanation(ownerAliases, bucket, ownerKey));
			} else if (AmazonErrorCodes.S3_NOT_FOUND.equals(e.getErrorCode())
					|| AmazonErrorCodes.S3_KEY_NOT_FOUND.equals(e.getErrorCode())) {
				if (ownerKey.equals(OWNER_MARKER)) {
					throw new IllegalArgumentException("Did not find S3 object at key " + ownerKey + " from bucket " + bucket
							+ ". " + getExplanation(ownerAliases, bucket, ownerKey));
				} else {
					throw new IllegalArgumentException("Did not find S3 object at key " + ownerKey + " from bucket " + bucket
							+ ". If the S3 object is in a folder, please make sure you specify a trailing '/' in the base key. "
							+ getExplanation(ownerAliases, bucket, ownerKey));
				}
			} else {
				throw new IllegalArgumentException("Could not read S3 object at key " + ownerKey + " from bucket " + bucket
						+ ": " + e.getMessage() + ". " + getExplanation(ownerAliases, bucket, ownerKey));
			}
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(s3object.getObjectContent(), StandardCharsets.UTF_8));
		inspectUsername(reader, ownerAliases, bucket, ownerKey);
	}

	private void validateGoogleCloudBucketOwnership(ExternalGoogleCloudStorageLocationSetting externalGoogleCloudStorageLocationSetting, List<PrincipalAlias> ownerAliases)
			throws IOException, NotFoundException {
		String bucket = externalGoogleCloudStorageLocationSetting.getBucket();
		String key = (externalGoogleCloudStorageLocationSetting.getBaseKey() == null ? ""
				: externalGoogleCloudStorageLocationSetting.getBaseKey()) + OWNER_MARKER;

		try {
			if (!googleCloudStorageClient.bucketExists(bucket)) {
				throw new IllegalArgumentException(
						"Did not find Google Cloud bucket " + bucket + ". " + getExplanation(ownerAliases, bucket, key));
			}
		} catch (StorageException e) {
			if (e.getMessage().contains("does not have storage.buckets.get access to")) {
				throw new IllegalArgumentException("Synapse does not have access to the Google Cloud bucket " + bucket
						+ ". See " + EXTERNAL_STORAGE_HELP);
			} else {
				throw e;
			}
		}

		if (googleCloudStorageClient.getObject(bucket, key) == null) {
			throw new IllegalArgumentException("Did not find Google Cloud object at key " + key + " from bucket "
					+ bucket
					+ ". If the object is in a folder, please make sure you specify a trailing '/' in the base key. "
					+ getExplanation(ownerAliases, bucket, key));
		}

		BufferedReader content = new BufferedReader(new InputStreamReader(googleCloudStorageClient.getObjectContent(bucket, key), StandardCharsets.UTF_8));
		inspectUsername(content, ownerAliases, bucket, key);
	}


	void inspectUsername(BufferedReader reader, List<PrincipalAlias> expectedAliases, String bucket, String key) throws IOException {
		String actualUsername;
		try {
			actualUsername = reader.readLine();
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not read username from key " + key + " from bucket " + bucket
					+ ". " + getExplanation(expectedAliases, bucket, key));
		} finally {
			reader.close();
		}

		if (StringUtils.isBlank(actualUsername)) {
			throw new IllegalArgumentException("No username found under key " + key + " from bucket " + bucket + ". "
					+ getExplanation(expectedAliases, bucket, key));
		}

		if (!checkForCorrectName(expectedAliases, actualUsername)) {
			throw new IllegalArgumentException("The username " + actualUsername + " found under key " + key + " from bucket "
					+ bucket + " is not what was expected. " + getExplanation(expectedAliases, bucket, key));
		}
	}

	private boolean checkForCorrectName(List<PrincipalAlias> allowedNames, String actualUsername) {
		if (allowedNames != null) {
			for (PrincipalAlias name : allowedNames) {
				if (name.getAlias().equalsIgnoreCase(actualUsername)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Collects the possible aliases that can be used to verify ownership of an S3 bucket.
	 * Currently, this is a user's username and their email addresses.
	 * @param userId
	 * @return
	 */
	private List<PrincipalAlias> getBucketOwnerAliases(Long userId) {
		return principalAliasDAO.listPrincipalAliases(userId, AliasType.USER_NAME, AliasType.USER_EMAIL);
	}

	private static final String EXTERNAL_STORAGE_HELP = "http://docs.synapse.org/articles/custom_storage_location.html for more information on how to create a new external upload destination.";
	private static final String SECURITY_EXPLANATION = "For security purposes, Synapse needs to establish that %s has permission to write to the bucket. Please create an object in bucket '%s' with key '%s' that contains the text '%s'. Also see "
			+ EXTERNAL_STORAGE_HELP;

	private static String getExplanation(List<PrincipalAlias> aliases, String bucket, String key) {
		String username = aliases.get(0).getAlias();
		for (PrincipalAlias pa : aliases) {
			if (pa.getType().equals(AliasType.USER_NAME)) {
				username = pa.getAlias();
				break;
			}
		}
		return String.format(SECURITY_EXPLANATION, username, bucket, key, username);
	}
}
