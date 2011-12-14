package org.sagebionetworks.tool.migration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.dao.QueryRunner;
import org.sagebionetworks.tool.migration.dao.QueryRunnerImpl;
import org.sagebionetworks.tool.migration.job.AggregateResult;
import org.sagebionetworks.tool.migration.job.CreationJobBuilder;
import org.sagebionetworks.tool.migration.job.CreatorResponse;
import org.sagebionetworks.tool.migration.job.Job;
import org.sagebionetworks.tool.migration.job.JobQueueWorker;
import org.sagebionetworks.tool.migration.job.JobUtil;

public class MigrationDriver {

	static private Log log = LogFactory.getLog(MigrationDriver.class);

	/**
	 * @param args
	 * @throws IOException
	 * @throws SynapseException
	 * @throws JSONException
	 * @throws InterruptedException 
	 * @throws ExecutionException 
	 */
	public static void main(String[] args) throws IOException,
			SynapseException, JSONException, InterruptedException, ExecutionException {
		// Load the location of the configuration property file
		if (args == null || args.length != 1) {
			throw new IllegalArgumentException(	"The first argument must be the configuation property file path");
		}
		String path = args[0];
		// Load all of the configuration information.
		Configuration.loadConfigurationFile(path);

		// Create the two connections.
		SynapseConnectionInfo sourceInfo = Configuration.getSourceConnectionInfo();
		SynapseConnectionInfo destInfo = Configuration.getDestinationConnectionInfo();
		// Create a source and destination
		ClientFactoryImpl factory = new ClientFactoryImpl();
		Synapse sourceClient = factory.createNewConnection(sourceInfo);
		Synapse destClient = factory.createNewConnection(destInfo);

		// Create the query provider
		QueryRunner queryRunner = new QueryRunnerImpl();
		long sourceTotal = queryRunner.getTotalEntityCount(sourceClient);
		long destTotal = queryRunner.getTotalEntityCount(destClient);
		// Do a safety check.  If the destination has more entities than the source then confirm with the user that they want to proceed.
		safetyCheck(sourceInfo.getRepositoryEndPoint(), destInfo.getRepositoryEndPoint(), sourceTotal, destTotal);
		// Create the thread pool
		int maxThreads = Configuration.getMaximumNumberThreads();
		// We must have at least 2 threads
		if(maxThreads < 2){
			maxThreads = 2;
		}
		ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);
		// The JOB queue
		Queue<Job> jobQueue = new LinkedList<Job>();
		
		// Start up the thread 
		int entitesProcessed = 0;
		int failedJobs = 0;
		int successJobs = 0;
		while(true){
			// Query for the data to test for
			List<EntityData> sourceData = queryRunner.getAllEntityData(sourceClient);
			List<EntityData> destData = queryRunner.getAllEntityData(destClient);
			Map<String, EntityData> destMap = JobUtil.buildMapFromList(destData);
			// Build up the create jobs
			CreationJobBuilder createBuilder = new CreationJobBuilder(sourceData, destMap, jobQueue, Configuration.getMaximumBatchSize());
			Future<CreatorResponse> createFuture = threadPool.submit(createBuilder);
			
			// Wait for each to complete
			CreatorResponse response = createFuture.get();
			log.info("Summited "+response.getSubmitedToQueue()+" entites to create queue.  There are "+response.getPendingDependancies()+" entites pending dependancy creations.");
			
			// Now process the queue with all threads
			JobQueueWorker queueWorker = new JobQueueWorker(jobQueue, threadPool, factory);
			// Wait for the worker
			Future<AggregateResult> workerFuture = threadPool.submit(queueWorker);
			// Wait for it to finish
			AggregateResult result = workerFuture.get();
			entitesProcessed += result.getTotalEntitesProcessed();
			failedJobs += result.getFailedJobCount();
			successJobs += result.getSuccessfulJobCount();
			String format = "FAILED jobs: %1$-10d SUCCESSFUL jobs: %2$-10d total entities processed: %3$-10d";

			log.info("Cleared the queue: "+String.format(format, failedJobs, successJobs, entitesProcessed));
			// If there are any failures exist
			if(failedJobs > 0 ){
				System.out.println("There are failed jobs, so existing");
				System.exit(1);
			}
			Thread.sleep(1000);
		}
		
	}

	/**
	 * This is a safety check.  If there are more entities in the destination than the source confirm with the caller that they want to continue.
	 * @param sourceInfo
	 * @param destInfo
	 * @param sourceTotal
	 * @param destTotal
	 */
	public static void safetyCheck(String sourceEndpoint, String destEndpoint, long sourceTotal, long destTotal) {
		log.info("Source: " + sourceEndpoint + " has: "+ sourceTotal + " entites");
		log.info("Destination: " + destEndpoint + " has: "+ destTotal + " entites");
		// If there are more in in the source than the destination the make sure
		// the user wants to proceed
		if (destTotal > sourceTotal) {
			System.out.println("The destination repostiory has more Entities than the source repostiory:");
			String format = "%1$15s total entites: %3$-10d endpoint: %2$s ";
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
