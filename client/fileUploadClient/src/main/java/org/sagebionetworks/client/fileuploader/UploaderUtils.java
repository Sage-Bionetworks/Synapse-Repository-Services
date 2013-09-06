package org.sagebionetworks.client.fileuploader;

import java.io.File;
import java.io.IOException;

import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public class UploaderUtils {

	public static FileEntity createChildFileEntity(final File file,
			final String mimeType, final Synapse synapseClient, final String targetEntityId,
			StatusCallback statusCallback) throws SynapseException, IOException {

		// create filehandle via multipart upload (blocking)
		statusCallback.setStatus(UploadStatus.UPLOADING);
		S3FileHandle fileHandle = synapseClient.createFileHandle(file, mimeType);

		// upload child under target entity (parent)
		final FileEntity newEntity = new FileEntity();
		newEntity.setParentId(targetEntityId);
		newEntity.setName(file.getName());
		newEntity.setDataFileHandleId(fileHandle.getId());
		return synapseClient.createEntity(newEntity);			
	}

	public static FileEntity createNewVersionFileEntity(File file,
			String mimeType, Synapse synapseClient, FileEntity targetEntity,
			StatusCallback statusCallback) throws SynapseException, IOException {

		// create filehandle via multipart upload (blocking)
		statusCallback.setStatus(UploadStatus.UPLOADING);
		S3FileHandle fileHandle = synapseClient.createFileHandle(file, mimeType);
		
		// update entity and create new version
		targetEntity.setDataFileHandleId(fileHandle.getId());
		targetEntity.setName(file.getName());
		EntityBundleCreate ebc = new EntityBundleCreate();		
		ebc.setEntity(targetEntity);
		EntityBundle eb = synapseClient.updateEntityBundle(targetEntity.getId(), ebc);
		
		return (FileEntity) eb.getEntity();
	}

}
