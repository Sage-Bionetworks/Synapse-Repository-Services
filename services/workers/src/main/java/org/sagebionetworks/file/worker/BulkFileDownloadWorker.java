package org.sagebionetworks.file.worker;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationAuthorizationStatus;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.FileDownloadCode;
import org.sagebionetworks.repo.model.file.FileDownloadStatus;
import org.sagebionetworks.repo.model.file.FileDownloadSummary;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.sqs.model.Message;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * 
 * This worker contains all of the business logic for building a bulk download zip.
 * 
 * @author John
 * 
 */
public class BulkFileDownloadWorker implements MessageDrivenRunner {

	public static final String PROCESSING_FILE_HANDLE_ID = "Processing FileHandleId :";

	public static final String ZIP_ENTRY_TEMPLATE = "%d/%d/%s";

	public static final String APPLICATION_ZIP = "application/zip";

	public static final int FILE_HANDLE_ID_MODULO_DIVISOR = 1000;

	public static final String FILE_EXCEEDS_THE_MAXIMUM_SIZE_LIMIT = "File exceeds the maximum size limit.";

	public static final String RESULT_FILE_HAS_REACHED_THE_MAXIMUM_SIZE = "Result file has reached the maximum size.";

	public static final String FILE_ALREADY_ADDED = "File already added.";

	static private Logger log = LogManager
			.getLogger(BulkFileDownloadWorker.class);

	/**
	 * The maximum total size in bytes of generated zip files.
	 */
	public static final long MAX_TOTAL_FILE_SIZE_BYTES = 1024 * 1024 * 1024; // 1 GB.

	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	UserManager userManger;
	@Autowired
	BulkDownloadManager bulkDownloadManager; 

	@Override
	public void run(ProgressCallback progressCallback, Message message)
			throws RecoverableMessageException, Exception {

		AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		try {

			if (!(status.getRequestBody() instanceof BulkFileDownloadRequest)) {
				throw new IllegalArgumentException("Unexpected request body: "
						+ status.getRequestBody());
			}
			BulkFileDownloadRequest request = AsynchJobUtils.extractRequestBody(status, BulkFileDownloadRequest.class);
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
	 * @throws IOException 
	 */
	public BulkFileDownloadResponse buildZip(
			final ProgressCallback progressCallback,
			final Message message, AsynchronousJobStatus status,
			BulkFileDownloadRequest request) throws IOException {
		// The generated zip will be written to this temp file.
		File tempResultFile = bulkDownloadManager.createTempFile("Job"
				+ status.getJobId(), ".zip");
		ZipOutputStream zipOut = bulkDownloadManager.createZipOutputStream(tempResultFile);
		try {
			UserInfo user = userManger.getUserInfo(status.getStartedByUserId());
			/*
			 * The first step is to determine if the user is authorized to
			 * download each requested file. The authorization check is
			 * normalized around the associated object.
			 */
			List<FileHandleAssociationAuthorizationStatus> authResults = bulkDownloadManager
					.canDownLoadFile(user, request.getRequestedFiles());
			// Track the files added to the zip.
			Set<String> fileIdsInZip = Sets.newHashSet();
			// Build the zip
			List<FileDownloadSummary> results = addFilesToZip(progressCallback,
					message, authResults, tempResultFile, zipOut, status, fileIdsInZip);
			
			IOUtils.closeQuietly(zipOut);
			// Is there at least one file in the zip?
			String resultFileHandleId = null;
			if(fileIdsInZip.size() > 0){
				// upload the result file to S3
				S3FileHandle resultHandle = bulkDownloadManager
						.multipartUploadLocalFile(user, tempResultFile,
								APPLICATION_ZIP, new ProgressListener() {
									@Override
									public void progressChanged(
											ProgressEvent progressEvent) {
									}
								});
				resultFileHandleId = resultHandle.getId();
			}

			// All of the parts are ready.
			BulkFileDownloadResponse response = new BulkFileDownloadResponse();
			response.setFileSummary(results);
			// added for PLFM-3629
			response.setUserId(""+user.getId());
			response.setResultZipFileHandleId(resultFileHandleId);
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
	public List<FileDownloadSummary> addFilesToZip(
			ProgressCallback progressCallback, Message message,
			List<FileHandleAssociationAuthorizationStatus> authResults,
			File tempResultFile, ZipOutputStream zipOut,
			AsynchronousJobStatus status,
			Set<String> fileIdsInZip) {
		long currentProgress = 0L;
		final long totalProgress = (long) authResults.size();
		// This will be the final summary of results..
		List<FileDownloadSummary> fileSummaries = Lists.newLinkedList();
		// process each request in order.
		for (FileHandleAssociationAuthorizationStatus fhas : authResults) {
			String fileHandleId = fhas.getAssociation().getFileHandleId();
			// update the job progress
			asynchJobStatusManager.updateJobProgress(status.getJobId(),
					currentProgress, totalProgress, PROCESSING_FILE_HANDLE_ID
							+ fhas.getAssociation().getFileHandleId());
			FileDownloadSummary summary = new FileDownloadSummary();
			summary.setFileHandleId(fileHandleId);
			summary.setAssociateObjectId(fhas.getAssociation().getAssociateObjectId());
			summary.setAssociateObjectType(fhas.getAssociation().getAssociateObjectType());
			fileSummaries.add(summary);
			try {
				String zipEntryName = writeOneFileToZip(zipOut, tempResultFile.length(), fhas, fileIdsInZip);
				// download this file from S3
				fileIdsInZip.add(fileHandleId);
				summary.setStatus(FileDownloadStatus.SUCCESS);
				summary.setZipEntryName(zipEntryName);
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
				summary.setFailureCode(FileDownloadCode.UNKNOWN_ERROR);
				log.error("Failed on: " + fhas.getAssociation(), e);
			}
			currentProgress++;
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
	 * @return The zip entry name used for this file.
	 */
	public String writeOneFileToZip(ZipOutputStream zipOut, long zipFileSize,
			FileHandleAssociationAuthorizationStatus fhas,
			Set<String> fileIdsInZip) throws IOException {
		String fileHandleId = fhas.getAssociation().getFileHandleId();
		// Is the user authorized to download this file?
		if (!fhas.getStatus().getAuthorized()) {
			throw new BulkFileException(fhas.getStatus().getReason(),
					FileDownloadCode.UNAUTHORIZED);
		}
		// Each file handle should only be added once
		if (fileIdsInZip.contains(fileHandleId)) {
			throw new BulkFileException(FILE_ALREADY_ADDED,
					FileDownloadCode.DUPLICATE);
		}
		// Each file must be less than the max.
		if (zipFileSize > MAX_TOTAL_FILE_SIZE_BYTES) {
			throw new BulkFileException(
					RESULT_FILE_HAS_REACHED_THE_MAXIMUM_SIZE,
					FileDownloadCode.EXCEEDS_SIZE_LIMIT);
		}
		// Get this filehandle.
		S3FileHandle s3Handle = bulkDownloadManager.getS3FileHandle(fileHandleId);
		// Each file must be under the max.s
		if (s3Handle.getContentSize() > MAX_TOTAL_FILE_SIZE_BYTES) {
			throw new BulkFileException(FILE_EXCEEDS_THE_MAXIMUM_SIZE_LIMIT,
					FileDownloadCode.EXCEEDS_SIZE_LIMIT);
		}
		// This file will be downloaded to this temp.
		File downloadTemp = bulkDownloadManager.downloadToTempFile(s3Handle);
		try {
			// The entry name is the path plus file name.
			String zipEntryName = createZipEntryName(s3Handle.getFileName(),
					Long.parseLong(s3Handle.getId()));
			// write the file to the zip.
			bulkDownloadManager.addFileToZip(zipOut, downloadTemp, zipEntryName);
			return zipEntryName;
		} finally {
			downloadTemp.delete();
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
		return String.format(ZIP_ENTRY_TEMPLATE, fileHandleModulus, fileHandleId,
				fileName);
	}

}
