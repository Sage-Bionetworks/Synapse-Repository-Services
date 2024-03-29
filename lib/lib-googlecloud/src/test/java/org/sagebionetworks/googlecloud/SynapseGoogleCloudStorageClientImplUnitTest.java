package org.sagebionetworks.googlecloud;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.RestorableState;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

@ExtendWith(MockitoExtension.class)
public class SynapseGoogleCloudStorageClientImplUnitTest {
	
	@Mock
	private Storage mockStorage;

	@Mock
	private Blob mockBlob;

	@Mock
	private Blob mockBlob2;

	@Mock
	private Page<Blob> mockPage;

	@Mock
	private ReadChannel mockReadChannel;

	@Captor
	private ArgumentCaptor<Storage.ComposeRequest> composeRequestCaptor;


	private SynapseGoogleCloudStorageClientImpl client;

	private static final String BUCKET_NAME = "bucket-name";
	private static final String OBJECT_KEY = "gcp-object-key";
	private static final BlobId OBJECT_BLOB_ID = BlobId.of(BUCKET_NAME, OBJECT_KEY);
	private static final BlobInfo OBJECT_BLOB_INFO = BlobInfo.newBuilder(OBJECT_BLOB_ID).build();

	@BeforeEach
	public void before() {
		client = new SynapseGoogleCloudStorageClientImpl(mockStorage);
	}
	
	@Test
	public void getObject() {
		when(mockStorage.get(OBJECT_BLOB_ID)).thenReturn(mockBlob);
		Blob result = client.getObject(BUCKET_NAME, OBJECT_KEY);
		assertEquals(mockBlob, result);
		verify(mockStorage).get(OBJECT_BLOB_ID);
	}

	@Test
	public void putObject() {
		byte[] fileContents = "some file content".getBytes(StandardCharsets.UTF_8);
		InputStream inputStream = new ByteArrayInputStream(fileContents);
		// We use a stubbed class to verify that the contents are copied properly
		TestStubWriteChannel stubWriteChannel = new TestStubWriteChannel();
		when(mockStorage.writer(OBJECT_BLOB_INFO)).thenReturn(stubWriteChannel);
		client.putObject(BUCKET_NAME, OBJECT_KEY, inputStream);
		verify(mockStorage).writer(OBJECT_BLOB_INFO);
		// Byte array equality checks the object, we can just convert back to a string.
		assertArrayEquals(fileContents, stubWriteChannel.outputStream.toByteArray());
	}

	@Test
	public void deleteObjectSuccess() {
		when(mockStorage.delete(OBJECT_BLOB_ID)).thenReturn(true);
		client.deleteObject(BUCKET_NAME, OBJECT_KEY);
		verify(mockStorage).delete(OBJECT_BLOB_ID);
	}

	@Test
	public void deleteObjectFailure() {
		when(mockStorage.delete(OBJECT_BLOB_ID)).thenReturn(false);
		assertThrows(StorageException.class, () -> client.deleteObject(BUCKET_NAME, OBJECT_KEY));
	}

	@Test
	public void createSignedGETUrl() {
		long expirationTime = 50L;
		HttpMethod method = HttpMethod.GET;
		client.createSignedUrl(BUCKET_NAME, OBJECT_KEY, expirationTime, method);

		verify(mockStorage).signUrl(eq(OBJECT_BLOB_INFO), eq(expirationTime),
				eq(TimeUnit.MILLISECONDS),
				any(Storage.SignUrlOption.class),
				any(Storage.SignUrlOption.class));
	}

	@Test
	public void createSignedPUTUrl() {
		// No way to inspect the different sign URL option
		long expirationTime = 50L;
		HttpMethod method = HttpMethod.PUT;
		client.createSignedUrl(BUCKET_NAME, OBJECT_KEY, expirationTime, method);

		verify(mockStorage).signUrl(eq(OBJECT_BLOB_INFO), eq(expirationTime),
				eq(TimeUnit.MILLISECONDS),
				any(Storage.SignUrlOption.class),
				any(Storage.SignUrlOption.class));
	}


	@Test
	public void composeObjects() {
		List<String> partKeys = Arrays.asList("key 1", "key 2", "key 3");

		client.composeObjects(BUCKET_NAME, OBJECT_KEY, partKeys);
		verify(mockStorage).compose(composeRequestCaptor.capture());

		assertEquals(OBJECT_BLOB_INFO, composeRequestCaptor.getValue().getTarget());
		assertEquals(partKeys.size(), composeRequestCaptor.getValue().getSourceBlobs().size());
		assertEquals(partKeys.get(0), composeRequestCaptor.getValue().getSourceBlobs().get(0).getName());
		assertEquals(partKeys.get(1), composeRequestCaptor.getValue().getSourceBlobs().get(1).getName());
		assertEquals(partKeys.get(2), composeRequestCaptor.getValue().getSourceBlobs().get(2).getName());
	}

	@Test
	public void composeTooManyObjects() {
		List<String> partKeys = new ArrayList<>();
		for (int i = 0; i < 33; i++) { // Max objects is 32
			partKeys.add("part "+ i);
		}
		assertThrows(IllegalArgumentException.class, () -> client.composeObjects(BUCKET_NAME, OBJECT_KEY, partKeys));
	}

	@Test
	public void testRename() {
		String newKey = "a new name";

		// Call under test
		client.rename(BUCKET_NAME, OBJECT_KEY, newKey);

		verify(mockStorage).copy(any(Storage.CopyRequest.class));
		verify(mockStorage).delete(OBJECT_BLOB_ID);
	}

	@Test
	public void testGetObjects() {
		Iterable<Blob> expected = Arrays.asList(mockBlob, mockBlob2);
		when(mockStorage.list(eq(BUCKET_NAME), any(Storage.BlobListOption.class), any(Storage.BlobListOption.class))).thenReturn(mockPage);
		when(mockPage.iterateAll()).thenReturn(expected);

		// Call under test
		Iterable<Blob> actual = client.getObjects(BUCKET_NAME, OBJECT_KEY);

		verify(mockStorage).list(eq(BUCKET_NAME), any(Storage.BlobListOption.class), any(Storage.BlobListOption.class));
		assertEquals(expected, actual);
	}

	@Test
	public void testDoesObjectExistWithExisting() {
		when(mockStorage.get(OBJECT_BLOB_ID)).thenReturn(mockBlob);

		// Call under test
		Boolean result = client.doesObjectExist(BUCKET_NAME, OBJECT_KEY);

		assertEquals(true, result);

		verify(mockStorage).get(OBJECT_BLOB_ID);
	}

	@Test
	public void testDoesObjectExistNull() {
		when(mockStorage.get(OBJECT_BLOB_ID)).thenReturn(null);

		// Call under test
		Boolean result = client.doesObjectExist(BUCKET_NAME, OBJECT_KEY);

		assertEquals(false, result);

		verify(mockStorage).get(OBJECT_BLOB_ID);
	}

	/**
	 * This class merely needs to implement the methods in {@link java.nio.channels.WritableByteChannel}
	 */
	private static class TestStubWriteChannel implements WriteChannel {
		private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		private WritableByteChannel outputChannel = Channels.newChannel(outputStream);

		@Override
		public void setChunkSize(int chunkSize) {
			// Not needed
		}

		@Override
		public RestorableState<WriteChannel> capture() {
			return null; // Not needed
		}

		@Override
		public int write(ByteBuffer src) throws IOException {
			return outputChannel.write(src);
		}

		@Override
		public boolean isOpen() {
			return true;
		}

		@Override
		public void close() {
		}
	}
}
