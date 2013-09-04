package org.sagebionetworks.client.fileuploader;

import java.io.File;
import java.io.IOException;

import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public class UploaderUtils {

	public static FileEntity createChildFileEntity(final File file,
			final String mimeType, final Synapse synapseClient, final String targetEntityId,
			StatusCallback statusCallback) throws SynapseException, IOException {

		// create filehandle via multipart upload (blocking)
		statusCallback.setStatus(UploadStatus.UPLOADING);
		S3FileHandle fileHandle = synapseClient.createFileHandle(file, mimeType);

		// upload child
		final FileEntity newEntity = new FileEntity();
		newEntity.setName(file.getName());
		newEntity.setDataFileHandleId(fileHandle.getId());
		// create child File entity under parent
		newEntity.setParentId(targetEntityId);
		return synapseClient.createEntity(newEntity);			
	}

	public static FileEntity createNewVersionFileEntity(File file,
			String mimeType, Synapse synapseClient, FileEntity targetEntity,
			StatusCallback statusCallback) throws SynapseException, IOException {

		// create filehandle via multipart upload (blocking)
		statusCallback.setStatus(UploadStatus.UPLOADING);
		S3FileHandle fileHandle = synapseClient.createFileHandle(file, mimeType);
		
		// create new version
		// TODO : NYI
		
		return null;
	}

}
