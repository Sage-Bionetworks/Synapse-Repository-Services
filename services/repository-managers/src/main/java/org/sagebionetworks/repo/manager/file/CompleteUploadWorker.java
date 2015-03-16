package org.sagebionetworks.repo.manager.file;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.dao.UploadDaemonStatusDao;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.State;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;

/**
 * This worker will add each chunk to the larger multi-part file and complete the multi-part process.
 * @author jmhill
 *
 */
public class CompleteUploadWorker implements Callable<Boolean>{
	
	static private Log log = LogFactory.getLog(CopyPartWorker.class);
	
	UploadDaemonStatusDao uploadDaemonStatusDao;
	ExecutorService uploadFileDaemonThreadPoolSecondary;
	UploadDaemonStatus uploadStatus;
	CompleteAllChunksRequest cacf;
	MultipartManager multipartManager;
	long maxWaitMS;
	String userId;

	/**
	 * A new worker should be created for each file.
	 * @param uploadDaemonStatusDao
	 * @param uploadFileDaemonThreadPoolSecondary
	 * @param uploadStatus
	 * @param cacf
	 * @param multipartManager
	 * @param bucket
	 * @param maxWaitMS
	 */
	public CompleteUploadWorker(UploadDaemonStatusDao uploadDaemonStatusDao,
			ExecutorService uploadFileDaemonThreadPoolSecondary,
			UploadDaemonStatus uploadStatus, CompleteAllChunksRequest cacf,
 MultipartManager multipartManager, long maxWaitMS, String userId) {
		super();
		this.uploadDaemonStatusDao = uploadDaemonStatusDao;
		this.uploadFileDaemonThreadPoolSecondary = uploadFileDaemonThreadPoolSecondary;
		this.uploadStatus = uploadStatus;
		this.cacf = cacf;
		this.multipartManager = multipartManager;
		this.maxWaitMS = maxWaitMS;
		this.userId = userId;
	}


	/**
	 * Create a new worker that should be used only once.  A new worker should be created for each job.
	 * @param uploadDaemonStatusDao
	 * @param fileHandleDao
	 * @param uploadStatus
	 * @param cacf
	 */


	@Override
	public Boolean call() throws Exception {
		try {
			// First, add each part to the upload
			long start = System.currentTimeMillis();
			List<Future<ChunkResult>> futures = new LinkedList<Future<ChunkResult>>();
			for(Long partNumber: cacf.getChunkNumbers()){
				// Submit each future
				CopyPartWorker worker = new CopyPartWorker(multipartManager, cacf.getChunkedFileToken(), partNumber.intValue(), maxWaitMS);
				Future<ChunkResult> future = uploadFileDaemonThreadPoolSecondary.submit(worker);
				futures.add(future);
			}
			// Now we need to wait for all of the workers to copy their parts.
			List<ChunkResult> done = waitForCopyWorkers(futures);
			// Once the parts are done we can complete the upload
			CompleteChunkedFileRequest ccfr = new CompleteChunkedFileRequest();
			ccfr.setChunkedFileToken(cacf.getChunkedFileToken());
			ccfr.setChunkResults(done);
			ccfr.setShouldPreviewBeGenerated(cacf.getShouldPreviewBeGenerated());
			S3FileHandle handle = multipartManager.completeChunkFileUpload(ccfr, ccfr.getChunkedFileToken().getStorageLocationId(), userId);
			if(handle == null) throw new RuntimeException("multipartManager.completeChunkFileUpload() returned null"); 
			if(handle.getId() == null) throw new RuntimeException("multipartManager.completeChunkFileUpload() returned FileHandle ID"); 
			// Update the status
			uploadStatus.setErrorMessage(null);
			uploadStatus.setPercentComplete(100.0);
			uploadStatus.setFileHandleId(handle.getId());
			uploadStatus.setState(State.COMPLETED);
			uploadStatus.setRunTimeMS(System.currentTimeMillis()-start);
			uploadDaemonStatusDao.update(uploadStatus);
			return true;
		} catch (Exception e) {
			// If we fail for any reason then update the status
			uploadStatus.setErrorMessage(e.getMessage());
			uploadStatus.setState(State.FAILED);
			uploadDaemonStatusDao.update(uploadStatus);
			log.error("Failed to complete multi-part upload", e);
			return false;
		}
	}
	
	/**
	 * Wait for all of the workers to finish the copy of each part.
	 * @param futures
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private List<ChunkResult> waitForCopyWorkers(List<Future<ChunkResult>> futures) throws InterruptedException, ExecutionException {
		// Now we need to wait for all of the workers to copy the parts.
		long start = System.currentTimeMillis();
		// We keep a second start time that we can reset as long as we are making progress.
		long resetableStart = start;
		int totalCount = futures.size();
		int doneCount =0;
		List<ChunkResult> done = new LinkedList<ChunkResult>();
		while(done.size() < totalCount){
			// Sleep
			log.debug("Waiting for copy workers. Done: "+done.size()+" Total: "+totalCount);
			Thread.sleep(1000);
			// We are still waiting for the workers to finish
			if(System.currentTimeMillis() - resetableStart > maxWaitMS){
				throw new RuntimeException("Timed out waiting for the multi-part load to finish: "+cacf.toString());
			}
			List<Future<ChunkResult>> toRemove = new LinkedList<Future<ChunkResult>>();
			// Find a part that is done
			for(Future<ChunkResult> future: futures){
				if(future.isDone()){
					// Get the result
					ChunkResult cr = future.get();
					// Add it to the 
					done.add(cr);
					doneCount++;
					// We need to remove this from the list
					toRemove.add(future);
				}
			}
			// Remove the done futures
			for(Future<ChunkResult> df: toRemove){
				futures.remove(df);
			}
			// Update the status if it has changed
			float percent = ((float)doneCount)/((float)totalCount+1)*100;
			if(percent > uploadStatus.getPercentComplete()){
				uploadStatus.setPercentComplete(new Double(percent));
				uploadStatus.setRunTimeMS(System.currentTimeMillis()-start);
				uploadDaemonStatusDao.update(uploadStatus);
				// As long as we are making progress reset the timer
				resetableStart = System.currentTimeMillis();
			}
		}
		return done;
	}

}
