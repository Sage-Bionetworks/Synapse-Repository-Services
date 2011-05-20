// Copyright [2011]-[2011] Amazon.com, Inc. or its affiliates. All Rights Reserved. 
// Licensed under the Amazon Web Services Customer Agreement (the “License”). 
// You may not use this file except in compliance with the License. A copy of the License is located at
//     http://aws.amazon.com/agreement/
// or in the “license” file accompanying this file. 
// This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
// either express or implied. See the License for the specific language governing permissions and limitations under the License.
package org.sagebionetworks.workflow.curation;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.sagebionetworks.workflow.curation.ConfigHelper;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.AsynchronyExecutorService;

/**
 * This is the process which hosts all the SWF activities exposed in this
 * package.
 * 
 * Note that some of the activities in this package depend upon R and
 * Bioconductor being properly installed on the machine and available in the
 * PATH.
 */
public class TcgaWorkflowActivityService {

	private static AmazonSimpleWorkflow swfService;
	private static TcgaWorkflowActivityService tcgaWorkflowActivityServiceInstance;
	private static AsynchronyExecutorService activityExecutor;

	/**
	 * @return the activity service instance
	 */
	public synchronized static TcgaWorkflowActivityService getTcgaWorkflowActivityServiceInstance() {
		if (tcgaWorkflowActivityServiceInstance == null) {
			tcgaWorkflowActivityServiceInstance = new TcgaWorkflowActivityService();
		}

		return tcgaWorkflowActivityServiceInstance;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		// TODO confirm that R and Bioconductor are available before proceeding.
		// Return a helpful error message to the user if the environment is not
		// sufficient for this service to run.

		ConfigHelper configHelper = ConfigHelper.createConfig(
				"TcgaWorkflowActivityService", args);

		// Create the client for Simple Workflow Service
		swfService = configHelper.createSWFClient();

		// Start Activity Executor Service
		getTcgaWorkflowActivityServiceInstance()
				.startTcgaWorkflowActivityService();

		// Add a Shutdown hook to close ActivityExecutorService
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			public void run() {
				try {
					getTcgaWorkflowActivityServiceInstance()
							.stopTcgaWorkflowActivityService();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}));

		System.out.println("Please press any key to terminate service.");
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.exit(0);
	}

	private void startTcgaWorkflowActivityService() throws Exception {
		System.out.println("Starting Agent Service...");

		activityExecutor = new AsynchronyExecutorService(swfService);
		activityExecutor
				.addActivitiesFromPackage(TcgaWorkflowActivityService.class
						.getPackage().getName());

		// Start ActivityExecutor Service
		activityExecutor.start();

		System.out.println("Agent Service Started...");
	}

	private void stopTcgaWorkflowActivityService() throws InterruptedException {
		System.out.println("Stopping Agent Service...");
		activityExecutor.shutdownNow();
		swfService.shutdown();
		activityExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		System.out.println("Agent Service Stopped...");
	}

}
