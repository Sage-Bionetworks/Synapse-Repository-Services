package org.sagebionetworks.sample.sts;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.file.S3FileHandle;

import java.io.IOException;
import java.util.List;

public class MigrateS3Bucket {
	private final DigestUtils md5DigestUtils;
	private final AmazonS3 s3Client;
	private final String s3Bucket;
	private final String s3Folder;
	private final long storageLocationId;
	private final SynapseClient synapseClient;
	private final String synapseFolderId;

	public MigrateS3Bucket(AmazonS3 s3Client, SynapseClient synapseClient, String s3Bucket, String s3Folder,
			String synapseFolderId, long storageLocationId) {
		// Init MD5 digest utils.
		this.md5DigestUtils = new DigestUtils(DigestUtils.getMd5Digest());

		this.s3Client = s3Client;
		this.synapseClient = synapseClient;

		// This is the S3 bucket and folder that we migrate from. If folder is null, we import all files in the S3
		// bucket.
		this.s3Bucket = s3Bucket;
		this.s3Folder = s3Folder;

		// This is the entity ID of the folder in Synapse to which we migrate the S3 files. This folder must have an
		// external S3 storage location with STS-enabled, as described in
		// https://docs.synapse.org/articles/sts_storage_locations.html
		this.synapseFolderId = synapseFolderId;

		// This is the storage location ID that is set on the Synapse folder. Note that you must be the owner of the
		// storage location in order to import existing S3 files into Synapse this way.
		this.storageLocationId = storageLocationId;
	}

	/** Executes the migration. */
	public void execute() throws IOException, SynapseException {
		executeForSubfolder(s3Folder, synapseFolderId);
	}

	/**
	 * Helper method to recursively walk the S3 folder hierarchy and migrate files to the given Synapse folder.
	 * When this method makes a recursive call, it will update s3SubFolder to the next folder and it will create
	 * a Synapse folder in which to migrate S3 files.
	 *
	 * @param s3SubFolder
	 *         the S3 subfolder from which we migrate files; if this is null then we export from the S3 bucket root
	 * @param synapseSubFolderId
	 *         the Synapse folder to which we migrate the S3 files
	 */
	private void executeForSubfolder(String s3SubFolder, String synapseSubFolderId) throws IOException,
			SynapseException {
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(s3Bucket)
				.withPrefix(s3SubFolder).withDelimiter("/");
		ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
		boolean hasNext;
		do {
			// Migrate all files in the listing.
			List<S3ObjectSummary> objectSummaryList = objectListing.getObjectSummaries();
			if (objectSummaryList != null) {
				for (S3ObjectSummary objectSummary : objectSummaryList) {
					migrateOneFile(objectSummary.getBucketName(), objectSummary.getKey(), synapseSubFolderId);
				}
			}

			// Recursively call for all subfolders in the listing.
			List<String> childFolderList = objectListing.getCommonPrefixes();
			if (childFolderList != null) {
				for (String childFolder : childFolderList) {
					String relativeChildFolder = childFolder;
					if (s3SubFolder != null) {
						// Subfolder is an absolute path from the root of the S3 bucket. Extract relative subfolder.
						relativeChildFolder = childFolder.substring(s3SubFolder.length());
					}
					String[] relativeChildFolderTokens = relativeChildFolder.split("/");

					// Make Synapse folders.
					String previousParentId = synapseSubFolderId;
					for (String folderToken : relativeChildFolderTokens) {
						if (folderToken == null || folderToken.isEmpty()) {
							// This could happen if there are some leading or trailing slashes.
							continue;
						}

						Folder folder = new Folder();
						folder.setName(folderToken);
						folder.setParentId(previousParentId);
						folder = synapseClient.createEntity(folder);
						previousParentId = folder.getId();
					}

					// Recursively call for the subfolder.
					executeForSubfolder(childFolder, previousParentId);
				}
			}

			// Fetch next page, if it exists.
			hasNext = objectListing.isTruncated();
			if (hasNext) {
				objectListing = s3Client.listNextBatchOfObjects(objectListing);
			}
		} while (hasNext);
	}

	/**
	 * Helper method which migrates one S3 file in the specified bucket and key and migrates it to the specified
	 * Synapse folder.
	 *
	 * @param s3Bucket
	 *         the bucket that the S3 file lives in
	 * @param s3Key
	 *         the absolute path of the S3 file to migrate
	 * @param synapseFolderId
	 *         the Synapse folder that the S3 file should be migrated to
	 */
	private void migrateOneFile(String s3Bucket, String s3Key, String synapseFolderId) throws IOException,
			SynapseException {
		// If you don't specify a file name, Synapse might generate a rather user-unfriendly one for you. We should
		// extract the filename from the S3 key.
		String filename = s3Key;
		if (s3Key.contains("/")) {
			// Key is a path. Just get the leaf.
			filename = s3Key.substring(s3Key.lastIndexOf("/") + 1);
		}

		if (filename.equals("owner.txt")) {
			// owner.txt is the file you used to set up your external S3 storage location. You probably don't want to
			// migrate that file to Synapse.
			return;
		}

		// Synapse requires external file handles to provide the MD5 hash, for file validation. In this sample code, we
		// download the file from S3 to hash it. We can save a file download by pre-computing the MD5 before migrating
		// and storing that MD5 hash somewhere (such as in S3 metadata). If we do this, we can migrate the file to
		// Synapse with no file transfer.
		S3Object s3Object = s3Client.getObject(s3Bucket, s3Key);
		byte[] md5 = md5DigestUtils.digest(s3Object.getObjectContent());
		String md5HexEncoded = Hex.encodeHexString(md5);

		// Create the file handle. This tells Synapse that the file exists and where to find it.
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setFileName(filename);
		fileHandle.setBucketName(s3Bucket);
		fileHandle.setKey(s3Key);
		fileHandle.setContentMd5(md5HexEncoded);
		fileHandle.setStorageLocationId(storageLocationId);
		fileHandle = synapseClient.createExternalS3FileHandle(fileHandle);

		// Now create the file entity. This puts the file in the Synapse hierarchy, where you can access it through
		// Synapse, set permissions, and share with others.
		FileEntity fileEntity = new FileEntity();
		fileEntity.setName(filename);
		fileEntity.setDataFileHandleId(fileHandle.getId());
		fileEntity.setParentId(synapseFolderId);
		synapseClient.createEntity(fileEntity);
	}
}
