package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.common.util.progress.ProgressListener;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.table.cluster.utils.CSVUtils;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import au.com.bytecode.opencsv.CSVReader;

/**
 * This worker reads CSV files from S3 and appends the data to a given TableEntity.
 * 
 * @author jmhill
 *
 */
@Service
public class TableCSVAppenderPreviewWorker implements AsyncJobRunner<UploadToTablePreviewRequest, UploadToTablePreviewResult> {

	static private Logger log = LogManager.getLogger(TableCSVAppenderPreviewWorker.class);

	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private SynapseS3Client s3Client;

	@Override
	public Class<UploadToTablePreviewRequest> getRequestType() {
		return UploadToTablePreviewRequest.class;
	}

	@Override
	public Class<UploadToTablePreviewResult> getResponseType() {
		return UploadToTablePreviewResult.class;
	}

	@Override
	public UploadToTablePreviewResult run(String jobId, UserInfo user, UploadToTablePreviewRequest request,
			AsyncJobProgressCallback jobProgressCallback)
			throws RecoverableMessageException, Exception {
		CSVReader reader = null;
		try {
			// Get the filehandle
			S3FileHandle fileHandle = (S3FileHandle) fileHandleManager.getRawFileHandle(user, request.getUploadFileHandleId());
			// Get the metadat for this file
			ObjectMetadata fileMetadata = s3Client.getObjectMetadata(fileHandle.getBucketName(), fileHandle.getKey());
			long progressCurrent = 0L;
			final long progressTotal = fileMetadata.getContentLength();
			// Start the progress
			jobProgressCallback.updateProgress("Starting...", progressCurrent, progressTotal);
			// Open a stream to the file in S3.
			S3Object s3Object = s3Client.getObject(fileHandle.getBucketName(), fileHandle.getKey());
			// Create a reader from the passed parameters
			reader = CSVUtils.createCSVReader(new InputStreamReader(s3Object.getObjectContent(), "UTF-8"), request.getCsvTableDescriptor(),
					request.getLinesToSkip());

			// Listen to progress events.
			ProgressListener listener = new ProgressListener() {

				AtomicLong counter = new AtomicLong();

				@Override
				public void progressMade() {
					long count = counter.incrementAndGet();
					// update the job progress.
					jobProgressCallback.updateProgress("Processed: " + (count), count, Long.MAX_VALUE);
				}
			};
			jobProgressCallback.addProgressListener(listener);
			try {
				// This builder does the work of building an actual preview.
				UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, request);
				return builder.buildResult();
			} finally {
				// unconditionally remove the listener
				jobProgressCallback.removeProgressListener(listener);
			}
		} catch (Throwable e) {
			log.error("Worker Failed", e);
			throw e;
		} finally {
			if (reader != null) {
				try {
					// Unconditionally close the stream to the S3 file.
					reader.close();
				} catch (IOException e) {
				}
			}
		}
	}

}
