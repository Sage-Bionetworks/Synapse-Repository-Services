package org.sagebionetworks.client.fileuploader;

import java.io.File;
import java.io.IOException;

import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.file.FileHandle;

public class UploaderUtils {

	public static FileEntity createChildFileEntity(final File file,
			final String mimeType, final SynapseClient synapseClient, final String targetEntityId,
			StatusCallback statusCallback) throws SynapseException, IOException {

		// create filehandle via multipart upload (blocking)
		statusCallback.setStatus(UploadStatus.UPLOADING);
		FileHandle fileHandle = synapseClient.createFileHandle(file, mimeType, targetEntityId);

		// upload child under target entity (parent)
		final FileEntity newEntity = new FileEntity();
		newEntity.setParentId(targetEntityId);
		newEntity.setName(file.getName());
		newEntity.setDataFileHandleId(fileHandle.getId());
		return synapseClient.createEntity(newEntity);			
	}

	public static FileEntity createNewVersionFileEntity(File file,
			String mimeType, SynapseClient synapseClient, FileEntity targetEntity,
			StatusCallback statusCallback) throws SynapseException, IOException {

		// create filehandle via multipart upload (blocking)
		statusCallback.setStatus(UploadStatus.UPLOADING);
		FileHandle fileHandle = synapseClient.createFileHandle(file, mimeType, targetEntity.getParentId());

		// update entity and create new version
		targetEntity.setDataFileHandleId(fileHandle.getId());
		targetEntity.setName(file.getName());
		EntityBundleCreate ebc = new EntityBundleCreate();		
		ebc.setEntity(targetEntity);
		EntityBundle eb = synapseClient.updateEntityBundle(targetEntity.getId(), ebc);
		
		return (FileEntity) eb.getEntity();
	}

}
