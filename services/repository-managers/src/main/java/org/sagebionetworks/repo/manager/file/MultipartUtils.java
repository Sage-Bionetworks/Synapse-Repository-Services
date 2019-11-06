package org.sagebionetworks.repo.manager.file;

import java.util.UUID;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.springframework.util.StringUtils;

public class MultipartUtils {

	// [base/]userid/UUID/filename
	public static final String FILE_TOKEN_TEMPLATE_SEPARATOR = "/";
	private static final String FILE_TOKEN_TEMPLATE = "%1$s%2$s" + FILE_TOKEN_TEMPLATE_SEPARATOR + "%3$s" + FILE_TOKEN_TEMPLATE_SEPARATOR
			+ "%4$s";
	
	/**
	 * Lookup the bucket to use given a storage location id.
	 * @param storageLocationSetting
	 * @return
	 */
	public static String getBucket(StorageLocationSetting storageLocationSetting) {
		String bucket;
		if (storageLocationSetting == null || storageLocationSetting instanceof S3StorageLocationSetting) {
			bucket = StackConfigurationSingleton.singleton().getS3Bucket();
		} else if (storageLocationSetting instanceof ExternalS3StorageLocationSetting) {
			bucket = ((ExternalS3StorageLocationSetting) storageLocationSetting).getBucket();
		} else if (storageLocationSetting instanceof ExternalGoogleCloudStorageLocationSetting) {
			bucket = ((ExternalGoogleCloudStorageLocationSetting) storageLocationSetting).getBucket();
		} else {
			throw new IllegalArgumentException("Cannot get bucket from storage location setting type " + storageLocationSetting.getClass());
		}
		return bucket;
	}
	
	/**
	 * Create a new S3 key given a file name and storage location settings.s
	 * @param userId
	 * @param fileName
	 * @param storageLocationSetting
	 * @return
	 */
	public static String createNewKey(String userId, String fileName, StorageLocationSetting storageLocationSetting) {
		String base = "";
		if (storageLocationSetting instanceof ExternalS3StorageLocationSetting) {
			ExternalS3StorageLocationSetting externalS3StorageLocationSetting = (ExternalS3StorageLocationSetting) storageLocationSetting;
			if (!StringUtils.isEmpty(externalS3StorageLocationSetting.getBaseKey())) {
				base = externalS3StorageLocationSetting.getBaseKey() + FILE_TOKEN_TEMPLATE_SEPARATOR;
			}
		} else if (storageLocationSetting instanceof ExternalGoogleCloudStorageLocationSetting) {
			ExternalGoogleCloudStorageLocationSetting externalGoogleCloudStorageLocationSetting = (ExternalGoogleCloudStorageLocationSetting) storageLocationSetting;
			if (!StringUtils.isEmpty(externalGoogleCloudStorageLocationSetting.getBaseKey())) {
				base = externalGoogleCloudStorageLocationSetting.getBaseKey() + FILE_TOKEN_TEMPLATE_SEPARATOR;
			}
		}
		return String.format(FILE_TOKEN_TEMPLATE, base, userId, UUID.randomUUID().toString(), fileName);
	}
	
	
}
