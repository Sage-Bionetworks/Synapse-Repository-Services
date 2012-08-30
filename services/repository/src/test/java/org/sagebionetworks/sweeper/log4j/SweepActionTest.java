package org.sagebionetworks.sweeper.log4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;

public class SweepActionTest {

	private AmazonS3 mockS3Client;
	private static File nonexistentFile;
	private static File realFile;

	@BeforeClass
	public static void setupClass() throws IOException {
		nonexistentFile = new File("");
		assertFalse(nonexistentFile.exists());

		realFile = new File("realFile");
		realFile.createNewFile();
		assert(realFile.exists());
		SweepAction.setOnEC2(false);
	}

	@AfterClass
	public static void teardownClass() {
		realFile.delete();
		assertFalse(realFile.exists());
	}

	@Before
	public void setup() {
		mockS3Client = mock(AmazonS3.class);
	}

	@Test(expected=NullPointerException.class)
	public void testConstructorNullFile() {
		new SweepAction(null, null, null, false);
	}

	@Test(expected=NullPointerException.class)
	public void testConstructorNullBucketName() {
		new SweepAction(nonexistentFile, null, null, false);
	}

	@Test(expected=NullPointerException.class)
	public void testConstructorNullClient() {
		new SweepAction(nonexistentFile, "bucketName", null, false);
	}

	@Test
	public void testConstructor() {
		String s3BucketName = "bucketName";
		SweepAction sweepAction = new SweepAction(nonexistentFile, s3BucketName, mockS3Client, false);
		assertEquals(nonexistentFile, sweepAction.getFile());
		assertEquals(s3BucketName, sweepAction.getS3BucketName());
		assertEquals(mockS3Client, sweepAction.getS3Client());
	}

	@Test
	public void testExecuteNonExistentFile() throws IOException {
		SweepAction sweepAction = new SweepAction(nonexistentFile, "", mockS3Client, false);
		assertFalse(sweepAction.execute());
	}

	@Test
	public void testExecuteAmazonException() throws IOException {
		SweepAction sweepAction = new SweepAction(realFile, "", mockS3Client, false);
		when(mockS3Client.putObject(anyString(), anyString(), (File)anyObject())).thenThrow(new AmazonClientException(""));
		assertFalse(sweepAction.execute());
	}

	@Test
	public void testExecuteNoDelete() throws IOException {
		File mockFile = mock(File.class);
		SweepAction sweepAction = new SweepAction(mockFile, "", mockS3Client, false);

		when(mockFile.exists()).thenReturn(true);
		sweepAction.execute();
		verify(mockFile, times(0)).delete();
	}

	@Test
	public void testExecuteDelete() throws IOException {
		File mockFile = mock(File.class);
		SweepAction sweepAction = new SweepAction(mockFile, "", mockS3Client, true);

		when(mockFile.exists()).thenReturn(true);
		boolean execute = sweepAction.execute();
		assertTrue(execute);

		verify(mockFile, times(1)).delete();
	}
}
