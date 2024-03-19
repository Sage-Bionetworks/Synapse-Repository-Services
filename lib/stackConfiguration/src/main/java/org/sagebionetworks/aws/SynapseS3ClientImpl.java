package org.sagebionetworks.aws;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.map.PassiveExpiringMap;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.services.s3.model.HeadBucketResult;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.util.StringUtils;

/*
 * 
 * This is a facade for AmazonS3 (Amazon's S3 Client), exposing just the methods used by Synapse
 * and, in each method, doing the job of figuring out which region the given bucket is in, 
 * so that the S3 Client for that region is used.
 * 
 */
public class SynapseS3ClientImpl implements SynapseS3Client {

	private Map<Region, AmazonS3> regionSpecificClients;

	private Map<String, Region> bucketLocation;

	public SynapseS3ClientImpl(Map<Region, AmazonS3> regionSpecificClients) {
		this.regionSpecificClients=regionSpecificClients;
		bucketLocation = Collections.synchronizedMap(new PassiveExpiringMap<>(1, TimeUnit.HOURS));
	}
	
	public Region getRegionForBucket(String bucketName) {
		if (StringUtils.isNullOrEmpty(bucketName)) throw new IllegalArgumentException("bucketName is required.");
		Region result = bucketLocation.get(bucketName);
		if (result!=null) return result;
		String location = null;
		try {
			// previously we used getBucketLocation but for some regions it doesn't work if the client is not in the correct region!!! :^(
			HeadBucketRequest headBucketRequest = new HeadBucketRequest(bucketName);
			HeadBucketResult headBucketResult = getUSStandardAmazonClient().headBucket(headBucketRequest);
			location = headBucketResult.getBucketRegion();
		}  catch (AmazonS3Exception e) {
			throw new CannotDetermineBucketLocationException("Failed to determine the Amazon region for bucket '"+bucketName+
					"'. Please ensure that the bucket exists, is shared with Synapse, in particular granting ListObject permission.", e);
		}
		if (StringUtils.isNullOrEmpty(location)) {
			result = Region.US_Standard;
		} else {
			result =  Region.fromValue(location);	
		}
		bucketLocation.put(bucketName, result);
		return result;
	}

	public AmazonS3 getS3ClientForBucket(String bucket) {
		Region region = getRegionForBucket(bucket);
		return regionSpecificClients.get(region);
	}

	public AmazonS3 getUSStandardAmazonClient() {
		return regionSpecificClients.get(Region.US_Standard);
	}

	@Override
	public Bucket createBucket(String bucketName) throws SdkClientException, AmazonServiceException {
		return getUSStandardAmazonClient().createBucket(bucketName);
	}

}
