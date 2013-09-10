package org.sagebionetworks.client.fileuploader;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;

public interface UploadFuturesFactory {

	Future<Entity> createChildFileEntityFuture(File file, String mimeType,
			ExecutorService threadPool, Synapse synapseClient,
			String targetEntityId, StatusCallback statusCallback);

	Future<Entity> createNewVersionFileEntityFuture(File file, String mimeType,
			ExecutorService uploadPool, Synapse synapseClient,
			FileEntity targetEntity, StatusCallback statusCallback);
}
