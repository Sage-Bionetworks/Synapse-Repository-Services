package org.sagebionetworks.repo.manager.storagelocation.objectreaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.StorageException;

@ExtendWith(MockitoExtension.class)
public class GCBucketObjectReaderTest {

	private static final String BUCKET_NAME = "somebucket";
	private static final String OBJECT_KEY = "someObjectKey";

	@Mock
	private SynapseGoogleCloudStorageClient mockGCClient;

	@InjectMocks
	private GCBucketObjectReader objectReader;

	@Mock
	private Blob mockBlob;

	@Mock
	private ReadChannel mockReadChannel;

	@Test
	public void testGetSupportedStorageLocationType() {
		assertEquals(ExternalGoogleCloudStorageLocationSetting.class, objectReader.getSupportedStorageLocationType());
	}

	@Test
	public void testVerifyBucketAccess() {
		when(mockGCClient.bucketExists(BUCKET_NAME)).thenReturn(true);

		// Call under test
		objectReader.verifyBucketAccess(BUCKET_NAME);

		verify(mockGCClient).bucketExists(BUCKET_NAME);
	}

	@Test
	public void testVerifyBucketAccessWithNonExistingBucket() {
		when(mockGCClient.bucketExists(BUCKET_NAME)).thenReturn(false);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.verifyBucketAccess(BUCKET_NAME);
		});

		assertEquals("Did not find Google Cloud bucket " + BUCKET_NAME, e.getMessage());
	}

	@Test
	public void testVerifyBucketAccessWith403StorageException() {
		int code = 403;
		String message = "Some message";
		
		StorageException ex = new StorageException(code, message);
		
		when(mockGCClient.bucketExists(BUCKET_NAME)).thenThrow(ex);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.verifyBucketAccess(BUCKET_NAME);
		});

		assertEquals("Synapse does not have the correct access rights for the google cloud bucket " + BUCKET_NAME, e.getMessage());
	}
	
	@Test
	public void testVerifyBucketAccessWithUnhandledStorageException() {
		int code = 0;
		String message = "Some message";
		
		StorageException ex = new StorageException(code, message);
		
		when(mockGCClient.bucketExists(BUCKET_NAME)).thenThrow(ex);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.verifyBucketAccess(BUCKET_NAME);
		});

		assertEquals("Synapse could not access the google cloud bucket " + BUCKET_NAME + ": " + message, e.getMessage());
	}

	@Test
	public void testOpenStream() {

		when(mockGCClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenReturn(mockBlob);
		when(mockBlob.reader()).thenReturn(mockReadChannel);

		// Call under test
		InputStream stream = objectReader.openStream(BUCKET_NAME, OBJECT_KEY);

		assertNotNull(stream);

		verify(mockGCClient).getObject(BUCKET_NAME, OBJECT_KEY);
		verify(mockBlob).reader();
	}
	
	@Test
	public void testOpenStreamWithUnhandledStorageExceptionOnReader() {
		
		int code = 0;
		String message = "Some message";
		
		StorageException ex = new StorageException(code, message);

		when(mockBlob.reader()).thenThrow(ex);
		when(mockGCClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenReturn(mockBlob);
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Could not access object at key " + OBJECT_KEY + " from bucket " + BUCKET_NAME + ": " + ex.getMessage(), e.getMessage());

		verify(mockGCClient).getObject(BUCKET_NAME, OBJECT_KEY);
		verify(mockBlob).reader();
	}
	
	@Test
	public void testOpenStreamWith403StorageExceptionOnReader() {
		
		int code = 403;
		String message = "Some message";
		
		StorageException ex = new StorageException(code, message);

		when(mockBlob.reader()).thenThrow(ex);
		when(mockGCClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenReturn(mockBlob);
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Synapse does not have the correct access rights for the google cloud bucket " + BUCKET_NAME, e.getMessage());

		verify(mockGCClient).getObject(BUCKET_NAME, OBJECT_KEY);
		verify(mockBlob).reader();
	}
	
	@Test
	public void testGetObjectOrThrow() {
		
		when(mockGCClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenReturn(mockBlob);
		
		// Call under test
		Blob blob = objectReader.getObjectOrThrow(BUCKET_NAME, OBJECT_KEY);
		
		assertEquals(mockBlob, blob);
	}
	
	@Test
	public void testGetObjectOrThrowWithNullBlob() {
		
		when(mockGCClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenReturn(null);
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.getObjectOrThrow(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Could not find google cloud object at key " + OBJECT_KEY + " from bucket " + BUCKET_NAME, e.getMessage());
	}
	
	@Test
	public void testGetObjectOrThrowWithUnhandledStorageException() {
		
		int code = 0;
		String message = "Some message";
		
		StorageException ex = new StorageException(code, message);
		
		when(mockGCClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenThrow(ex);
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.getObjectOrThrow(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Could not access object at key " + OBJECT_KEY + " from bucket " + BUCKET_NAME + ": " + message, e.getMessage());
	}
	
	@Test
	public void testGetObjectOrThrowWith403StorageException() {
		
		int code = 403;
		String message = "Some message";
		
		StorageException ex = new StorageException(code, message);
		
		when(mockGCClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenThrow(ex);
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.getObjectOrThrow(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Synapse does not have the correct access rights for the google cloud bucket " + BUCKET_NAME, e.getMessage());
	}

}
