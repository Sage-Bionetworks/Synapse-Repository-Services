package org.sagebionetworks.tool.searchupdater;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.tool.migration.AllEntityDataWorker;
import org.sagebionetworks.tool.migration.ClientFactoryImpl;
import org.sagebionetworks.tool.migration.ResponseBundle;
import org.sagebionetworks.tool.migration.Progress.AggregateProgress;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.dao.QueryRunner;
import org.sagebionetworks.tool.migration.dao.QueryRunnerImpl;
import org.sagebionetworks.tool.migration.job.AggregateResult;
import org.sagebionetworks.tool.migration.job.BuilderResponse;
import org.sagebionetworks.tool.migration.job.Job;
import org.sagebionetworks.tool.migration.job.JobQueueWorker;
import org.sagebionetworks.tool.migration.job.JobUtil;
import org.sagebionetworks.tool.searchupdater.dao.SearchRunnerImpl;
import org.sagebionetworks.tool.searchupdater.job.SearchDocumentAddJobBuilder;
import org.sagebionetworks.tool.searchupdater.job.SearchDocumentDeleteJobBuilder;
import org.sagebionetworks.utils.HttpClientHelperException;

/**
 * Search index updater daemon modeled after the repository migration driver.
 * Note that this is a temporary implementation. This use case is less complex
 * than repository migration and well-solved by Simple Workflow but they haven't
 * launch their new version yet and the API has changed dramatically. To meet
 * sprint timelines I'm leveraging the jobs/worker infrastructure here and will
 * port this to SWF at a later date.
 * 
 * @author deflaux
 * 
 */
public class SearchMigrationDriver {

	static private Log log = LogFactory.getLog(SearchMigrationDriver.class);
	static private SearchUpdaterConfigurationImpl configuration = new SearchUpdaterConfigurationImpl();

	/**
	 * @param args
	 * @throws IOException
	 * @throws SynapseException
	 * @throws JSONException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws HttpClientHelperException
	 */
	public static void main(String[] args) throws IOException,
			SynapseException, JSONException, InterruptedException,
			ExecutionException, HttpClientHelperException {

		ClientFactoryImpl factory = new ClientFactoryImpl();
		SynapseAdministration sourceClient = configuration
				.createSynapseClient();
		CloudSearchClient destClient = configuration.createCloudSearchClient();

		// Create the query provider
		QueryRunner sourceQueryRunner = new QueryRunnerImpl(sourceClient);
		QueryRunner destQueryRunner = new SearchRunnerImpl(destClient);
		long sourceTotal = sourceQueryRunner.getTotalEntityCount();
		long destTotal = destQueryRunner.getTotalEntityCount();
		log.info("     Source Entity Count: " + sourceTotal);
		log.info("Destination Entity Count: " + destTotal);

		// TODO: ANOTHER SAFTEY CHECK HERE?

		// Create the thread pool
		int maxThreads = configuration.getMaximumNumberThreads();
		// We must have at least 2 threads
		if (maxThreads < 2) {
			maxThreads = 2;
		}
		ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);
		// The JOB queue
		Queue<Job> jobQueue = new ConcurrentLinkedQueue<Job>();

		// Start up the thread
		int entitesProcessed = 0;
		int failedJobs = 0;
		int successJobs = 0;
		// There are three parts to this loop.
		// 1. Get all entity data from both the source and destination.
		// 2. Calculate what needs to be created, updated, or deleted and create
		// a job for each.
		// 3. Process all jobs on the queue.
		// Wash/rinse repeat.
		long totalStart = System.currentTimeMillis();
		while (true) {
			// 1. Get all entity data from both the source and destination.

			BasicProgress sourceProgress = new BasicProgress();
			BasicProgress destProgress = new BasicProgress();
			AllEntityDataWorker sourceQueryWorker = new AllEntityDataWorker(
					sourceQueryRunner, sourceProgress);
			AllEntityDataWorker destQueryWorker = new AllEntityDataWorker(
					destQueryRunner, destProgress);

			// Start both at the same time
			Future<List<EntityData>> sourceFuture = threadPool
					.submit(sourceQueryWorker);
			Future<List<EntityData>> destFuture = threadPool
					.submit(destQueryWorker);
			// Wait for both to finish
			log
					.info("Starting phase one: Gathering all data from the source and destination repository...");
			while (!sourceFuture.isDone() || !destFuture.isDone()) {
				// Report on the progress.
				log.info("     Source query: "
						+ sourceProgress.getCurrentStatus());
				log.info("Destination query: "
						+ destProgress.getCurrentStatus());
				Thread.sleep(2000);
			}

			// Get the results
			List<EntityData> sourceData = sourceFuture.get();
			List<EntityData> destData = destFuture.get();
			log.debug("Finished phase one.  Source entity count: "
					+ sourceData.size() + ". Destination entity Count: "
					+ destData.size());
			// Start phase 2
			log
					.debug("Starting phase two: Calculating creates, updates, and deletes...");
			populateQueue(threadPool, jobQueue, sourceData, destData,
					configuration.getMaximumBatchSize());

			// 3. Process all jobs on the queue.
			log.debug("Starting phase three: Processing the job queue...");
			AggregateProgress consumingProgress = new AggregateProgress();
			Future<AggregateResult> consumFuture = consumeAllJobs(factory,
					threadPool, jobQueue, consumingProgress);
			while (!consumFuture.isDone()) {
				log.info("Processing entities: "
						+ consumingProgress.getCurrentStatus());
				Thread.sleep(2000);
			}
			AggregateResult result = consumFuture.get();
			entitesProcessed += result.getTotalEntitesProcessed();
			failedJobs += result.getFailedJobCount();
			successJobs += result.getSuccessfulJobCount();
			String format = "FAILED jobs: %1$-10d SUCCESSFUL jobs: %2$-10d total entities processed: %3$-10d";
			log.info("Cleared the queue: "
					+ String.format(format, failedJobs, successJobs,
							entitesProcessed));
			// If there are any failures exist
			long endTotal = System.currentTimeMillis();
			log.info("Total elapse time: " + (endTotal - totalStart) + " ms");
			Thread.sleep(1000 * 60);
		}
	}

	/**
	 * Consume all jobs on the queue. This method will block until the queue is
	 * empty.
	 * 
	 * @param factory
	 * @param threadPool
	 * @param jobQueue
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static Future<AggregateResult> consumeAllJobs(
			ClientFactoryImpl factory, ExecutorService threadPool,
			Queue<Job> jobQueue, AggregateProgress progress)
			throws InterruptedException, ExecutionException {
		// Create a new worker job.
		JobQueueWorker queueWorker = new JobQueueWorker(configuration,
				jobQueue, threadPool, factory, progress);
		// Start the worker job.
		return threadPool.submit(queueWorker);
	}

	/**
	 * Populate the queue with create, update, and delete jobs using what we
	 * know about the source and destination repositories. This will block until
	 * the queue is populated.
	 * 
	 * @param threadPool
	 * @param jobQueue
	 * @param sourceData
	 * @param destData
	 * @param maxBatchSize
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static ResponseBundle populateQueue(ExecutorService threadPool,
			Queue<Job> jobQueue, List<EntityData> sourceData,
			List<EntityData> destData, int maxBatchSize)
			throws InterruptedException, ExecutionException {
		// Build the maps
		Map<String, EntityData> destMap = JobUtil.buildMapFromList(destData);
		Map<String, EntityData> sourceMap = JobUtil
				.buildMapFromList(sourceData);
		// Build up the search document "add" jobs
		SearchDocumentAddJobBuilder searchAddBuilder = new SearchDocumentAddJobBuilder(
				sourceData, destMap, jobQueue, maxBatchSize);
		Future<BuilderResponse> searchAddFuture = threadPool
				.submit(searchAddBuilder);
		// Build up the search document "delete" jobs
		SearchDocumentDeleteJobBuilder searchDeleteBuilder = new SearchDocumentDeleteJobBuilder(
				sourceMap, destData, jobQueue, maxBatchSize);
		Future<BuilderResponse> searchDeleteFuture = threadPool
				.submit(searchDeleteBuilder);

		// Wait for each to complete
		BuilderResponse searchAddResponse = searchAddFuture.get();
		BuilderResponse searchDeleteResponse = searchDeleteFuture.get();
		log.info("Submitted " + searchAddResponse.getSubmitedToQueue()
				+ " entities to be added to the search index.  Submitted "
				+ searchDeleteResponse.getSubmitedToQueue() + " for delete.");

		return new ResponseBundle(searchAddResponse, null, searchDeleteResponse);
	}
}
