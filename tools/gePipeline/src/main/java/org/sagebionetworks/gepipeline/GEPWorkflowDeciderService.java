package org.sagebionetworks.gepipeline;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.WorkflowWorker;

/**
 * This is the process which hosts all the SWF deciders exposed in this package.
 * 
 */
public class GEPWorkflowDeciderService {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		AmazonSimpleWorkflow swfService = GEPWorkflowConfigHelper
				.getSWFClient();
		String domain = GEPWorkflowConfigHelper.getStack();

		final WorkflowWorker worker = new WorkflowWorker(swfService, domain,
				GEPWorkflow.DECISIONS_TASK_LIST);
		worker.addWorkflowImplementationType(GEPWorkflowImpl.class);
		worker.start();

		System.out.println("Workflow Host Service Started...");

		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				try {
					worker.shutdownAndAwaitTermination(10, TimeUnit.MINUTES);
					System.out.println("Workflow Host Service Terminated...");
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

}
