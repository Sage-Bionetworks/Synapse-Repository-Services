package org.sagebionetworks.repo.manager.storagelocation.objectreaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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
public class GCObjectReaderTest {

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
	public void testVerifyBucketAccessWithNoAccessException() {
		StorageException ex = new StorageException(403, "Some exception");
		when(mockGCClient.bucketExists(BUCKET_NAME)).thenThrow(ex);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.verifyBucketAccess(BUCKET_NAME);
		});

		assertEquals("Synapse does not have access to the Google Cloud bucket " + BUCKET_NAME, e.getMessage());
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
	public void testOpenStreamWithNoObject() {

		when(mockGCClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenReturn(null);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Did not find Google Cloud object at key " + OBJECT_KEY + " from bucket " + BUCKET_NAME, e.getMessage());

		verify(mockGCClient).getObject(BUCKET_NAME, OBJECT_KEY);
		verifyZeroInteractions(mockBlob);
	}

	@Test
	public void testOpenStreamWithStorageException() {

		StorageException ex = new StorageException(403, "Some exception");

		when(mockGCClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenThrow(ex);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Could not read object at key " + OBJECT_KEY + " from bucket " + BUCKET_NAME, e.getMessage());

		verify(mockGCClient).getObject(BUCKET_NAME, OBJECT_KEY);
		verifyZeroInteractions(mockBlob);
	}
	
	@Test
	public void testOpenStreamWithStorageExceptionOnReader() {

		when(mockGCClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenReturn(mockBlob);

		StorageException ex = new StorageException(403, "Some exception");

		when(mockBlob.reader()).thenThrow(ex);
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Could not read object at key " + OBJECT_KEY + " from bucket " + BUCKET_NAME, e.getMessage());

		verify(mockGCClient).getObject(BUCKET_NAME, OBJECT_KEY);
		verify(mockBlob).reader();
	}

}
