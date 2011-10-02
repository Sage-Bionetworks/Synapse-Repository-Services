package org.sagebionetworks.gepipeline;

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
public class GEPWorkflowDeciderService {

	private static AmazonSimpleWorkflow swfService;
	private static GEPWorkflowDeciderService gepWorkflowDeciderServiceInstance;
	private static AsynchronyExecutorService deciderExecutor;

	/**
	 * @return the decider service instance
	 */
	public synchronized static GEPWorkflowDeciderService getGEPWorkflowDeciderServiceInstance() {
		if (gepWorkflowDeciderServiceInstance == null) {
			gepWorkflowDeciderServiceInstance = new GEPWorkflowDeciderService();
		}

		return gepWorkflowDeciderServiceInstance;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// Create the client for Simple Workflow Service
		swfService = ConfigHelper.createSWFClient();

		// Start Decider Executor Service
		getGEPWorkflowDeciderServiceInstance().startDecider();

		// Add a Shutdown hook to close DeciderExecutorService
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					getGEPWorkflowDeciderServiceInstance().stopDecider();
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
		System.out.println("Starting GEP Workflow Decider Service...");

		// Register GEP workflow
		deciderExecutor = new AsynchronyExecutorService(swfService);
		deciderExecutor
				.addWorkflowsFromPackage(GEPWorkflowDeciderService.class
						.getPackage().getName());

		// Start DeciderExecutor Service
		deciderExecutor.start();

		System.out.println("GEP Workflow Decider Service Started...");
	}

	/**
	 * @throws InterruptedException
	 */
	public void stopDecider() throws InterruptedException {
		System.out.println("Stopping GEP Workflow Decider Service...");
		deciderExecutor.shutdownNow();
		swfService.shutdown();
		deciderExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		System.out.println("GEP Workflow Decider Service Stopped...");
	}

}
