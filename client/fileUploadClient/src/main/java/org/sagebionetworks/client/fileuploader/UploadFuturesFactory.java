package org.sagebionetworks.client.fileuploader;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;

public interface UploadFuturesFactory {

	Future<Entity> createChildFileEntityFuture(File file, String mimeType,
			ExecutorService threadPool, SynapseClient synapseClient,
			String targetEntityId, StatusCallback statusCallback);

	Future<Entity> createNewVersionFileEntityFuture(File file, String mimeType,
			ExecutorService uploadPool, SynapseClient synapseClient,
			FileEntity targetEntity, StatusCallback statusCallback);
}
