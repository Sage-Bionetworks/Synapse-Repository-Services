package org.sagebionetworks.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.io.InputStream;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.audit.dao.GzipCsvS3ObjectReader;
import org.sagebionetworks.audit.dao.GzipCsvS3ObjectWriter;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.csv.utils.ExampleObject;
import org.sagebionetworks.util.ContentDispositionUtils;

import com.amazonaws.services.s3.model.ObjectMetadata;

public class GzipCsvS3ObjectWriterReaderTest {

	private SynapseS3Client mockS3Client;
	private String bucketName;
	private Class<ExampleObject> objectClass;
	private String[] headers;
	private GzipCsvS3ObjectWriter<ExampleObject> writer;
	private GzipCsvS3ObjectReader<ExampleObject> reader;

	@Before
	public void setUp() {
		mockS3Client = Mockito.mock(SynapseS3Client.class);
		bucketName = "object.csv.dao.test";
		objectClass = ExampleObject.class;
		headers = new String[]{"aString", "aLong", "aBoolean", "aDouble", "anInteger", "aFloat", "someEnum"};
		
		writer = new GzipCsvS3ObjectWriter<ExampleObject>(mockS3Client, objectClass, headers);
		reader = new GzipCsvS3ObjectReader<ExampleObject>(mockS3Client, objectClass, headers);
	}

	/**
	 * Test write and read methods
	 * @throws Exception
	 */
	@Test
	public void testRoundTrip() throws Exception{
		// Build up some sample data
		List<ExampleObject> data = ExampleObject.buildExampleObjectList(12);
		String key = "akey";
		// call under test.
		writer.write(data, bucketName, key);
		// capture results
		ArgumentCaptor<String> bucketCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> keyCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<InputStream> inCapture = ArgumentCaptor.forClass(InputStream.class);
		ArgumentCaptor<ObjectMetadata> metaCapture = ArgumentCaptor.forClass(ObjectMetadata.class);
		verify(mockS3Client).putObject(bucketCapture.capture(), keyCapture.capture(), inCapture.capture(), metaCapture.capture());
		assertEquals(bucketName, bucketCapture.getValue());
		assertEquals(key, keyCapture.getValue());
		// Can we read the results?
		List<ExampleObject> results = reader.readFromStream(inCapture.getValue());
		assertEquals(data, results);
		assertEquals(ContentDispositionUtils.getContentDispositionValue(key), metaCapture.getValue().getContentDisposition());
		assertEquals("application/x-gzip", metaCapture.getValue().getContentType());
		assertEquals("gzip", metaCapture.getValue().getContentEncoding());
		assertTrue(metaCapture.getValue().getContentLength() > 1);
	}


}
