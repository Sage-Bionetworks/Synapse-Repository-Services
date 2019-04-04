package org.sagebionetworks;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.aws.AwsClientFactory;
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;
import org.sagebionetworks.aws.SynapseS3ClientImpl;

import com.amazonaws.services.s3.model.Region;

public class SynapseS3ClientImplTest {
	
	private static SynapseS3ClientImpl synapseS3Client;
	
	private static final String S3_BUCKET_NAME = StackConfigurationSingleton.singleton().getS3Bucket();

	
	@BeforeClass
	public static void beforeClass() {
		synapseS3Client = (SynapseS3ClientImpl)AwsClientFactory.createAmazonS3Client();
	}

	@Test
	public void testGetRegionForBucketHappyCase() {
		assertEquals(Region.US_Standard, synapseS3Client.getRegionForBucket(S3_BUCKET_NAME));
	}

	@Test(expected=CannotDetermineBucketLocationException.class)
	public void testGetRegionNonexistentBucket() {
		synapseS3Client.getRegionForBucket("some-nonexistent-bucket");
	}

	@Test
	public void testGetStandardRegionClient() {
		assertEquals(Region.US_Standard, synapseS3Client.getUSStandardAmazonClient().getRegion());
	}

	@Test
	public void testGetRegionForBucketOrAssumeUSStandard() {
		assertEquals(Region.US_Standard, synapseS3Client.getRegionForBucketOrAssumeUSStandard(S3_BUCKET_NAME));
	}

	@Test
	public void testGetRegionForBucketOrAssumeUSStandard_NonexistentBucket() {
		assertEquals(Region.US_Standard, synapseS3Client.getRegionForBucketOrAssumeUSStandard("some-nonexistent-bucket"));
	}

	@Test
	public void testGetS3ClientForBucketOrAssumeUSStandard() {
		assertEquals(Region.US_Standard, synapseS3Client.getS3ClientForBucketOrAssumeUSStandard(S3_BUCKET_NAME).getRegion());
	}
	

}
