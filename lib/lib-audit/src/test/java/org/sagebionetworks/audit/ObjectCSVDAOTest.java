package org.sagebionetworks.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.audit.dao.ObjectCSVDAO;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.csv.utils.ExampleObject;
import org.sagebionetworks.util.ContentDispositionUtils;

import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class ObjectCSVDAOTest {

	private SynapseS3Client mockS3Client;
	private int stackInstanceNumber;
	private String bucketName;
	private Class<ExampleObject> objectClass;
	private String[] headers;
	private ObjectCSVDAO<ExampleObject> dao;

	@Before
	public void setUp() {
		mockS3Client = Mockito.mock(SynapseS3Client.class);
		stackInstanceNumber = 1;
		bucketName = "object.csv.dao.test";
		objectClass = ExampleObject.class;
		headers = new String[]{"aString", "aLong", "aBoolean", "aDouble", "anInteger", "aFloat", "someEnum"};
		dao = new ObjectCSVDAO<ExampleObject>(mockS3Client, stackInstanceNumber, bucketName, objectClass, headers);
	}

	/**
	 * Test write and read methods
	 * @throws Exception
	 */
	@Test
	public void testRoundTrip() throws Exception{
		Long timestamp = System.currentTimeMillis();
		boolean rolling = false;

		// Build up some sample data
		List<ExampleObject> data = ExampleObject.buildExampleObjectList(12);
		// call under test.
		String key = dao.write(data, timestamp, rolling);
		// capture results
		ArgumentCaptor<String> bucketCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> keyCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<InputStream> inCapture = ArgumentCaptor.forClass(InputStream.class);
		ArgumentCaptor<ObjectMetadata> metaCapture = ArgumentCaptor.forClass(ObjectMetadata.class);
		verify(mockS3Client).putObject(bucketCapture.capture(), keyCapture.capture(), inCapture.capture(), metaCapture.capture());
		assertEquals(bucketName, bucketCapture.getValue());
		assertEquals(key, keyCapture.getValue());
		// Can we read the results?
		List<ExampleObject> results = dao.readFromStream(inCapture.getValue());
		assertEquals(data, results);
		assertEquals(ContentDispositionUtils.getContentDispositionValue(key), metaCapture.getValue().getContentDisposition());
		assertEquals("application/x-gzip", metaCapture.getValue().getContentType());
		assertEquals("gzip", metaCapture.getValue().getContentEncoding());
		assertTrue(metaCapture.getValue().getContentLength() > 1);
	}


	@Test
	public void testListBatchKeys() throws Exception {
		
		ObjectListing one = new ObjectListing();
		S3ObjectSummary sum = new S3ObjectSummary();
		sum.setKey("one");
		one.getObjectSummaries().add(sum);
		sum = new S3ObjectSummary();
		sum.setKey("two");
		one.getObjectSummaries().add(sum);
		one.setNextMarker("nextMarker");
		
		ObjectListing two = new ObjectListing();
		sum = new S3ObjectSummary();
		sum.setKey("three");
		two.getObjectSummaries().add(sum);
		sum = new S3ObjectSummary();
		sum.setKey("four");
		two.getObjectSummaries().add(sum);
		two.setMarker(null);
		
		when(mockS3Client.listObjects(any(ListObjectsRequest.class))).thenReturn(one, two);
		// Now iterate over all key and ensure all keys are found
		Set<String> foundKeys = dao.listAllKeys();
		// the two set should be equal
		assertEquals(4, foundKeys.size());
		assertTrue(foundKeys.contains("one"));
		assertTrue(foundKeys.contains("two"));
		assertTrue(foundKeys.contains("three"));
		assertTrue(foundKeys.contains("four"));
	}


}
