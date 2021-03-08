package org.sagebionetworks.repo.manager.file;

import java.util.UUID;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.jdo.NameValidation;
import org.sagebionetworks.repo.model.project.BaseKeyStorageLocationSetting;
import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.util.ValidateArgument;
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
		} else if (storageLocationSetting instanceof BucketOwnerStorageLocationSetting) {
			bucket = ((BucketOwnerStorageLocationSetting) storageLocationSetting).getBucket();
		} else {
			throw new IllegalArgumentException("Cannot get bucket from storage location setting type " + storageLocationSetting.getClass());
		}
		return bucket;
	}

	/** Create a new S3 key given a file name and storage location settings. */
	public static String createNewKey(String userId, String fileName, StorageLocationSetting storageLocationSetting) {
		// S3 has token signing issues when the key contains non-ascii characters
		NameValidation.validateName(fileName);
		String base = "";
		if (storageLocationSetting instanceof BaseKeyStorageLocationSetting) {
			BaseKeyStorageLocationSetting baseKeyStorageLocationSetting = (BaseKeyStorageLocationSetting) storageLocationSetting;
			if (!StringUtils.isEmpty(baseKeyStorageLocationSetting.getBaseKey())) {
				base = baseKeyStorageLocationSetting.getBaseKey() + FILE_TOKEN_TEMPLATE_SEPARATOR;
			}
		}
		return String.format(FILE_TOKEN_TEMPLATE, base, userId, UUID.randomUUID().toString(), fileName);
	}
}
