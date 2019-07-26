package org.sagebionetworks.googlecloud;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.api.client.http.HttpStatusCodes;
import com.google.api.gax.paging.Page;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

public class SynapseGoogleCloudStorageClientImpl implements SynapseGoogleCloudStorageClient {

	private static final int MAX_OBJECTS_IN_COMPOSE = 32;

	private Storage storage;

	public SynapseGoogleCloudStorageClientImpl(Storage storageClient) throws StorageException {
		this.storage = storageClient;
	}

	@Override
	public Blob getObject(String bucket, String key) throws StorageException {
		return storage.get(BlobId.of(bucket, key));
	}

	@Override
	public Blob putObject(String bucket, String key, InputStream inputStream) throws StorageException {
		putObject(BlobInfo.newBuilder(BlobId.of(bucket, key)).build(), inputStream);
		return this.getObject(bucket, key);
	}

	@Override
	public Blob putObject(String bucket, String key, File file) throws FileNotFoundException, StorageException {
		putObject(BlobInfo.newBuilder(BlobId.of(bucket, key)).build(), new FileInputStream(file));
		return this.getObject(bucket, key);
	}

	private void putObject(BlobInfo blobInfo, InputStream inputStream) {
		try (WriteChannel writer = storage.writer(blobInfo)) {
			ByteStreams.copy(inputStream, Channels.newOutputStream(writer));
		} catch (IOException e) {
			throw new RuntimeException("Error writing input stream to Google Cloud", e);
		}
	}

	@Override
	public void deleteObject(String bucket, String key) throws StorageException {
		BlobId blobId = BlobId.of(bucket, key);
		if (!storage.delete(blobId)) {
			throw new StorageException(HttpStatusCodes.STATUS_CODE_SERVICE_UNAVAILABLE,
					"Error encountered when deleting the object in Google Cloud Storage. The item has not been deleted.");
		}
	}

	@Override
	public URL createSignedUrl(String bucket, String key, long expirationInMilliseconds, HttpMethod requestMethod) throws StorageException {
		return storage.signUrl(BlobInfo.newBuilder(BlobId.of(bucket, key)).build(),
				expirationInMilliseconds, TimeUnit.MILLISECONDS, Storage.SignUrlOption.withV4Signature(),
				Storage.SignUrlOption.httpMethod(requestMethod));
	}

	@Override
	public Blob composeObjects(String bucket, String newKey, List<String> partKeys) throws StorageException {
		if (partKeys.size() > MAX_OBJECTS_IN_COMPOSE)
			throw new IllegalArgumentException("Cannot compose more than " + MAX_OBJECTS_IN_COMPOSE + " objects in one request");
		BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, newKey)).build();
		return storage.compose(
				Storage.ComposeRequest.newBuilder()
						.setTarget(blobInfo)
						.addSource(partKeys)
						.build()
		);
	}

	@Override
	public void rename(String bucket, String oldKey, String newKey) throws StorageException {
		storage.copy(
				Storage.CopyRequest.newBuilder()
						.setSource(BlobId.of(bucket,oldKey))
						.setTarget(BlobId.of(bucket,newKey))
						.build());
		storage.delete(BlobId.of(bucket,oldKey));
	}

	@Override
	public List<Blob> getObjects(String bucket, String keyPrefix) {
		Page<Blob> blobsPage = storage.list(bucket, Storage.BlobListOption.currentDirectory(), Storage.BlobListOption.prefix(keyPrefix));
		List<Blob> blobs = Lists.newLinkedList(blobsPage.iterateAll());
		while (blobsPage.hasNextPage()) {
			blobsPage = blobsPage.getNextPage();
			blobs.addAll(Lists.newLinkedList(blobsPage.iterateAll()));
		}
		return blobs;
	}

	@Override
	public Boolean bucketExists(String bucket) {
		return storage.get(bucket, Storage.BucketGetOption.fields()) != null;
	}

	@Override
	public BufferedReader getObjectContent(String bucket, String key) {
		return new BufferedReader(
				new InputStreamReader(
						Channels.newInputStream(this.getObject(bucket, key).reader())
				)
		);
	}
}
