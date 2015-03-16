package org.sagebionetworks.repo.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.util.CollectionUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;

public class ProjectSettingsUtil {

	private static final String OWNER_MARKER = "owner.txt";
	private static final String EXTERNAL_S3_HELP = "www.synapes.org//#!Help:ExternalS3 for more information on how to create a new external s3 upload destination";

	public static void validateProjectSetting(ProjectSetting setting, UserInfo currentUser, UserProfileManager userProfileManager,
			StorageLocationDAO storageLocationDAO) {
		ValidateArgument.required(setting.getProjectId(), "projectId");
		ValidateArgument.required(setting.getSettingsType(), "settingsType");
		if (setting instanceof UploadDestinationListSetting) {
			ProjectSettingsUtil.validateUploadDestinationListSetting((UploadDestinationListSetting) setting, currentUser, userProfileManager,
					storageLocationDAO);
		} else {
			ValidateArgument.failRequirement("Cannot handle project setting of type " + setting.getClass().getName());
		}
	}

	private static void validateUploadDestinationListSetting(UploadDestinationListSetting setting, UserInfo currentUser,
			UserProfileManager userProfileManager, StorageLocationDAO storageLocationDAO) {
		ValidateArgument.requirement(CollectionUtils.isEmpty(setting.getDestinations()), "setting.getDestinations() cannot have a value.");
		ValidateArgument.required(setting.getLocations(), "settings.locations");
		ValidateArgument.requirement(setting.getLocations().size() >= 1, "settings.locations must at least have one entry");
		for (Long uploadId : setting.getLocations()) {
			try {
				StorageLocationSetting storageLocationSetting = storageLocationDAO.get(uploadId);
				if (storageLocationSetting instanceof ExternalS3StorageLocationSetting) {
					// only the owner or an admin can add this setting to a project
					if (!currentUser.isAdmin() && !currentUser.getId().equals(storageLocationSetting.getCreatedBy())) {
						UserProfile owner = userProfileManager.getUserProfile(currentUser, storageLocationSetting.getCreatedBy()
								.toString());
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

	public static void validateOwnership(ExternalS3StorageLocationSetting externalS3StorageLocationSetting,
			UserProfile userProfile, AmazonS3Client s3client) throws IOException, NotFoundException {
		// check the ownership of the S3 bucket against the user
		String bucket = externalS3StorageLocationSetting.getBucket();
		String key = externalS3StorageLocationSetting.getBaseKey() + OWNER_MARKER;

		S3Object s3object;
		try {
			s3object = s3client.getObject(bucket, key);
		} catch (AmazonServiceException e) {
			if ("NoSuchBucket".equals(e.getErrorCode())) {
				throw new IllegalArgumentException("Did not find S3 bucket " + bucket + ". " + getExplanation(userProfile, bucket, key));
			} else if ("NoSuchKey".equals(e.getErrorCode())) {
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
