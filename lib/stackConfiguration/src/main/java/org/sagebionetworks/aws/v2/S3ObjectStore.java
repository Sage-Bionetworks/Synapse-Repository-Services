package org.sagebionetworks.aws.v2;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.aws.CannotDetermineBucketLocationException;

/**
 * Object level access to S3: read, write, delete, describe, tag and pre-sign a download. Bucket
 * administration, multipart upload, CORS configuration, object restore and listing are deliberately
 * absent — a consumer that needs one of those takes a raw SDK client instead.
 * <p>
 * No SDK type appears on this interface: payloads are {@link InputStream} or {@link File}, metadata
 * is an {@link S3ObjectInfo}, and write settings are an {@link S3WriteOptions}. This keeps the SDK
 * version an implementation detail of this package rather than something every caller imports.
 * <p>
 * A bucket that cannot be reached at all — it does not exist, it is not shared with Synapse, or the
 * SDK cannot route to its region — surfaces as a {@link CannotDetermineBucketLocationException} from
 * every method here. Failures scoped to the object itself, a missing key most of all, propagate as
 * the SDK's own exception.
 */
public interface S3ObjectStore {

	/**
	 * Opens the content of an object for reading. The caller owns the returned stream and must close
	 * it.
	 *
	 * @throws CannotDetermineBucketLocationException If the bucket cannot be reached
	 */
	InputStream getObject(String bucket, String key);

	/**
	 * Downloads the content of an object, replacing the destination file if it already exists.
	 *
	 * @throws CannotDetermineBucketLocationException If the bucket cannot be reached
	 */
	void getObject(String bucket, String key, File destination);

	/**
	 * Looks up the metadata S3 holds for an object, which doubles as an existence check.
	 *
	 * @return Empty when no object exists under the key
	 * @throws CannotDetermineBucketLocationException If the bucket cannot be reached
	 */
	Optional<S3ObjectInfo> getObjectInfo(String bucket, String key);

	/**
	 * Writes an object from a stream. The caller owns the stream and must close it.
	 *
	 * @param contentLength The exact number of bytes to read from the stream, which S3 requires up
	 *                      front
	 * @throws CannotDetermineBucketLocationException If the bucket cannot be reached
	 */
	void putObject(String bucket, String key, InputStream content, long contentLength, S3WriteOptions options);

	/**
	 * Writes an object from a file.
	 *
	 * @throws CannotDetermineBucketLocationException If the bucket cannot be reached
	 */
	void putObject(String bucket, String key, File file, S3WriteOptions options);

	/**
	 * Deletes an object. Deleting a key that does not exist succeeds, as it does in S3 itself.
	 *
	 * @throws CannotDetermineBucketLocationException If the bucket cannot be reached
	 */
	void deleteObject(String bucket, String key);

	/**
	 * Builds a URL that grants anonymous read of an object until it expires.
	 *
	 * @param expiration How long the URL remains valid, measured from now
	 * @throws CannotDetermineBucketLocationException If the bucket cannot be reached
	 */
	URL presignGetObject(String bucket, String key, Duration expiration);

	/**
	 * Builds a URL that grants anonymous read of an object until it expires, overriding the response
	 * headers S3 returns when the URL is redeemed.
	 *
	 * @param expiration How long the URL remains valid, measured from now
	 * @throws CannotDetermineBucketLocationException If the bucket cannot be reached
	 */
	URL presignGetObject(String bucket, String key, Duration expiration, S3ResponseHeaders responseHeaders);

	/**
	 * @return The tags currently on the object, empty when it has none
	 * @throws CannotDetermineBucketLocationException If the bucket cannot be reached
	 */
	List<S3ObjectTag> getObjectTags(String bucket, String key);

	/**
	 * Replaces the full set of tags on an object.
	 *
	 * @throws CannotDetermineBucketLocationException If the bucket cannot be reached
	 */
	void setObjectTags(String bucket, String key, List<S3ObjectTag> tags);

	/**
	 * Probes a bucket to confirm Synapse can reach it, without touching any object in it.
	 *
	 * @throws CannotDetermineBucketLocationException If the bucket cannot be reached
	 */
	void verifyBucketAccess(String bucket);

	/**
	 * @return True when both buckets live in the same region, which is a precondition of a
	 *         server-side copy between them
	 * @throws CannotDetermineBucketLocationException If either bucket cannot be reached
	 */
	boolean isSameRegion(String bucketOne, String bucketTwo);

}
