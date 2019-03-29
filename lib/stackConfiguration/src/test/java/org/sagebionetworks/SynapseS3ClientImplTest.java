package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.aws.AwsClientFactory;
import org.sagebionetworks.aws.SynapseS3ClientImpl;

import com.amazonaws.services.s3.model.Region;

public class SynapseS3ClientImplTest {
	
	private static final SynapseS3ClientImpl synapseS3Client = (SynapseS3ClientImpl)AwsClientFactory.createAmazonS3Client();;
	
	private static final String S3_BUCKET_NAME = StackConfigurationSingleton.singleton().getS3Bucket();

	@Test
	void testGetRegionForBucket() {
		assertEquals(Region.US_Standard, synapseS3Client.getRegionForBucket(S3_BUCKET_NAME));
	}

	@Test
	void testGetS3ClientForBucket() {
		assertEquals(Region.US_Standard, synapseS3Client.getS3ClientForBucket(S3_BUCKET_NAME).getRegion());
	}
	
	@Test
	public void getStandardRegionClient() {
		assertEquals(Region.US_Standard, synapseS3Client.getUSStandardAmazonClient().getRegion());
	}

}
