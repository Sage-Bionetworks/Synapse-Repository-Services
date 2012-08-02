package org.sagebionetworks.tool.migration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.tool.migration.Progress.AggregateProgress;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;
import org.sagebionetworks.tool.migration.dao.MigrationQueryRunner;
import org.sagebionetworks.tool.migration.job.AggregateResult;
import org.sagebionetworks.tool.migration.job.BuilderResponse;
import org.sagebionetworks.tool.migration.job.CreationJobBuilder;
import org.sagebionetworks.tool.migration.job.DeleteJobBuilder;
import org.sagebionetworks.tool.migration.job.Job;
import org.sagebionetworks.tool.migration.job.JobQueueWorker;
import org.sagebionetworks.tool.migration.job.JobUtil;
import org.sagebionetworks.tool.migration.job.UpdateJobBuilder;

/**
 * The main driver for migration.
 * @author John
 *
 */
public class RepositoryMigrationDriver {

	static private Log log = LogFactory.getLog(RepositoryMigrationDriver.class);
	static private MigrationConfigurationImpl configuration = new MigrationConfigurationImpl();

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		loadConfigUsingArgs(configuration, args);
		
		// Create the two connections.
		SynapseConnectionInfo sourceInfo = configuration.getSourceConnectionInfo();
		SynapseConnectionInfo destInfo = configuration.getDestinationConnectionInfo();
		// Create a source and destination
		ClientFactoryImpl factory = new ClientFactoryImpl();
		SynapseAdministration sourceClient = factory.createNewConnection(sourceInfo);
		SynapseAdministration destClient = factory.createNewConnection(destInfo);

		// Create the query providers
		MigrationQueryRunner sourceQueryRunner = new MigrationQueryRunner(sourceClient, /*queryForDependencies*/true);
		MigrationQueryRunner destQueryRunner = new MigrationQueryRunner(destClient, /*queryForDependencies*/false);
		long sourceTotal = sourceQueryRunner.getTotalEntityCount();
		long destTotal = destQueryRunner.getTotalEntityCount();
		// Do a safety check.  If the destination has more entities than the source then confirm with the user that they want to proceed.
		safetyCheck(sourceInfo.getRepositoryEndPoint(), destInfo.getRepositoryEndPoint(), sourceTotal, destTotal);
		// Create the thread pool
		int maxThreads = configuration.getMaximumNumberThreads();
		// We must have at least 2 threads
		if(maxThreads < 2){
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
		// 2. Calculate what needs to be created, updated, or deleted and create a job for each.
		// 3. Process all jobs on the queue.
		// Wash/rinse repeat.
		long totalStart = System.currentTimeMillis();
		while(true){
			// 1. Get all entity data from both the source and destination.
			BasicProgress sourceProgress = new BasicProgress();
			BasicProgress destProgress = new BasicProgress();
			AllEntityDataWorker<MigratableObjectData> sourceQueryWorker = new AllEntityDataWorker<MigratableObjectData>(sourceQueryRunner, sourceProgress);
			AllEntityDataWorker<MigratableObjectData> destQueryWorker = new AllEntityDataWorker<MigratableObjectData>(destQueryRunner, destProgress);
			// Start both at the same time
			Future<List<MigratableObjectData>> sourceFuture = threadPool.submit(sourceQueryWorker);
			Future<List<MigratableObjectData>> destFuture = threadPool.submit(destQueryWorker);
			// Wait for both to finish
			log.info("Starting phase one: Gathering all data from the source and destination repository...");
			while(!sourceFuture.isDone() || !destFuture.isDone()){
				// Report on the progress.
				log.info("     Source query: "+sourceProgress.getCurrentStatus());
				log.info("Destination query: "+destProgress.getCurrentStatus());
				Thread.sleep(2000);
			}
			
			// Get the results
			List<MigratableObjectData> sourceData = sourceFuture.get();
			List<MigratableObjectData> destData = destFuture.get();
			log.debug("Finished phase one.  Source entity count: "+sourceData.size()+". Destination entity Count: "+destData.size());
			// Start phase 2
			log.debug("Starting phase two: Calculating creates, updates, and deletes...");
			populateQueue(threadPool, jobQueue, sourceData, destData, configuration.getMaximumBatchSize());
			
			// 3. Process all jobs on the queue.
			log.debug("Starting phase three: Processing the job queue...");
			AggregateProgress consumingProgress = new AggregateProgress();
			Future<AggregateResult> consumFuture = consumeAllJobs(factory, threadPool, jobQueue, consumingProgress);
			while(!consumFuture.isDone()){
				log.info("Processing entities: "+consumingProgress.getCurrentStatus());
				Thread.sleep(2000);
			}
			AggregateResult result = consumFuture.get();
			entitesProcessed += result.getTotalEntitesProcessed();
			failedJobs += result.getFailedJobCount();
			successJobs += result.getSuccessfulJobCount();
			String format = "FAILED jobs: %1$-10d SUCCESSFUL jobs: %2$-10d total entities processed: %3$-10d";
			log.info("Cleared the queue: "+String.format(format, failedJobs, successJobs, entitesProcessed));
			// If there are any failures exist
			long endTotal = System.currentTimeMillis();
			log.info("Total elapse time: "+(endTotal-totalStart)+" ms");
			Thread.sleep(1000*5);
		}
		
	}

	/**
	 * Load the configuration using the passed args.
	 * @param configuration 
	 * @param args
	 * @throws IOException
	 */
	public static void loadConfigUsingArgs(MigrationConfigurationImpl configuration, String[] args) throws IOException {
		// Load the location of the configuration property file
		if (args == null) {
			throw new IllegalArgumentException(	"The first argument must be the configuration property file path.");
		}
		if (args.length != 1) {
			throw new IllegalArgumentException(	"The first argument must be the configuration property file path. args.length: "+args.length);
		}
		String path = args[0];
		// Load all of the configuration information.
		configuration.loadConfigurationFile(path);
	}

	/**
	 * Consume all jobs on the queue.  This method will block until the queue is empty.
	 * @param factory
	 * @param threadPool
	 * @param jobQueue
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static Future<AggregateResult> consumeAllJobs(ClientFactoryImpl factory,	ExecutorService threadPool, Queue<Job> jobQueue, AggregateProgress progress)
			throws InterruptedException, ExecutionException {
		// Create a new worker job.
		JobQueueWorker queueWorker = new JobQueueWorker(configuration, jobQueue, threadPool, factory, progress);
		// Start the worker job.
		return threadPool.submit(queueWorker);
	}
	
	/**
	 * Populate the queue with create, update, and delete jobs using what we know about the source and destination repositories using the configured batch size.
	 * This will block until the queue is populated.
	 * @param threadPool
	 * @param jobQueue
	 * @param sourceData
	 * @param destData
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static ResponseBundle populateQueue(ExecutorService threadPool, Queue<Job> jobQueue, List<MigratableObjectData> sourceData, List<MigratableObjectData> destData) throws InterruptedException,
			ExecutionException {
		return populateQueue(threadPool, jobQueue, sourceData, destData, configuration.getMaximumBatchSize());
	}
	/**
	 * Populate the queue with create, update, and delete jobs using what we know about the source and destination repositories.
	 * This will block until the queue is populated.
	 * @param threadPool
	 * @param jobQueue
	 * @param sourceData
	 * @param destData
	 * @param maxBatchSize 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static ResponseBundle populateQueue(ExecutorService threadPool, Queue<Job> jobQueue, List<MigratableObjectData> sourceData, List<MigratableObjectData> destData, int maxBatchSize) throws InterruptedException,
			ExecutionException {
		// Build the maps
		Map<MigratableObjectDescriptor, MigratableObjectData> destMap = JobUtil.buildMigratableMapFromList(destData);
		Map<MigratableObjectDescriptor, MigratableObjectData> sourceMap = JobUtil.buildMigratableMapFromList(sourceData);
		// Build up the create jobs
		CreationJobBuilder createBuilder = new CreationJobBuilder(sourceData, destMap, jobQueue, maxBatchSize);
		Future<BuilderResponse> createFuture = threadPool.submit(createBuilder);
		// Build up the update jobs
		UpdateJobBuilder updateBuilder = new UpdateJobBuilder(sourceData, destMap, jobQueue, maxBatchSize);
		Future<BuilderResponse> updateFuture = threadPool.submit(updateBuilder);
		// build up the delete jobs
		DeleteJobBuilder deleteBuilder = new DeleteJobBuilder(sourceMap, destData, jobQueue, maxBatchSize);
		Future<BuilderResponse> deleteFuture = threadPool.submit(deleteBuilder);
		// Wait for each to complete
		BuilderResponse createResponse = createFuture.get();
		BuilderResponse updateResponse = updateFuture.get();
		BuilderResponse deleteResponse = deleteFuture.get();
		log.info("Submitted "+createResponse.getSubmittedToQueue()+" Entities to create queue.  There are "+createResponse.getPendingDependencies()+" Entities pending dependency creations. Submitted "+updateResponse.getSubmittedToQueue()+" updates to the queue. Submitted "+deleteResponse.getSubmittedToQueue()+" for delete.");
		
		return new ResponseBundle(createResponse, updateResponse, deleteResponse);
	}

	/**
	 * This is a safety check.  If there are more entities in the destination than the source confirm with the caller that they want to continue.
	 * @param sourceInfo
	 * @param destInfo
	 * @param sourceTotal
	 * @param destTotal
	 */
	public static void safetyCheck(String sourceEndpoint, String destEndpoint, long sourceTotal, long destTotal) {
		log.info("Source: " + sourceEndpoint + " has: "+ sourceTotal + " Entities");
		log.info("Destination: " + destEndpoint + " has: "+ destTotal + " Entities");
		// If there are more in in the source than the destination the make sure
		// the user wants to proceed
		if (destTotal > sourceTotal) {
			System.out.println("The destination repository has more Entities than the source repository:");
			String format = "%1$15s total entities: %3$-10d endpoint: %2$s ";
			System.out.println(String.format(format, "DESTINATION" ,destEndpoint, destTotal));
			System.out.println(String.format(format, "SOURCE" ,sourceEndpoint, sourceTotal));
			System.out.print("Are you sure you want to continue? (Y/N): ");
			// open up standard input
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String input = null;
			try {
				input = br.readLine();
				if(input == null){
					System.out.println("No input");
					System.exit(1);
				}
				if(!"y".equals(input.toLowerCase())){
					System.out.println("Stopping.");
					System.exit(0);
				}
				System.out.println("Continuing...");
			} catch (IOException ioe) {
				System.out.println("IO error trying to read your name!");
				System.exit(1);
			}
		}
	}

}
