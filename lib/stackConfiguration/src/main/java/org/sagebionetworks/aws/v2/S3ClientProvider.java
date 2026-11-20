package org.sagebionetworks.aws.v2;

import org.sagebionetworks.aws.CannotDetermineBucketLocationException;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Provides the AWS SDK v2 S3 client matching the region of a given bucket.
 */
public interface S3ClientProvider {

	/**
	 * Find the region of the given bucket.
	 *
	 * @param bucketName The name of the bucket
	 * @return The region the bucket resides in
	 * @throws CannotDetermineBucketLocationException If the region of the bucket cannot be determined
	 */
	Region getRegionForBucket(String bucketName) throws CannotDetermineBucketLocationException;

	/**
	 * @param bucketName The name of the bucket
	 * @return A client for the region of the given bucket
	 * @throws CannotDetermineBucketLocationException If the region of the bucket cannot be determined
	 */
	S3Client getClientForBucket(String bucketName) throws CannotDetermineBucketLocationException;

	/**
	 * @return A client for the us-east-1 region, used for operations that are not bound to a specific bucket
	 */
	S3Client getUsEast1Client();

}
