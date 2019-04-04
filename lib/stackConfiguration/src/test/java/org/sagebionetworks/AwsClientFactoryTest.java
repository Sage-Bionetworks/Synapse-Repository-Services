package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.aws.AwsClientFactory;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.Region;

class AwsClientFactoryTest {

	@Test
	void testGetS3RegionForAWSRegions() {
		assertEquals(Region.US_Standard, AwsClientFactory.getS3RegionForAWSRegions(Regions.US_EAST_1));
		assertEquals(Region.US_West, AwsClientFactory.getS3RegionForAWSRegions(Regions.US_WEST_1));
		assertEquals(Region.US_GovCloud, AwsClientFactory.getS3RegionForAWSRegions(Regions.GovCloud));
		assertEquals(Region.EU_Ireland, AwsClientFactory.getS3RegionForAWSRegions(Regions.EU_WEST_1));
	}
	
	@Test
	void testClientGeneration() throws Exception {
		assertEquals(Region.US_Standard, 
				AwsClientFactory.createAmazonS3Client().getUSStandardAmazonClient().getRegion());
	}

}
