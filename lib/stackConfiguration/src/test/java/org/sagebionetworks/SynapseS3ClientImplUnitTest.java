package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;
import org.sagebionetworks.aws.SynapseS3ClientImpl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.BucketWebsiteConfiguration;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.HeadBucketResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.RestoreObjectRequest;
import com.amazonaws.services.s3.model.RestoreObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.SetObjectTaggingResult;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;

@ExtendWith(MockitoExtension.class)
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

	@BeforeEach
	public void before() {
		Map<Region, AmazonS3> regionSpecificClients = new HashMap<Region, AmazonS3>();
		regionSpecificClients.put(BUCKET_REGION_US_STANDARD, mockAmazonUSStandardClient);
		regionSpecificClients.put(BUCKET_REGION, mockAmazonClient);
		client = new SynapseS3ClientImpl(regionSpecificClients);
		
		// this setting is used by most tests in this class, overridden where necessary
		HeadBucketResult headBucketResult = new HeadBucketResult().withBucketRegion(BUCKET_REGION.getFirstRegionId());
		// Set as lenient as only one test is not using this
		Mockito.lenient().doReturn(headBucketResult).when(mockAmazonUSStandardClient).headBucket(any());
	}
	
	@Test
	public void testGetRegionForBucketNotUSStandard() {
		assertEquals(Region.US_West, client.getRegionForBucket(BUCKET_NAME));
	}
	
	@Test
	public void testGetRegionForBucketUSStandardAsNull() {
		when(mockAmazonUSStandardClient.headBucket(any())).thenReturn(new HeadBucketResult().withBucketRegion(null));
		assertEquals(Region.US_Standard, client.getRegionForBucket(BUCKET_NAME));
	}
	
	@Test
	public void testGetRegionForBucketUSStandardAsEmptyString() {
		// just in case they start returning a zero length string instead of null:
		when(mockAmazonUSStandardClient.headBucket(any())).thenReturn(new HeadBucketResult().withBucketRegion(""));
		assertEquals(Region.US_Standard, client.getRegionForBucket(BUCKET_NAME));
	}
	
	@Test
	public void testGetRegionForBucketCantTellRegion() {
		
		AmazonS3Exception s3Ex = new AmazonS3Exception("can't check region");
		
		when(mockAmazonUSStandardClient.headBucket(any())).thenThrow(s3Ex);
		
		CannotDetermineBucketLocationException ex = assertThrows(CannotDetermineBucketLocationException.class, () -> {			
			client.getRegionForBucket(BUCKET_NAME);
		});
		
		assertEquals(s3Ex, ex.getCause());
		assertEquals("Failed to determine the Amazon region for bucket '"+BUCKET_NAME+"'. Please ensure that the bucket exists, is shared with Synapse, in particular granting ListObject permission.", ex.getMessage());
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

	@Test
	public void testPutObject() {		
		ObjectMetadata objectMetadata = new ObjectMetadata();
		InputStream input=null;
		PutObjectResult expected = new PutObjectResult();
		when(mockAmazonClient.putObject(BUCKET_NAME, OBJECT_KEY, input, objectMetadata)).thenReturn(expected);
		
		// method under test
		PutObjectResult actual = client.putObject(BUCKET_NAME, OBJECT_KEY, input, objectMetadata);
		
		verify(mockAmazonClient).putObject(BUCKET_NAME, OBJECT_KEY, input, objectMetadata);
		assertEquals(expected, actual);		
	}

	@Test
	public void testPutObject2() {				
		File file= new File("foo");
		PutObjectRequest request = new PutObjectRequest(BUCKET_NAME, OBJECT_KEY, file);
		PutObjectResult expected = new PutObjectResult();
		when(mockAmazonClient.putObject(request)).thenReturn(expected);
		
		// method under test
		PutObjectResult actual = client.putObject(request);
		
		verify(mockAmazonClient).putObject(request);
		assertEquals(expected, actual);
	}


	@Test
	public void testPutObject3() {				
		File file= new File("foo");
		PutObjectResult expected = new PutObjectResult();
		when(mockAmazonClient.putObject(BUCKET_NAME, OBJECT_KEY, file)).thenReturn(expected);
		
		// method under test
		PutObjectResult actual = client.putObject(BUCKET_NAME, OBJECT_KEY, file);
		
		verify(mockAmazonClient).putObject(BUCKET_NAME, OBJECT_KEY, file);
		assertEquals(expected, actual);
	}

	@Test
	public void testGetObject() {				
		S3Object expected = new S3Object();
		when(mockAmazonClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenReturn(expected);
		
		// method under test
		S3Object actual = client.getObject(BUCKET_NAME, OBJECT_KEY);
		
		verify(mockAmazonClient).getObject(BUCKET_NAME, OBJECT_KEY);
		assertEquals(expected, actual);
	}

	@Test
	public void testGetObject2() {			
		GetObjectRequest request = new GetObjectRequest(BUCKET_NAME, OBJECT_KEY);
		
		S3Object expected = new S3Object();
		when(mockAmazonClient.getObject(request)).thenReturn(expected);
		
		// method under test
		S3Object actual = client.getObject(request);
		
		verify(mockAmazonClient).getObject(request);
		assertEquals(expected, actual);
	}

	@Test
	public void testGetObject3() {			
		File file= new File("foo");
		GetObjectRequest request = new GetObjectRequest(BUCKET_NAME, OBJECT_KEY);
		
		ObjectMetadata expected = new ObjectMetadata();
		when(mockAmazonClient.getObject(request, file)).thenReturn(expected);
		
		// method under test
		ObjectMetadata actual = client.getObject(request, file);
		
		verify(mockAmazonClient).getObject(request, file);
		assertEquals(expected, actual);
	}

	@Test
	public void testListObject() {				
		ObjectListing expected = new ObjectListing();
		String prefix = "PRE";
		when(mockAmazonClient.listObjects(BUCKET_NAME, prefix)).thenReturn(expected);
		
		// method under test
		ObjectListing actual = client.listObjects(BUCKET_NAME, prefix);
		
		verify(mockAmazonClient).listObjects(BUCKET_NAME, prefix);
		assertEquals(expected, actual);
	}

	@Test
	public void testListObject2() {				
		ObjectListing expected = new ObjectListing();
		ListObjectsRequest request = new ListObjectsRequest().withBucketName(BUCKET_NAME);
		when(mockAmazonClient.listObjects(request)).thenReturn(expected);
		
		// method under test
		ObjectListing actual = client.listObjects(request);
		
		verify(mockAmazonClient).listObjects(request);
		assertEquals(expected, actual);
	}

	@Test
	public void testCreateBucket() {
		
		Bucket expected = new Bucket(BUCKET_NAME);
		when(mockAmazonUSStandardClient.createBucket(BUCKET_NAME)).thenReturn(expected);
		
		// method under test
		Bucket actual = client.createBucket(BUCKET_NAME);
		
		verify(mockAmazonUSStandardClient).createBucket(BUCKET_NAME);
		assertEquals(expected, actual);
	}

	@Test
	public void testDoesObjectExist() {
		boolean expected = true;
		when(mockAmazonClient.doesObjectExist(BUCKET_NAME, OBJECT_KEY)).thenReturn(expected);

		// method under test
		boolean actual = client.doesObjectExist(BUCKET_NAME, OBJECT_KEY);
		
		verify(mockAmazonClient).doesObjectExist(BUCKET_NAME, OBJECT_KEY);
		assertEquals(expected, actual);
	}

	@Test
	public void testSetBucketCrossOriginConfiguration() {
		BucketCrossOriginConfiguration bucketCrossOriginConfiguration = new BucketCrossOriginConfiguration();
		
		// method under test
		client.setBucketCrossOriginConfiguration(BUCKET_NAME, bucketCrossOriginConfiguration);
		
		verify(mockAmazonClient).setBucketCrossOriginConfiguration(BUCKET_NAME, bucketCrossOriginConfiguration);
	}

	@Test
	public void testGeneratePresignedUrl() throws IOException {
		URL expected = new URL("http://someurl.org");
		GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(BUCKET_NAME, OBJECT_KEY);
		when(mockAmazonClient.generatePresignedUrl(request)).thenReturn(expected);

		// method under test
		URL actual = client.generatePresignedUrl(request);
		
		verify(mockAmazonClient).generatePresignedUrl(request);
		assertEquals(expected, actual);
	}

	@Test
	public void testInitiateMultipartUpload() {
		InitiateMultipartUploadResult expected = new InitiateMultipartUploadResult();
		InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(BUCKET_NAME, OBJECT_KEY);
		when(mockAmazonClient.initiateMultipartUpload(request)).thenReturn(expected);

		// method under test
		InitiateMultipartUploadResult actual = client.initiateMultipartUpload(request);
		
		verify(mockAmazonClient).initiateMultipartUpload(request);
		assertEquals(expected, actual);
	}

	@Test
	public void testCopyPart() {
		CopyPartResult expected = new CopyPartResult();
		CopyPartRequest request = new CopyPartRequest().withDestinationBucketName(BUCKET_NAME);
		when(mockAmazonClient.copyPart(request)).thenReturn(expected);

		// method under test
		CopyPartResult actual = client.copyPart(request);
		
		verify(mockAmazonClient).copyPart(request);
		assertEquals(expected, actual);
	}

	@Test
	public void testCompleteMultipartUpload() {
		CompleteMultipartUploadResult expected = new CompleteMultipartUploadResult();
		CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest().withBucketName(BUCKET_NAME);
		when(mockAmazonClient.completeMultipartUpload(request)).thenReturn(expected);

		// method under test
		CompleteMultipartUploadResult actual = client.completeMultipartUpload(request);
		
		verify(mockAmazonClient).completeMultipartUpload(request);
		assertEquals(expected, actual);
	}
	
	@Test
	public void testAbportMultipartUpload() {
		doNothing().when(mockAmazonClient).abortMultipartUpload(any());
		
		AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(BUCKET_NAME, OBJECT_KEY, "uploadId");
		
		// Call under test
		client.abortMultipartUpload(request);
		
		verify(mockAmazonClient).abortMultipartUpload(request);
	}

	@Test
	public void testBucketWebsiteConfiguration() {
		BucketWebsiteConfiguration bucketWebsiteConfiguration = new BucketWebsiteConfiguration();

		// method under test
		client.setBucketWebsiteConfiguration(BUCKET_NAME, bucketWebsiteConfiguration);
		
		verify(mockAmazonClient).setBucketWebsiteConfiguration(BUCKET_NAME, bucketWebsiteConfiguration);
	}

	@Test
	public void testSetBucketPolicy() {
		String policyText = "text";
		
		// method under test
		client.setBucketPolicy(BUCKET_NAME, policyText);
		
		verify(mockAmazonClient).setBucketPolicy(BUCKET_NAME, policyText);
	}

	@Test
	public void testGetBucketCrossOriginConfiguration() {				
		BucketCrossOriginConfiguration expected = new BucketCrossOriginConfiguration();
		when(mockAmazonClient.getBucketCrossOriginConfiguration(BUCKET_NAME)).thenReturn(expected);
		
		// method under test
		BucketCrossOriginConfiguration actual = client.getBucketCrossOriginConfiguration(BUCKET_NAME);
		
		verify(mockAmazonClient).getBucketCrossOriginConfiguration(BUCKET_NAME);
		assertEquals(expected, actual);
	}
	
	@Test
	public void testGetObjectTags() {
		
		List<Tag> tags = Arrays.asList(
				new Tag("key", "value"), new Tag("key2", "value")
		);
		
		when(mockAmazonClient.getObjectTagging(any())).thenReturn(new GetObjectTaggingResult(tags));
		
		// Call under test
		List<Tag> result = client.getObjectTags(BUCKET_NAME, OBJECT_KEY);
		
		assertEquals(tags, result);
		
		ArgumentCaptor<GetObjectTaggingRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectTaggingRequest.class);
		
		verify(mockAmazonClient).getObjectTagging(requestCaptor.capture());
		
		assertEquals(BUCKET_NAME, requestCaptor.getValue().getBucketName());
		assertEquals(OBJECT_KEY, requestCaptor.getValue().getKey());
	}
	
	@Test
	public void testSetObjectTags() {
		
		List<Tag> tags = Arrays.asList(
				new Tag("key", "value"), new Tag("key2", "value")
		);
		
		when(mockAmazonClient.setObjectTagging(any())).thenReturn(new SetObjectTaggingResult());
		
		// Call under test
		client.setObjectTags(BUCKET_NAME, OBJECT_KEY, tags);
		
		ArgumentCaptor<SetObjectTaggingRequest> requestCaptor = ArgumentCaptor.forClass(SetObjectTaggingRequest.class);
		
		verify(mockAmazonClient).setObjectTagging(requestCaptor.capture());
		
		assertEquals(BUCKET_NAME, requestCaptor.getValue().getBucketName());
		assertEquals(OBJECT_KEY, requestCaptor.getValue().getKey());
		assertEquals(tags, requestCaptor.getValue().getTagging().getTagSet());
	}
	
	@Test
	public void testRestoreObject() {
		
		RestoreObjectRequest request = new RestoreObjectRequest(BUCKET_NAME, OBJECT_KEY);
		RestoreObjectResult expectedResult = new RestoreObjectResult();
		
		when(mockAmazonClient.restoreObjectV2(request)).thenReturn(expectedResult);
		
		// Call under test
		RestoreObjectResult result = client.restoreObject(request);
		
		assertEquals(expectedResult, result);
		
		verify(mockAmazonClient).restoreObjectV2(request);
		
	}
	
	@Test
	public void testUploadPart() {
		
		UploadPartRequest request = new UploadPartRequest()
			.withBucketName(BUCKET_NAME)
			.withKey(OBJECT_KEY);
		
		UploadPartResult expected = new UploadPartResult();
		
		when(mockAmazonClient.uploadPart(any())).thenReturn(expected);
		
		// Call under test
		UploadPartResult result = client.uploadPart(request);
		
		assertEquals(expected, result);
		
		verify(mockAmazonClient).uploadPart(request);
	}

}
