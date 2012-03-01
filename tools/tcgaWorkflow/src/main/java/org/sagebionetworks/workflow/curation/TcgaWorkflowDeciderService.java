package org.sagebionetworks.workflow.curation;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.WorkflowWorker;

/**
 * @author deflaux
 * 
 */
public class TcgaWorkflowDeciderService {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		AmazonSimpleWorkflow swfService = TcgaWorkflowConfigHelper.getSWFClient();
		String domain = TcgaWorkflowConfigHelper.getStack();

		final WorkflowWorker worker = new WorkflowWorker(swfService, domain,
				TcgaWorkflow.DECISIONS_TASK_LIST);
		worker.addWorkflowImplementationType(TcgaWorkflowImpl.class);
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
