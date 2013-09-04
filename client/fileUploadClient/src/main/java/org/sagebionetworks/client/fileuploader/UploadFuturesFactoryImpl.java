package org.sagebionetworks.client.fileuploader;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;

public class UploadFuturesFactoryImpl implements UploadFuturesFactory {

	static UploadFuturesFactoryImpl instance;
	
	static {
		instance = new UploadFuturesFactoryImpl();
	}
	
	private UploadFuturesFactoryImpl() {
	}

	public static UploadFuturesFactory getInstance() { 
		return instance;
	}
	
	@Override
	public Future<Entity> createChildFileEntityFuture(final File file,
			final String mimeType, final ExecutorService threadPool,
			final Synapse synapseClient, final String targetEntityId,
			final StatusCallback statusCallback) {
		return threadPool.submit(new Callable<Entity>() {
			@Override
			public FileEntity call() throws Exception {
				return UploaderUtils.createChildFileEntity(file, mimeType, synapseClient, targetEntityId, statusCallback);
			}
		}); 
	}

	@Override
	public Future<Entity> createNewVersionFileEntityFuture(final File file,
			final String mimeType, final ExecutorService threadPool, final Synapse synapseClient,
			final FileEntity targetEntity, final StatusCallback statusCallback) {
		return threadPool.submit(new Callable<Entity>() {
			@Override
			public FileEntity call() throws Exception {
				return UploaderUtils.createNewVersionFileEntity(file, mimeType, synapseClient, targetEntity, statusCallback);
			}
		}); 
	}

}
