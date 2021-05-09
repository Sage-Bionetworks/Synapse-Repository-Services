package org.sagebionetworks.repo.manager.sts;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.google.common.collect.ImmutableMap;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.MultipartUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StsStorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.sts.StsCredentials;
import org.sagebionetworks.repo.model.sts.StsPermission;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class StsManagerImpl implements StsManager {
	static final int DURATION_SECONDS = 12 * 60 * 60; // 12 hours
	private static final String POLICY_TEMPLATE_FILENAME = "sts-policy-template.json";

	// The AWS IAM policy string for the actions the user is allowed to do.
	private static final Map<StsPermission, String> PERMISSION_TO_POLICY_ACTIONS =
			ImmutableMap.<StsPermission, String>builder()
					.put(StsPermission.read_only, "\"s3:GetObject\",\"s3:ListBucket\"")
					.put(StsPermission.read_write, "\"s3:AbortMultipartUpload\",\"s3:DeleteObject\",\"s3:GetObject\",\"s3:ListBucket\"," +
							"\"s3:ListMultipartUploadParts\",\"s3:PutObject\",\"s3:ListBucketMultipartUploads\"")
					.build();

	@Autowired
	private EntityAuthorizationManager authManager;

	@Autowired
	private FileHandleManager fileHandleManager;

	@Autowired
	private ProjectSettingsManager projectSettingsManager;

	@Autowired
	private StackConfiguration stackConfiguration;

	@Autowired
	private AWSSecurityTokenService stsClient;

	private final VelocityEngine velocityEngine;

	/** Initializes the STS Manager. */
	public StsManagerImpl() {
		velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		velocityEngine.setProperty("runtime.references.strict", true);
	}

	@Override
	public StsCredentials getTemporaryCredentials(UserInfo userInfo, String entityId, StsPermission permission) {
		// Validate args.
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(permission, "permission");

		// Entity must have an STS-enabled storage location.
		Optional<UploadDestinationListSetting> projectSetting = projectSettingsManager.getProjectSettingForNode(
				userInfo, entityId, ProjectSettingsType.upload, UploadDestinationListSetting.class);
		if (!projectSetting.isPresent()) {
			throw new IllegalArgumentException("Entity must have a project setting");
		}
		if (!projectSettingsManager.isStsStorageLocationSetting(projectSetting.get())) {
			throw new IllegalArgumentException("Entity must have an STS-enabled storage location");
		}
		// Shortcut: STS-enabled project settings can only have 1 storage location.
		long storageLocationId = projectSetting.get().getLocations().get(0);
		StsStorageLocationSetting storageLocationSetting = (StsStorageLocationSetting) projectSettingsManager
				.getStorageLocationSetting(storageLocationId);

		if (permission == StsPermission.read_write && storageLocationSetting instanceof S3StorageLocationSetting) {
			throw new IllegalArgumentException("STS write access is not allowed in Synapse storage");
		}

		if (permission == StsPermission.read_write) {
			// For write permissions, we need download, update, create, and delete.
			authManager.hasAccess(userInfo, entityId, ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.UPDATE, ACCESS_TYPE.CREATE, ACCESS_TYPE.DELETE)
					.checkAuthorizationOrElseThrow();
		}else {
			// for read-only access we need download.
			authManager.hasAccess(userInfo, entityId, ACCESS_TYPE.DOWNLOAD)
					.checkAuthorizationOrElseThrow();
		}

		String bucket = MultipartUtils.getBucket(storageLocationSetting);

		// Optional base key. Convert null to blank so we can do string substitution correctly.
		String baseKey = storageLocationSetting.getBaseKey();
		String bucketWithFolder;
		String folderWithTrailingSlash;
		if (baseKey == null) {
			baseKey = "";
			bucketWithFolder = bucket;
			folderWithTrailingSlash = "";
		} else {
			bucketWithFolder = bucket + "/" + baseKey;
			folderWithTrailingSlash = baseKey + "/";
		}

		// Generate policy doc.
		String actions = PERMISSION_TO_POLICY_ACTIONS.get(permission);
		VelocityContext context = new VelocityContext();
		context.put("actions", actions);
		context.put("bucket", bucket);
		context.put("bucketWithFolder", bucketWithFolder);
		context.put("folder", baseKey);
		context.put("folderWithTrailingSlash", folderWithTrailingSlash);

		Template template = velocityEngine.getTemplate(POLICY_TEMPLATE_FILENAME);
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		String policy = writer.toString();

		// Call STS.
		AssumeRoleRequest request = new AssumeRoleRequest();
		request.setDurationSeconds(DURATION_SECONDS);
		request.setPolicy(policy);
		request.setRoleArn(stackConfiguration.getTempCredentialsIamRoleArn());

		// Session Name is required, but it has a max length of 64 characters. Keep it short but descriptive.
		request.setRoleSessionName("sts-" + userInfo.getId() + "-" + entityId);

		AssumeRoleResult result = stsClient.assumeRole(request);

		// Convert credentials to our own home-made class, so that our service can marshall it to/from JSON.
		Credentials awsCredentials = result.getCredentials();
		StsCredentials stsCredentials = new StsCredentials();
		stsCredentials.setBucket(bucket);
		stsCredentials.setBaseKey(storageLocationSetting.getBaseKey());
		stsCredentials.setAccessKeyId(awsCredentials.getAccessKeyId());
		stsCredentials.setSecretAccessKey(awsCredentials.getSecretAccessKey());
		stsCredentials.setSessionToken(awsCredentials.getSessionToken());
		stsCredentials.setExpiration(awsCredentials.getExpiration());
		return stsCredentials;
	}

	@Override
	public void validateCanAddFile(UserInfo userInfo, String fileHandleId, String parentId) {
		// Is the file STS-enabled?
		// Note that getRawFileHandle throws if the file handle exists, but the storage location ID might be null.
		FileHandle fileHandle = fileHandleManager.getRawFileHandleUnchecked(fileHandleId);
		Long fileStorageLocationId = fileHandle.getStorageLocationId();
		StorageLocationSetting fileStorageLocationSetting = projectSettingsManager.getStorageLocationSetting(
				fileStorageLocationId);
		boolean fileStsEnabled = projectSettingsManager.isStsStorageLocationSetting(fileStorageLocationSetting);

		// Is the parent STS-enabled?
		Long parentStorageLocationId = null;
		boolean parentStsEnabled = false;
		Optional<UploadDestinationListSetting> projectSetting = projectSettingsManager.getProjectSettingForNode(
				userInfo, parentId, ProjectSettingsType.upload, UploadDestinationListSetting.class);
		if (projectSetting.isPresent()) {
			// Short-cut: Just grab the first storage location ID. We only compare storage location IDs if STS is
			// enabled, and folders with STS enabled can't have multiple storage locations.
			parentStsEnabled = projectSettingsManager.isStsStorageLocationSetting(projectSetting.get());
			parentStorageLocationId = projectSetting.get().getLocations().get(0);
		}

		// If either the file's storage location or the parent's storage location has STS enabled, then the storage
		// locations must be the same. ie, Files in STS-enabled Storage Locations must be placed in a folder with the
		// same storage location, and folders with STS-enabled Storage Locations can only contain files from that
		// storage location.
		if ((fileStsEnabled || parentStsEnabled) && !Objects.equals(fileStorageLocationId, parentStorageLocationId)) {
			// Determine which error message to throw depending on whether the file is STS-enabled or the parent.
			if (fileStsEnabled) {
				throw new IllegalArgumentException("Files in STS-enabled storage locations can only be placed in " +
						"folders with the same storage location");
			}
			//noinspection ConstantConditions
			if (parentStsEnabled) {
				throw new IllegalArgumentException("Folders with STS-enabled storage locations can only accept " +
						"files with the same storage location");
			}
		}
	}

	@Override
	public void validateCanMoveFolder(UserInfo userInfo, String moveCandidateId, String oldParentId, String newParentId) {
		if (oldParentId.equals(newParentId)) {
			// Folder is not being moved. Trivial.
			return;
		}

		// Folder is being moved. STS restrictions may apply.
		boolean isCandidateStsRoot = false;
		boolean oldStsEnabled = false;
		Long oldStorageLocationId = null;
		boolean newStsEnabled = false;
		Long newStorageLocationId = null;

		// If the project setting is defined on the folder directly (ie the folder is a "root folder"), special logic
		// applies.
		Optional<ProjectSetting> folderProjectSetting = projectSettingsManager.getProjectSettingByProjectAndType(
				userInfo, moveCandidateId, ProjectSettingsType.upload);
		if (folderProjectSetting.isPresent()) {
			isCandidateStsRoot = true;

			// Short-cut: Just grab the first storage location ID. We only compare storage location IDs if STS is
			// enabled, and folders with STS enabled can't have multiple storage locations.
			oldStsEnabled = projectSettingsManager.isStsStorageLocationSetting(folderProjectSetting.get());
			oldStorageLocationId = ((UploadDestinationListSetting) folderProjectSetting.get()).getLocations().get(0);
		} else {
			// If the project setting isn't defined on the folder directly, check the project settings in the old
			// parent's hierarchy. Note that we do it like this because this validation is called for both folder moves
			// and for restoring from the trash can, so the folder's current parent hierarchy might not match the
			// original parent hierarchy.
			Optional<UploadDestinationListSetting> oldParentProjectSetting = projectSettingsManager
					.getProjectSettingForNode(userInfo, oldParentId, ProjectSettingsType.upload,
							UploadDestinationListSetting.class);
			if (oldParentProjectSetting.isPresent()) {
				// Similar shortcut as per above.
				oldStsEnabled = projectSettingsManager.isStsStorageLocationSetting(oldParentProjectSetting.get());
				oldStorageLocationId = oldParentProjectSetting.get().getLocations().get(0);
			}
		}

		// Check new parent project settings.
		Optional<UploadDestinationListSetting> newProjectSetting = projectSettingsManager
				.getProjectSettingForNode(userInfo, newParentId, ProjectSettingsType.upload,
						UploadDestinationListSetting.class);
		if (newProjectSetting.isPresent()) {
			// Similar shortcut as per above.
			newStsEnabled = projectSettingsManager.isStsStorageLocationSetting(newProjectSetting.get());
			newStorageLocationId = newProjectSetting.get().getLocations().get(0);
		}

		if (isCandidateStsRoot && oldStsEnabled) {
			// The folder that we are moving is itself an STS-enabled folder. This is fine, as long as we
			// aren't moving it into another STS-enabled folder. Note that even if the other STS-enabled folder
			// is the same storage location, this is still a problem because it violates the "can't override
			// project settings" constraint and causes weird things to happen.
			if (newStsEnabled) {
				throw new IllegalArgumentException("Cannot place an STS-enabled folder inside another " +
						"STS-enabled folder");
			}
		} else if ((oldStsEnabled || newStsEnabled) && !Objects.equals(oldStorageLocationId,
				newStorageLocationId)) {
			// If the storage location is different, this means we're moving into or out of an STS-enabled
			// folder, which is not allowed.
			// Determine which error message to show depending on whether we're moving into our out of an
			// STS-enabled folder.
			if (oldStsEnabled) {
				throw new IllegalArgumentException("Folders in STS-enabled storage locations can only " +
						"be placed in folders with the same storage location");
			}
			//noinspection ConstantConditions
			if (newStsEnabled) {
				throw new IllegalArgumentException("Non-STS-enabled folders cannot be placed inside " +
						"STS-enabled folders");
			}
		}
	}
}
