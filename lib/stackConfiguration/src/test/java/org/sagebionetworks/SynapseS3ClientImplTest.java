package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.aws.AwsClientFactory;
import org.sagebionetworks.aws.SynapseS3ClientImpl;

import com.amazonaws.services.s3.model.Region;

@RunWith(MockitoJUnitRunner.class)
public class SynapseS3ClientImplTest {
	
	private static SynapseS3ClientImpl synapseS3Client;
	
	private static final String S3_BUCKET_NAME = StackConfigurationSingleton.singleton().getS3Bucket();

	
	@BeforeClass
	public static void beforeClass() {
		synapseS3Client = (SynapseS3ClientImpl)AwsClientFactory.createAmazonS3Client();
	}

	@Test
	public void testGetRegionForBucket() {
		assertEquals(Region.US_Standard, synapseS3Client.getRegionForBucketOrAssumeUSStandard(S3_BUCKET_NAME));
	}

	@Test
	public void testGetS3ClientForBucket() {
		assertEquals(Region.US_Standard, synapseS3Client.getS3ClientForBucketOrAssumeUSStandard(S3_BUCKET_NAME).getRegion());
	}
	
	@Test
	public void testGetStandardRegionClient() {
		assertEquals(Region.US_Standard, synapseS3Client.getUSStandardAmazonClient().getRegion());
	}
	
	@Test
	public void testLookupNonStandardRegion() {
		System.out.println(synapseS3Client.getRegionForBucketOrAssumeUSStandard("cf-templates-a60ndmakkdpc-us-west-2"));
		assertNotNull(synapseS3Client.getS3ClientForBucketOrAssumeUSStandard("cf-templates-a60ndmakkdpc-us-west-2"));
	}

}
