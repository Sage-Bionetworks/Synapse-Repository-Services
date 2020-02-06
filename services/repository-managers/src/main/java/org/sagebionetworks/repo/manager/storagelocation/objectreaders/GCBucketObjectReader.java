package org.sagebionetworks.repo.manager.storagelocation.objectreaders;

import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.function.Supplier;

import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.manager.storagelocation.BucketObjectReader;
import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.api.client.http.HttpStatusCodes;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.StorageException;

@Service
public class GCBucketObjectReader implements BucketObjectReader {
	
	private static Supplier<String> cannotAccessObjectMessageSupplier(Throwable e, String bucketName, String key) {
		return () -> "Could not access object at key " + key + " from bucket " + bucketName + ": " + e.getMessage();
	}

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
			throw handleStorageException(e, bucketName, () -> 
				"Synapse could not access the google cloud bucket " + bucketName + ": " + e.getMessage()
			);
		}
	}

	@Override
	public InputStream openStream(String bucketName, String key) {

		Blob blob = getObjectOrThrow(bucketName, key);
		
		ReadChannel readChannel = null;

		try {
			readChannel = blob.reader();
			return Channels.newInputStream(readChannel);
		} catch (Throwable e) {

			dispose(readChannel);

			if (e instanceof StorageException) {
				throw handleStorageException((StorageException) e, bucketName, cannotAccessObjectMessageSupplier(e, bucketName, key));
			}

			throw new IllegalArgumentException(cannotAccessObjectMessageSupplier(e, bucketName, key).get(), e);
		}

	}
	
	Blob getObjectOrThrow(String bucketName, String key) {
		try {
			Blob blob = googleCloudStorageClient.getObject(bucketName, key);
			if (blob == null) {				
				throw new IllegalArgumentException("Could not find google cloud object at key " + key + " from bucket " + bucketName);
			}
			return blob;
		} catch (StorageException e) {
			throw handleStorageException(e, bucketName, cannotAccessObjectMessageSupplier(e, bucketName, key));
		}
	}

	private IllegalArgumentException handleStorageException(StorageException e, String bucketName, Supplier<String> unhandledExceptionMessageProvider) {
		// From https://cloud.google.com/storage/docs/json_api/v1/status-codes
		if (HttpStatusCodes.STATUS_CODE_FORBIDDEN == e.getCode()) {
			throw new IllegalArgumentException("Synapse does not have the correct access rights for the google cloud bucket " + bucketName, e);
		}
		throw new IllegalArgumentException(unhandledExceptionMessageProvider.get(), e);
	}

	private void dispose(ReadChannel readChannel) {
		if (readChannel == null) {
			return;
		}
		readChannel.close();
	}
}
