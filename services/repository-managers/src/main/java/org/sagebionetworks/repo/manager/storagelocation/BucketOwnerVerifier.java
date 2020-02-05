package org.sagebionetworks.repo.manager.storagelocation;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;

/**
 * Generic interface to verify bucket ownership (e.g. S3 or GC)
 * 
 * @author Marco
 */
public interface BucketOwnerVerifier {
	
	public static final String OWNER_MARKER = "owner.txt";

	/**
	 * Verifies that the given user is the owner of the bucket with the given name, the
	 * 
	 * @param userInfo
	 * @param bucketName The bucket name
	 * @param baseKey    Optional base key
	 * @param reader     A {@link BucketObjectReader} that allows to open an input stream toward a key in the given bucket
	 */
	void verifyBucketOwnership(UserInfo userInfo, BucketOwnerStorageLocationSetting storageLocation);

}
