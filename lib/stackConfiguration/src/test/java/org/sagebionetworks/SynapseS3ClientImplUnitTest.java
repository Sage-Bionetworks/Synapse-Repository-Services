package org.sagebionetworks;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.aws.SynapseS3ClientImpl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.Region;

@RunWith(MockitoJUnitRunner.class)
public class SynapseS3ClientImplUnitTest {
	
	@Mock
	private AmazonS3 mockAmazonUSStandardClient;
	
	@Mock
	private AmazonS3 mockAmazonClient;
	
	private SynapseS3ClientImpl client;
	
	// name and region for the US Standard client
	private static final String BUCKET_NAME = "bucket-name";
	private static final Region BUCKET_REGION_US_STANDARD = Region.US_Standard;
	
	// region for a non-US-Standard client
	private static final Region BUCKET_REGION = Region.US_West;
	
	private static final String OBJECT_KEY = "s3-object-key";

	@Before
	public void before() {
		Map<Region, AmazonS3> regionSpecificClients = new HashMap<Region, AmazonS3>();
		regionSpecificClients.put(BUCKET_REGION_US_STANDARD, mockAmazonUSStandardClient);
		regionSpecificClients.put(BUCKET_REGION, mockAmazonClient);
		client = new SynapseS3ClientImpl(regionSpecificClients);
		
		when(mockAmazonUSStandardClient.getBucketLocation(BUCKET_NAME)).thenReturn(BUCKET_REGION.getFirstRegionId());
	}
	
	@Test
	public void testDeleteObject() {
		// method under test
		client.deleteObject(BUCKET_NAME, OBJECT_KEY);
		
		verify(mockAmazonClient).deleteObject(BUCKET_NAME, OBJECT_KEY);
	}

	@Test
	public void testDeleteObjects() {		
		DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(BUCKET_NAME);

		// method under test
		client.deleteObjects(deleteObjectsRequest);
		
		verify(mockAmazonClient).deleteObjects(deleteObjectsRequest);
	}

}
