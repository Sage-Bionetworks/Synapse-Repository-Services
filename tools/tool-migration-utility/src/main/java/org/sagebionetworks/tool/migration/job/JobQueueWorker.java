package org.sagebionetworks.tool.migration.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.sagebionetworks.tool.migration.BasicProgress;
import org.sagebionetworks.tool.migration.ClientFactory;
import org.sagebionetworks.tool.migration.job.Job.Type;
import org.sagebionetworks.tool.migration.job.WorkerResult.JobStatus;

/**
 * This worker will clear the queue using all available threads, then 
 * report the status.
 * 
 * @author John
 * 
 */
public class JobQueueWorker implements Callable<AggregateResult> {

	Queue<Job> jobQueue;
	ExecutorService threadPool;
	ClientFactory factory;
	BasicProgress progress;

	public JobQueueWorker(Queue<Job> jobQueue, ExecutorService threadPool,
			ClientFactory factory, BasicProgress progress) {
		super();
		this.jobQueue = jobQueue;
		this.threadPool = threadPool;
		this.factory = factory;
		this.progress = progress;
	}

	@Override
	public AggregateResult call() throws Exception {
		// This worker will clear the queue then terminate.
		List<Future<WorkerResult>> futures = new ArrayList<Future<WorkerResult>>();
		Job job = null;
		// Build up the progress
		long total = 0;
		while ((job = this.jobQueue.poll()) != null) {
			if (Type.CREATE == job.getJobType()	|| Type.UPDATE == job.getJobType()) {
				// Create a works
				CreateUpdateWorker worker = new CreateUpdateWorker(this.factory, job.getEntityIds());
				// Get a thread working on this
				futures.add(threadPool.submit(worker));
			}else if (Type.DELETE == job.getJobType()) {
				// Create a works
				DeleteWorker worker = new DeleteWorker(this.factory, job.getEntityIds());
				// Get a thread working on this
				futures.add(threadPool.submit(worker));
			} else {
				throw new IllegalArgumentException("Unknown job type: "	+ job.getJobType());
			}
			// Setup the total progress
			total += job.getEntityIds().size();

			// Yield between each job.
			Thread.yield();
		}
		progress.setTotal(total);
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
			// Update the progress
			progress.setCurrent(totalEntitesProcessed);
		}
		// We are done
		progress.setCurrent(progress.getTotal());
		return new AggregateResult(totalEntitesProcessed, failedJobCount, successfulJobCount);
	}

}
