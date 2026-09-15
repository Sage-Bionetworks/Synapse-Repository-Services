package org.sagebionetworks.aws.v2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Delegates to a single cross-region {@link S3Client}, which discovers a bucket's region itself
 * after a redirect, so this class holds no per-region client map.
 * <p>
 * Pre-signing is the exception: it is local computation with no request to learn a region from, so
 * {@link S3Presigner} must be told the region up front. Bucket regions are therefore resolved here
 * with a head-bucket probe and cached for an hour, and one presigner is built lazily per region.
 * <p>
 * Bucket-level failures are translated to {@link CannotDetermineBucketLocationException}, which
 * several consumers catch for control flow. The trigger is an error code of NoSuchBucket or
 * PermanentRedirect, or any 403 — see {@link #isBucketAccessFailure(S3Exception)}.
 */
public class S3ObjectStoreImpl implements S3ObjectStore {

	private static final long REGION_CACHE_TTL_HOURS = 1;

	private static final String NO_SUCH_BUCKET = "NoSuchBucket";
	private static final String PERMANENT_REDIRECT = "PermanentRedirect";

	private final S3Client s3Client;
	private final Function<Region, S3Presigner> presignerBuilder;
	private final Map<String, Region> bucketRegions;
	private final ConcurrentMap<Region, S3Presigner> presigners;

	public S3ObjectStoreImpl(S3Client s3Client) {
		this(s3Client, AwsClientFactoryV2::createS3Presigner);
	}

	S3ObjectStoreImpl(S3Client s3Client, Function<Region, S3Presigner> presignerBuilder) {
		this.s3Client = s3Client;
		this.presignerBuilder = presignerBuilder;
		this.bucketRegions = Collections
				.synchronizedMap(new PassiveExpiringMap<String, Region>(REGION_CACHE_TTL_HOURS, TimeUnit.HOURS));
		this.presigners = new ConcurrentHashMap<>();
	}

	@Override
	public InputStream getObject(String bucket, String key) {
		requiredNotEmpty(bucket, "bucket");
		requiredNotEmpty(key, "key");

		return translateBucketFailures(bucket,
				() -> s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build()));
	}

	@Override
	public void getObject(String bucket, String key, File destination) {
		requiredNotEmpty(bucket, "bucket");
		requiredNotEmpty(key, "key");
		if (destination == null) {
			throw new IllegalArgumentException("destination is required.");
		}

		// The SDK's own file transformer refuses to overwrite, but callers hand us a temp file that
		// already exists, so the copy is done here instead.
		try (InputStream content = getObject(bucket, key)) {
			Files.copy(content, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public Optional<S3ObjectInfo> getObjectInfo(String bucket, String key) {
		requiredNotEmpty(bucket, "bucket");
		requiredNotEmpty(key, "key");

		HeadObjectResponse response;

		try {
			response = s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
		} catch (S3Exception e) {
			if (isBucketAccessFailure(e)) {
				throw cannotDetermineBucketLocation(bucket, e);
			}
			if (HttpStatusCode.NOT_FOUND == e.statusCode()) {
				return Optional.empty();
			}
			throw e;
		}

		return Optional.of(S3ObjectInfo.builder()
				.withContentType(response.contentType())
				.withContentEncoding(response.contentEncoding())
				.withEtag(response.eTag())
				.withContentLength(response.contentLength())
				.withArchiveStatus(response.archiveStatusAsString())
				.withOngoingRestore(S3ObjectInfo.parseOngoingRestore(response.restore()))
				.build());
	}

	@Override
	public void putObject(String bucket, String key, InputStream content, long contentLength, S3WriteOptions options) {
		requiredNotEmpty(bucket, "bucket");
		requiredNotEmpty(key, "key");
		if (content == null) {
			throw new IllegalArgumentException("content is required.");
		}

		PutObjectRequest request = newPutRequest(bucket, key, options);

		translateBucketFailures(bucket,
				() -> s3Client.putObject(request, RequestBody.fromInputStream(content, contentLength)));
	}

	@Override
	public void putObject(String bucket, String key, File file, S3WriteOptions options) {
		requiredNotEmpty(bucket, "bucket");
		requiredNotEmpty(key, "key");
		if (file == null) {
			throw new IllegalArgumentException("file is required.");
		}

		PutObjectRequest request = newPutRequest(bucket, key, options);

		translateBucketFailures(bucket, () -> s3Client.putObject(request, RequestBody.fromFile(file)));
	}

	@Override
	public void deleteObject(String bucket, String key) {
		requiredNotEmpty(bucket, "bucket");
		requiredNotEmpty(key, "key");

		translateBucketFailures(bucket,
				() -> s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build()));
	}

	@Override
	public URL presignGetObject(String bucket, String key, Duration expiration) {
		return presignGetObject(bucket, key, expiration, null);
	}

	@Override
	public URL presignGetObject(String bucket, String key, Duration expiration, S3ResponseHeaders responseHeaders) {
		requiredNotEmpty(bucket, "bucket");
		requiredNotEmpty(key, "key");
		if (expiration == null) {
			throw new IllegalArgumentException("expiration is required.");
		}

		GetObjectRequest.Builder objectRequest = GetObjectRequest.builder().bucket(bucket).key(key);

		if (responseHeaders != null) {
			objectRequest.responseContentType(responseHeaders.getContentType())
					.responseContentDisposition(responseHeaders.getContentDisposition());
		}

		return presignerForBucket(bucket).presignGetObject(GetObjectPresignRequest.builder()
				.signatureDuration(expiration)
				.getObjectRequest(objectRequest.build())
				.build()).url();
	}

	@Override
	public List<S3ObjectTag> getObjectTags(String bucket, String key) {
		requiredNotEmpty(bucket, "bucket");
		requiredNotEmpty(key, "key");

		List<Tag> tagSet = translateBucketFailures(bucket,
				() -> s3Client.getObjectTagging(GetObjectTaggingRequest.builder().bucket(bucket).key(key).build()))
						.tagSet();

		List<S3ObjectTag> tags = new ArrayList<>(tagSet.size());

		for (Tag tag : tagSet) {
			tags.add(new S3ObjectTag(tag.key(), tag.value()));
		}

		return tags;
	}

	@Override
	public void setObjectTags(String bucket, String key, List<S3ObjectTag> tags) {
		requiredNotEmpty(bucket, "bucket");
		requiredNotEmpty(key, "key");
		if (tags == null) {
			throw new IllegalArgumentException("tags is required.");
		}

		List<Tag> tagSet = new ArrayList<>(tags.size());

		for (S3ObjectTag tag : tags) {
			tagSet.add(Tag.builder().key(tag.getKey()).value(tag.getValue()).build());
		}

		translateBucketFailures(bucket, () -> s3Client.putObjectTagging(PutObjectTaggingRequest.builder()
				.bucket(bucket)
				.key(key)
				.tagging(Tagging.builder().tagSet(tagSet).build())
				.build()));
	}

	@Override
	public void verifyBucketAccess(String bucket) {
		resolveBucketRegion(bucket);
	}

	@Override
	public boolean isSameRegion(String bucketOne, String bucketTwo) {
		return resolveBucketRegion(bucketOne).equals(resolveBucketRegion(bucketTwo));
	}

	private PutObjectRequest newPutRequest(String bucket, String key, S3WriteOptions options) {
		if (options == null) {
			throw new IllegalArgumentException("options is required, use S3WriteOptions.empty() for S3 defaults.");
		}

		PutObjectRequest.Builder builder = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentType(options.getContentType())
				.contentEncoding(options.getContentEncoding())
				.contentDisposition(options.getContentDisposition())
				.contentMD5(options.getContentMd5Base64());

		if (options.getStorageClass() != null) {
			builder.storageClass(toStorageClass(options.getStorageClass()));
		}

		if (options.getCannedAcl() != null) {
			builder.acl(toObjectCannedAcl(options.getCannedAcl()));
		}

		return builder.build();
	}

	static StorageClass toStorageClass(S3StorageClass storageClass) {
		switch (storageClass) {
		case STANDARD:
			return StorageClass.STANDARD;
		case INTELLIGENT_TIERING:
			return StorageClass.INTELLIGENT_TIERING;
		default:
			throw new IllegalArgumentException("Unsupported storage class: " + storageClass);
		}
	}

	static ObjectCannedACL toObjectCannedAcl(S3CannedAcl cannedAcl) {
		switch (cannedAcl) {
		case BUCKET_OWNER_FULL_CONTROL:
			return ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL;
		case PUBLIC_READ:
			return ObjectCannedACL.PUBLIC_READ;
		default:
			throw new IllegalArgumentException("Unsupported canned ACL: " + cannedAcl);
		}
	}

	private S3Presigner presignerForBucket(String bucket) {
		return presigners.computeIfAbsent(resolveBucketRegion(bucket), presignerBuilder);
	}

	/**
	 * Resolves the region of a bucket with a head-bucket probe, caching the answer for an hour.
	 * Unlike the operations above, any failure here means the bucket could not be located at all, so
	 * every SDK exception is translated.
	 */
	private Region resolveBucketRegion(String bucket) {
		requiredNotEmpty(bucket, "bucket");

		Region cached = bucketRegions.get(bucket);

		if (cached != null) {
			return cached;
		}

		HeadBucketResponse response;

		try {
			response = s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
		} catch (S3Exception e) {
			throw cannotDetermineBucketLocation(bucket, e);
		}

		Region region = response.bucketRegion() == null ? Region.US_EAST_1 : Region.of(response.bucketRegion());

		bucketRegions.put(bucket, region);

		return region;
	}

	private <T> T translateBucketFailures(String bucket, Supplier<T> operation) {
		try {
			return operation.get();
		} catch (S3Exception e) {
			if (isBucketAccessFailure(e)) {
				throw cannotDetermineBucketLocation(bucket, e);
			}
			throw e;
		}
	}

	/**
	 * Distinguishes "Synapse cannot reach this bucket" from a failure scoped to the object. A
	 * cross-region client only learns a bucket's region reactively, so an unreachable bucket has no
	 * single failure mode: it surfaces as NoSuchBucket, as a PermanentRedirect the SDK could not
	 * follow, or as a 403 from whichever operation was attempted.
	 */
	static boolean isBucketAccessFailure(S3Exception e) {
		if (HttpStatusCode.FORBIDDEN == e.statusCode()) {
			return true;
		}

		AwsErrorDetails details = e.awsErrorDetails();
		String errorCode = details == null ? null : details.errorCode();

		return NO_SUCH_BUCKET.equals(errorCode) || PERMANENT_REDIRECT.equals(errorCode);
	}

	private static CannotDetermineBucketLocationException cannotDetermineBucketLocation(String bucket, S3Exception e) {
		return new CannotDetermineBucketLocationException("Failed to determine the Amazon region for bucket '" + bucket
				+ "'. Please ensure that the bucket exists, is shared with Synapse, in particular granting ListObject permission.",
				e);
	}

	private static void requiredNotEmpty(String value, String name) {
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException(name + " is required.");
		}
	}

}
