package org.sagebionetworks.aws.v2;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class S3ClientProviderImpl implements S3ClientProvider {

	private static final String BUCKET_REGION_HEADER = "x-amz-bucket-region";

	private static final int PERMANENT_REDIRECT_STATUS_CODE = 301;

	private static final long REGION_CACHE_TIMEOUT_HOURS = 1;

	private final Function<Region, S3Client> clientFactory;

	private final Map<Region, S3Client> regionSpecificClients;

	private final Map<String, Region> bucketRegions;

	/**
	 * @param clientFactory Supplies the client for a given region, invoked at most once per region
	 */
	public S3ClientProviderImpl(Function<Region, S3Client> clientFactory) {
		this.clientFactory = clientFactory;
		this.regionSpecificClients = new ConcurrentHashMap<>();
		this.bucketRegions = Collections
				.synchronizedMap(new PassiveExpiringMap<String, Region>(REGION_CACHE_TIMEOUT_HOURS, TimeUnit.HOURS));
	}

	@Override
	public Region getRegionForBucket(String bucketName) throws CannotDetermineBucketLocationException {
		if (bucketName == null || bucketName.trim().isEmpty()) {
			throw new IllegalArgumentException("bucketName is required.");
		}

		Region cachedRegion = bucketRegions.get(bucketName);

		if (cachedRegion != null) {
			return cachedRegion;
		}

		Region region = lookupRegionForBucket(bucketName);

		bucketRegions.put(bucketName, region);

		return region;
	}

	@Override
	public S3Client getClientForBucket(String bucketName) throws CannotDetermineBucketLocationException {
		return getClientForRegion(getRegionForBucket(bucketName));
	}

	@Override
	public S3Client getUsEast1Client() {
		return getClientForRegion(Region.US_EAST_1);
	}

	private S3Client getClientForRegion(Region region) {
		return regionSpecificClients.computeIfAbsent(region, clientFactory);
	}

	// HeadBucket is used rather than GetBucketLocation since the latter fails for some regions unless
	// the client already resides in the region of the bucket.
	private Region lookupRegionForBucket(String bucketName) {
		try {
			HeadBucketResponse response = getUsEast1Client()
					.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());

			return regionFromBucketRegion(response.bucketRegion());
		} catch (S3Exception e) {
			// A bucket outside of the client region may answer with a redirect that still carries the
			// region of the bucket, any other failure means the region cannot be established.
			return redirectRegion(e)
					.orElseThrow(() -> new CannotDetermineBucketLocationException(
							"Failed to determine the Amazon region for bucket '" + bucketName
									+ "'. Please ensure that the bucket exists, is shared with Synapse, in particular granting ListObject permission.",
							e));
		}
	}

	// S3 reports us-east-1 as an absent region
	private Region regionFromBucketRegion(String bucketRegion) {
		if (bucketRegion == null || bucketRegion.trim().isEmpty()) {
			return Region.US_EAST_1;
		}
		return Region.of(bucketRegion);
	}

	private Optional<Region> redirectRegion(S3Exception e) {
		if (e.statusCode() != PERMANENT_REDIRECT_STATUS_CODE || e.awsErrorDetails() == null
				|| e.awsErrorDetails().sdkHttpResponse() == null) {
			return Optional.empty();
		}

		return e.awsErrorDetails().sdkHttpResponse().firstMatchingHeader(BUCKET_REGION_HEADER)
				.filter(bucketRegion -> !bucketRegion.trim().isEmpty()).map(Region::of);
	}

}
