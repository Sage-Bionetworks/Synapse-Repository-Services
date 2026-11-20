package org.sagebionetworks.aws.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class AwsClientFactoryV2Test {

	@Test
	public void testCreateS3Client() {
		// call under test
		S3Client client = AwsClientFactoryV2.createS3Client(Region.US_WEST_1);

		assertEquals(Region.US_WEST_1, client.serviceClientConfiguration().region());
	}

	@Test
	public void testCreateS3ClientProvider() {
		// call under test
		S3ClientProvider provider = AwsClientFactoryV2.createS3ClientProvider();

		assertEquals(Region.US_EAST_1, provider.getUsEast1Client().serviceClientConfiguration().region());
	}

}
