package org.sagebionetworks.file.worker;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationAuthorizationStatus;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.S3FileHandle;

/**
 * A data access object used to support bulk file download.  This abstraction contains 
 * 
 * @author John
 *
 */
public interface FileHandleSupport {
	
	/**
	 * Given a mixed list of FileHandleAssociation determine if the user is authorized to download each file.
	 * @see #canDownloadFile(UserInfo, List, String, FileHandleAssociateType)
	 * @param user
	 * @param associations
	 * @return
	 */
	public List<FileHandleAssociationAuthorizationStatus> canDownLoadFile(UserInfo user, List<FileHandleAssociation> associations);

	/**
	 * Create a temporary file on the local machine.
	 * 
	 * @param prefix
	 * @param suffix
	 * @return
	 * @throws IOException 
	 */
	public File createTempFile(String prefix, String suffix) throws IOException;
	
	/**
	 * Create a ZipOutputStream using the given file.
	 * @param outFile
	 * @return
	 * @throws IOException 
	 */
	public ZipOutputStream createZipOutputStream(File outFile) throws IOException;
	
	
	/**
	 * Multi-part upload a local file to S3.  This is used by workers.
	 * 
	 * @param userInfo
	 * @param fileToUpload
	 * @param contentType
	 * @param listener
	 * @return
	 */
	S3FileHandle multipartUploadLocalFile(LocalFileUploadRequest request);
	
	/**
	 * Get the S3FileHandle for the given FileHandle.id.
	 * 
	 * @param fileHandleId
	 * @return
	 * @throws IllegalArgumentException If the requested FileHandle is not an S3FileHandle.
	 */
	S3FileHandle getS3FileHandle(String fileHandleId);
	
	/**
	 * Download the given FileHandle to the a local file.
	 * @param fileHandle
	 * @return
	 * @throws IOException 
	 */
	public File downloadToTempFile(S3FileHandle fileHandle) throws IOException;
	
	/**
	 * Add the given file to 
	 * @param zipOut
	 * @param toAdd
	 * @param entryName
	 * @throws IOException 
	 */
	public void addFileToZip(ZipOutputStream zipOut, File toAdd, String entryName) throws IOException;
	
	
}
