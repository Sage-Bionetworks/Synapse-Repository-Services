package org.sagebionetworks.file.worker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationAuthorizationStatus;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.FileDownloadCode;
import org.sagebionetworks.repo.model.file.FileDownloadStatus;
import org.sagebionetworks.repo.model.file.FileDownloadSummary;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.sqs.model.Message;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * This worker creates a zip file containing requested files.
 * 
 * @author John
 * 
 */
public class BulkFileDownloadWorker implements MessageDrivenRunner {

	private static final String APPLICATION_ZIP = "application/zip";

	private static final int FILE_HANDLE_ID_MODULO_DIVISOR = 1000;

	private static final String FILE_EXCEEDS_THE_MAXIMUM_SIZE_LIMIT = "File exceeds the maximum size limit.";

	private static final String ONLY_S3_FILE_HANDLES_CAN_BE_DOWNLOADED = "Only S3FileHandles can be downloaded.";

	private static final String RESULT_FILE_HAS_REACHED_THE_MAXIMUM_SIZE = "Result file has reached the maximum size";

	private static final String FILE_ALREADY_ADDED = "File already added.";

	static private Logger log = LogManager
			.getLogger(BulkFileDownloadWorker.class);

	public static final long MAX_TOTAL_FILE_SIZE_BYTES = 1024 * 1024 * 1024; // 1 GB.

	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private UserManager userManger;
	@Autowired
	FileHandleAuthorizationManager fileHandleAuthorizationManager;
	@Autowired
	FileHandleDao fileHandleDao;
	@Autowired
	AmazonS3Client s3client;
	@Autowired
	FileResourceProvider fileResourceProvider;
	@Autowired
	private FileHandleManager fileHandleManager;

	@Override
	public void run(ProgressCallback<Message> progressCallback, Message message)
			throws RecoverableMessageException, Exception {

		AsynchronousJobStatus status = MessageUtils.readMessageBody(message,
				AsynchronousJobStatus.class);
		try {

			if (!(status.getRequestBody() instanceof BulkFileDownloadRequest)) {
				throw new IllegalArgumentException("Unexpected request body: "
						+ status.getRequestBody());
			}
			BulkFileDownloadRequest request = (BulkFileDownloadRequest) status
					.getRequestBody();
			// build the zip from the results
			BulkFileDownloadResponse response = buildZip(progressCallback,
					message, status, request);
			asynchJobStatusManager.setComplete(status.getJobId(), response);
		} catch (Throwable e) {
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
		}

	}

	/**
	 * Build the result zip for each authorized file.
	 * 
	 * @param progressCallback
	 * @param message
	 * @param authResults
	 */
	public BulkFileDownloadResponse buildZip(
			final ProgressCallback<Message> progressCallback,
			final Message message, AsynchronousJobStatus status,
			BulkFileDownloadRequest request) {
		File tempResultFile = fileResourceProvider.createTempFile("Job"
				+ status.getJobId(), "zip");
		ZipOutputStream zipOut = fileResourceProvider
				.createZipOutputStream(tempResultFile);
		try {
			UserInfo user = userManger.getUserInfo(status.getStartedByUserId());
			/*
			 * The first step is to determine if the user is authorized to
			 * download each requested file. The authorization check is
			 * normalized around the associated object.
			 */
			List<FileHandleAssociationAuthorizationStatus> authResults = fileHandleAuthorizationManager
					.canDownLoadFile(user, request.getRequestedFiles());
			// Build the zip
			List<FileDownloadSummary> results = buildZip(progressCallback,
					message, authResults, tempResultFile, zipOut, status);
			// upload the result file to S3
			S3FileHandle resultHandle = fileHandleManager
					.multipartUploadLocalFile(user, tempResultFile,
							APPLICATION_ZIP, new ProgressListener() {
								@Override
								public void progressChanged(
										ProgressEvent progressEvent) {
									progressCallback.progressMade(message);
								}
							});
			BulkFileDownloadResponse response = new BulkFileDownloadResponse();
			response.setFileSummary(results);
			response.setResultZipFileHandleId(resultHandle.getId());
			return response;
		} finally {
			IOUtils.closeQuietly(zipOut);
			tempResultFile.delete();
		}
	}

	/**
	 * 
	 * @param progressCallback
	 * @param message
	 * @param authResults
	 * @param tempResultFile
	 * @param zipOut
	 */
	public List<FileDownloadSummary> buildZip(
			ProgressCallback<Message> progressCallback, Message message,
			List<FileHandleAssociationAuthorizationStatus> authResults,
			File tempResultFile, ZipOutputStream zipOut,
			AsynchronousJobStatus status) {
		long currentProgress = 0L;
		long totalProgress = (long) authResults.size();
		// This will be the final summary of results..
		List<FileDownloadSummary> fileSummaries = Lists.newLinkedList();
		// Track the files added to the zip.
		Set<String> fileIdsInZip = Sets.newHashSet();
		// Track the total size of the zip file
		long zipFileSize = 0L;
		for (FileHandleAssociationAuthorizationStatus fhas : authResults) {
			String fileHandleId = fhas.getAssociation().getAssociateObjectId();
			// Make progress between each file
			progressCallback.progressMade(message);
			// update the job progress
			asynchJobStatusManager.updateJobProgress(status.getJobId(),
					currentProgress, totalProgress, "Processing FileHandleId :"
							+ fhas.getAssociation().getFileHandleId());
			FileDownloadSummary summary = new FileDownloadSummary();
			summary.setFileHandleId(fileHandleId);
			fileSummaries.add(summary);
			try {
				writeOneFileToZip(zipOut, zipFileSize, fhas, fileIdsInZip);
				// download this file from S3
				fileIdsInZip.add(fileHandleId);
				summary.setStatus(FileDownloadStatus.SUCCESS);
				zipFileSize = tempResultFile.length();

			} catch (BulkFileException e) {
				// known error conditions.
				summary.setStatus(FileDownloadStatus.FAILURE);
				summary.setFailureMessage(e.getMessage());
				summary.setFailureCode(e.getFailureCode());
			} catch (NotFoundException e) {
				// file did not exist
				summary.setStatus(FileDownloadStatus.FAILURE);
				summary.setFailureMessage(e.getMessage());
				summary.setFailureCode(FileDownloadCode.NOT_FOUND);
			} catch (Exception e) {
				// all unknown errors.
				summary.setStatus(FileDownloadStatus.FAILURE);
				summary.setFailureMessage(e.getMessage());
				summary.setFailureCode(FileDownloadCode.UNKNOWN);
				log.error("Failed on: " + fhas.getAssociation(), e);
			}
			totalProgress++;
		}
		return fileSummaries;
	}

	/**
	 * Write a single file to the given zip stream.
	 * 
	 * @param zipOut
	 * @param zipFileSize
	 * @param fhas
	 * @param fileIdsInZip
	 * @throws IOException
	 */
	public void writeOneFileToZip(ZipOutputStream zipOut, long zipFileSize,
			FileHandleAssociationAuthorizationStatus fhas,
			Set<String> fileIdsInZip) throws IOException {
		// Is the user authorized to download this file?
		if (!fhas.getStatus().getAuthorized()) {
			throw new BulkFileException(fhas.getStatus().getReason(),
					FileDownloadCode.UNAUTHORIZED);
		}
		// Each file handle should only be added once
		if (fileIdsInZip.contains(fileIdsInZip)) {
			throw new BulkFileException(FILE_ALREADY_ADDED,
					FileDownloadCode.DUPLICATE);
		}
		if (zipFileSize > MAX_TOTAL_FILE_SIZE_BYTES) {
			throw new BulkFileException(
					RESULT_FILE_HAS_REACHED_THE_MAXIMUM_SIZE,
					FileDownloadCode.EXCEEDS_SIZE_LIMIT);
		}
		FileHandle handle = fileHandleDao.get(fhas.getAssociation()
				.getAssociateObjectId());
		// only S3 files can be added
		if (!(handle instanceof S3FileHandle)) {
			throw new IllegalArgumentException(
					ONLY_S3_FILE_HANDLES_CAN_BE_DOWNLOADED);
		}
		S3FileHandle s3Handle = (S3FileHandle) handle;
		if (s3Handle.getContentSize() > MAX_TOTAL_FILE_SIZE_BYTES) {
			throw new BulkFileException(FILE_EXCEEDS_THE_MAXIMUM_SIZE_LIMIT,
					FileDownloadCode.EXCEEDS_SIZE_LIMIT);
		}

		File tempFile = fileResourceProvider.createTempFile(
				"FileId" + s3Handle.getId(), "tmp");
		InputStream fileInput = null;
		try {
			// download this file to the local machine
			s3client.getObject(new GetObjectRequest(s3Handle.getBucketName(),
					s3Handle.getKey()), tempFile);
			// Create a reader used to copy the file to the zip
			fileInput = fileResourceProvider.createInputStream(tempFile);
			String zipEntryName = createZipEntryName(s3Handle.getFileName(),
					Long.parseLong(s3Handle.getId()));
			ZipEntry entry = new ZipEntry(zipEntryName);
			zipOut.putNextEntry(entry);
			// Write the file the zip
			fileResourceProvider.copy(fileInput, zipOut);
			zipOut.closeEntry();
		} finally {
			IOUtils.closeQuietly(fileInput);
			tempFile.delete();
		}
	}

	/**
	 * Create a zip entry using: {fileHandleId modulo 1000}
	 * /{fileHandleId}/{fileName}
	 * 
	 * @param fileName
	 * @param fileHandleId
	 * @return
	 */
	public static String createZipEntryName(String fileName, long fileHandleId) {
		long fileHandleModulus = fileHandleId % FILE_HANDLE_ID_MODULO_DIVISOR;
		return String.format("%1s/%2s/%3s", fileHandleModulus, fileHandleId,
				fileName);
	}

}
