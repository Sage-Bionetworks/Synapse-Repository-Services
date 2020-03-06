package org.sagebionetworks.sample.sts;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.file.FileHandle;

import java.io.File;
import java.io.IOException;

public class MigrateSynapseProject {
	private final String destinationFolderId;
	private final long destinationStorageLocationId;
	private final String sourceEntityId;
	private final SynapseClient synapseClient;
	private final File tmpDir;

	public MigrateSynapseProject(SynapseClient synapseClient, String sourceEntityId, String destinationFolderId,
			long destinationStorageLocationId) {
		this.synapseClient = synapseClient;

		// This is the Synapse entity from which you want to migrate data. This can be a project or a folder.
		this.sourceEntityId = sourceEntityId;

		// This is the entity ID of the folder in Synapse to which we migrate the S3 files. This folder must have an
		// external S3 storage location with STS-enabled, as described in
		// https://docs.synapse.org/articles/sts_storage_locations.html
		this.destinationFolderId = destinationFolderId;

		// This is the storage location ID that is set on the Synapse folder.
		this.destinationStorageLocationId = destinationStorageLocationId;

		// Temporary directory to store downloaded files.
		tmpDir = Files.createTempDir();
	}

	/** Executes the migration. */
	public void execute() throws IOException, SynapseException {
		executeForSubfolder(sourceEntityId, destinationFolderId);

		tmpDir.delete();
	}

	/**
	 * Helper method to recursively walk the source folder hierarchy and migrate files to the given destination folder.
	 * When this method makes a recursive call, it will update sourceSubFolderId to the next folder and it will create
	 * a destination folder in which to migrate files.
	 *
	 * @param sourceSubFolderId
	 *         the subfolder from which we migrate files
	 * @param destinationSubFolderId
	 *         the folder to which we migrate the files
	 */
	private void executeForSubfolder(String sourceSubFolderId, String destinationSubFolderId) throws IOException,
			SynapseException {
		EntityChildrenRequest entityChildrenRequest = new EntityChildrenRequest();
		entityChildrenRequest.setParentId(sourceSubFolderId);
		entityChildrenRequest.setIncludeTypes(ImmutableList.of(EntityType.file, EntityType.folder));
		EntityChildrenResponse entityChildrenResponse =  synapseClient.getEntityChildren(entityChildrenRequest);

		boolean hasNext;
		do {
			// Migrate all files and folders.
			for (EntityHeader childEntityHeader : entityChildrenResponse.getPage()) {
				if (Folder.class.getName().equals(childEntityHeader.getType())) {
					// Make the corresponding folder in the destination.
					Folder newFolder = new Folder();
					newFolder.setName(childEntityHeader.getName());
					newFolder.setParentId(destinationSubFolderId);
					newFolder = synapseClient.createEntity(newFolder);

					// Recursively call for the subfolder.
					executeForSubfolder(childEntityHeader.getId(), newFolder.getId());
				} else if (FileEntity.class.getName().equals(childEntityHeader.getType())) {
					migrateOneFile(childEntityHeader.getId(), destinationSubFolderId);
				}
			}

			// Fetch next page, if it exists.
			hasNext = entityChildrenResponse.getNextPageToken() != null;
			if (hasNext) {
				entityChildrenRequest.setNextPageToken(entityChildrenResponse.getNextPageToken());
				entityChildrenResponse = synapseClient.getEntityChildren(entityChildrenRequest);
			}
		} while (hasNext);
	}

	/**
	 * Helper method which migrates the specified file to the specified Synapse folder.
	 *
	 * @param sourceFileEntityId
	 *         the file to be migrated
	 * @param destinationSubFolderId
	 *         the folder that the file should be migrated to
	 */
	private void migrateOneFile(String sourceFileEntityId, String destinationSubFolderId) throws IOException,
			SynapseException {
		// Get the file entity from the ID.
		FileEntity sourceFileEntity = synapseClient.getEntity(sourceFileEntityId, FileEntity.class);

		// Download the file. Note that this can be optimized using the BulkFileDownload API.
		File tmpFile = new File(tmpDir, sourceFileEntity.getName());
		synapseClient.downloadFromFileHandleTemporaryUrl(sourceFileEntity.getDataFileHandleId(), tmpFile);

		// Upload the file to the new storage location.
		FileHandle fileHandle = synapseClient.multipartUpload(tmpFile, destinationStorageLocationId, null,
				null);

		// Now create the file entity in the destination.
		FileEntity fileEntity = new FileEntity();
		fileEntity.setName(sourceFileEntity.getName());
		fileEntity.setDataFileHandleId(fileHandle.getId());
		fileEntity.setParentId(destinationSubFolderId);
		synapseClient.createEntity(fileEntity);

		// Clean up temp file.
		tmpFile.delete();
	}
}
