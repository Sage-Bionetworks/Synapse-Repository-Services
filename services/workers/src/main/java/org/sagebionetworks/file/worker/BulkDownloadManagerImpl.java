package org.sagebionetworks.file.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationAuthorizationStatus;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;

public class BulkDownloadManagerImpl implements BulkDownloadManager{
	
	private static final String ONLY_S3_FILE_HANDLES_CAN_BE_DOWNLOADED = "Only S3FileHandles can be downloaded.";
	
	@Autowired
	FileHandleDao fileHandleDao;
	@Autowired
	AmazonS3 s3client;
	@Autowired
	FileHandleAuthorizationManager fileHandleAuthorizationManager;
	@Autowired
	FileHandleManager fileHandleManager;

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.file.worker.BulkDownloadDao#canDownLoadFile(org.sagebionetworks.repo.model.UserInfo, java.util.List)
	 */
	@Override
	public List<FileHandleAssociationAuthorizationStatus> canDownLoadFile(
			UserInfo user, List<FileHandleAssociation> associations) {
		return fileHandleAuthorizationManager.canDownLoadFile(user, associations);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.file.worker.BulkDownloadDao#createTempFile(java.lang.String, java.lang.String)
	 */
	@Override
	public File createTempFile(String prefix, String suffix) throws IOException {
		return File.createTempFile(prefix, suffix);
	}

	@Override
	public ZipOutputStream createZipOutputStream(File outFile) throws IOException {
		return new ZipOutputStream(new FileOutputStream(outFile));
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.file.worker.BulkDownloadDao#multipartUploadLocalFile(org.sagebionetworks.repo.model.UserInfo, java.io.File, java.lang.String, com.amazonaws.event.ProgressListener)
	 */
	@Override
	public S3FileHandle multipartUploadLocalFile(UserInfo userInfo,
			File fileToUpload, String contentType, ProgressListener listener) {
		return fileHandleManager.multipartUploadLocalFile(userInfo, fileToUpload, contentType, listener);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.file.worker.BulkDownloadDao#getS3FileHandle(java.lang.String)
	 */
	@Override
	public S3FileHandle getS3FileHandle(String fileHandleId) {
		FileHandle handle = fileHandleDao.get(fileHandleId);
		if (!(handle instanceof S3FileHandle)) {
			throw new IllegalArgumentException(
					ONLY_S3_FILE_HANDLES_CAN_BE_DOWNLOADED);
		}
		return (S3FileHandle) handle;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.file.worker.BulkDownloadDao#downloadToTempFile(org.sagebionetworks.repo.model.file.S3FileHandle)
	 */
	@Override
	public File downloadToTempFile(S3FileHandle fileHandle) throws IOException {
		File tempFile = File.createTempFile("FileHandle"+fileHandle.getId(), ".tmp");
		// download this file to the local machine
		s3client.getObject(new GetObjectRequest(fileHandle.getBucketName(),
				fileHandle.getKey()), tempFile);
		return tempFile;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.file.worker.BulkDownloadDao#addFileToZip(java.util.zip.ZipOutputStream, java.io.File, java.lang.String)
	 */
	@Override
	public void addFileToZip(ZipOutputStream zipOut, File toAdd, String zipEntryName) throws IOException {
		InputStream in = new FileInputStream(toAdd);
		try{
			ZipEntry entry = new ZipEntry(zipEntryName);
			zipOut.putNextEntry(entry);
			// Write the file the zip
			IOUtils.copy(in, zipOut);
			zipOut.closeEntry();
		}finally{
			IOUtils.closeQuietly(in);
		}
	}

}
