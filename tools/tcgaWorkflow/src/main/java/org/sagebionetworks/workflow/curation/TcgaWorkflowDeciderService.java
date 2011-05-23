package org.sagebionetworks.workflow.curation;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.AsynchronyExecutorService;

/**
 * This is the process which hosts all the SWF deciders exposed in this package.
 * 
 * Note that the TCGA Curation pipeline decider has no external dependencies
 * (e.g., it does not need to run on a machine with R and Bioconductor)
 * 
 */
public class TcgaWorkflowDeciderService {

	private static AmazonSimpleWorkflow swfService;
	private static TcgaWorkflowDeciderService tcgaWorkflowDeciderServiceInstance;
	private static AsynchronyExecutorService deciderExecutor;

	/**
	 * @return the decider service instance
	 */
	public synchronized static TcgaWorkflowDeciderService getTcgaWorkflowDeciderServiceInstance() {
		if (tcgaWorkflowDeciderServiceInstance == null) {
			tcgaWorkflowDeciderServiceInstance = new TcgaWorkflowDeciderService();
		}

		return tcgaWorkflowDeciderServiceInstance;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ConfigHelper configHelper = ConfigHelper.createConfig(
				"TcgaWorkflowActivityService", args);

		// Create the client for Simple Workflow Service
		swfService = configHelper.createSWFClient();

		// Start Decider Executor Service
		getTcgaWorkflowDeciderServiceInstance().startDecider();

		// Add a Shutdown hook to close DeciderExecutorService
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					getTcgaWorkflowDeciderServiceInstance().stopDecider();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		System.out.println("Please press any key to terminate service.");
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.exit(0);
	}

	/**
	 * @throws Exception
	 */
	public void startDecider() throws Exception {
		System.out.println("Starting Tcga Workflow Decider Service...");

		// Register Tcga workflow
		deciderExecutor = new AsynchronyExecutorService(swfService);
		deciderExecutor
				.addWorkflowsFromPackage(TcgaWorkflowDeciderService.class
						.getPackage().getName());

		// Start DeciderExecutor Service
		deciderExecutor.start();

		System.out.println("Tcga Workflow Decider Service Started...");
	}

	/**
	 * @throws InterruptedException
	 */
	public void stopDecider() throws InterruptedException {
		System.out.println("Stopping Tcga Workflow Decider Service...");
		deciderExecutor.shutdownNow();
		swfService.shutdown();
		deciderExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		System.out.println("Tcga Workflow Decider Service Stopped...");
	}

}
