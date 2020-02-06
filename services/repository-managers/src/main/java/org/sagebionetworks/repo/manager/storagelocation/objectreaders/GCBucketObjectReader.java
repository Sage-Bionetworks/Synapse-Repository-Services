package org.sagebionetworks.repo.manager.storagelocation.objectreaders;

import java.io.InputStream;
import java.nio.channels.Channels;

import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.manager.storagelocation.BucketObjectReader;
import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.StorageException;

@Service
public class GCBucketObjectReader implements BucketObjectReader {

	@Autowired
	private SynapseGoogleCloudStorageClient googleCloudStorageClient;

	@Override
	public Class<? extends BucketOwnerStorageLocationSetting> getSupportedStorageLocationType() {
		return ExternalGoogleCloudStorageLocationSetting.class;
	}

	@Override
	public void verifyBucketAccess(String bucketName) {
		try {
			if (!googleCloudStorageClient.bucketExists(bucketName)) {
				throw new IllegalArgumentException("Did not find Google Cloud bucket " + bucketName);
			}
		} catch (StorageException e) {
			throw new IllegalArgumentException("Synapse does not have access to the Google Cloud bucket " + bucketName, e);
		}
	}

	@Override
	public InputStream openStream(String bucketName, String key) {

		ReadChannel readChannel = null;

		try {
			Blob blob = googleCloudStorageClient.getObject(bucketName, key);

			if (blob == null) {
				throw new IllegalArgumentException("Did not find Google Cloud object at key " + key + " from bucket " + bucketName);
			}

			readChannel = blob.reader();

			return Channels.newInputStream(readChannel);

		} catch (Throwable e) {

			dispose(readChannel);

			if (e instanceof IllegalArgumentException) {
				throw e;
			}

			throw new IllegalArgumentException("Could not read object at key " + key + " from bucket " + bucketName, e);
		}

	}
	
	private void dispose(ReadChannel readChannel) {
		if (readChannel == null) {
			return;
		}
		readChannel.close();
	}
}
