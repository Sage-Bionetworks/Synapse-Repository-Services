package org.sagebionetworks.tool.migration.job;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.sagebionetworks.tool.migration.ClientFactory;
import org.sagebionetworks.tool.migration.Configuration;
import org.sagebionetworks.tool.migration.Progress.AggregateProgress;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;
import org.sagebionetworks.tool.migration.job.Job.Type;
import org.sagebionetworks.tool.migration.job.WorkerResult.JobStatus;
import org.sagebionetworks.tool.searchupdater.job.SearchDocumentAddWorker;
import org.sagebionetworks.tool.searchupdater.job.SearchDocumentDeleteWorker;

/**
 * This worker will clear the queue using all available threads, then 
 * report the status.
 * 
 * @author John
 * 
 */
public class JobQueueWorker implements Callable<AggregateResult> {

	Configuration configuration = null;
	Queue<Job> jobQueue;
	ExecutorService threadPool;
	ClientFactory factory;
	AggregateProgress progress;

	public JobQueueWorker(Configuration configuration, Queue<Job> jobQueue, ExecutorService threadPool,
			ClientFactory factory, AggregateProgress progress) {
		super();
		this.configuration = configuration;
		this.jobQueue = jobQueue;
		this.threadPool = threadPool;
		this.factory = factory;
		this.progress = progress;
	}

	@Override
	public AggregateResult call() throws Exception {
		// This worker will clear the queue then terminate.

		Job job = null;
		// Build up the progress
		long total = 0;
		List<Callable<WorkerResult>> workerList = new LinkedList<Callable<WorkerResult>>();
		while ((job = this.jobQueue.poll()) != null) {
			// Setup progress for each job
			BasicProgress progress = new BasicProgress();
			progress.setCurrent(0);
			progress.setTotal(job.getEntityIds().size());
			this.progress.addProgresss(progress);
			// Start the works
			if (Type.CREATE == job.getJobType()	|| Type.UPDATE == job.getJobType()) {
				// Create a works
				CreateUpdateWorker worker = new CreateUpdateWorker(configuration, this.factory, job.getEntityIds(), progress);
				// add this worker to the list
				workerList.add(worker);
			} else if (Type.DELETE == job.getJobType()) {
				// Create a works
				DeleteWorker worker = new DeleteWorker(configuration, this.factory, job.getEntityIds(), progress);
				// add this worker to the list
				workerList.add(worker);
			} else if (Type.SEARCH_ADD == job.getJobType()) {
				SearchDocumentAddWorker worker = new SearchDocumentAddWorker(configuration, this.factory, job.getEntityIds(), progress);
				workerList.add(worker);
			} else if (Type.SEARCH_DELETE == job.getJobType()) {
				SearchDocumentDeleteWorker worker = new SearchDocumentDeleteWorker(configuration, this.factory, job.getEntityIds(), progress);
				workerList.add(worker);
			} else {
				throw new IllegalArgumentException("Unknown job type: "	+ job.getJobType());
			}
			// Setup the total progress
			total += job.getEntityIds().size();

			// Yield between each job.
			Thread.yield();
		}
		// Add all of the workers to the thread queue and start working on them.
		List<Future<WorkerResult>> futures = threadPool.invokeAll(workerList);
		// Now wait for all Future to complete
		int totalEntitesProcessed = 0;
		int failedJobCount = 0;
		int successfulJobCount = 0;
		// Wait for
		for(Future<WorkerResult> future: futures){
			// Wait for all to finish
			WorkerResult result = future.get();
			if(JobStatus.FAILED == result.getStatus()){
				failedJobCount++;
			}else{
				successfulJobCount++;
			}
			totalEntitesProcessed += result.getEntitesProcessed();
		}
		// We are done
		return new AggregateResult(totalEntitesProcessed, failedJobCount, successfulJobCount);
	}

}
