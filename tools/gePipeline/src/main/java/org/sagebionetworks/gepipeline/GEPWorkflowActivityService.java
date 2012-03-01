package org.sagebionetworks.gepipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.ActivityWorker;

/**
 * This is the process which hosts all the SWF activities exposed in this
 * package.
 * 
 * Note that some of the activities in this package depend upon R and
 * Bioconductor being properly installed on the machine and available in the
 * PATH.
 */
public class GEPWorkflowActivityService {

	private static final String CAPABILITY_PROPERTY_NAME = "org.sagebionetworks.gepipeline.capability";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		// TODO confirm that R and Bioconductor are available before proceeding.
		// Return a helpful error message to the user if the environment is not
		// sufficient for this service to run.

		// Enable routing to specific hosts, based on capabilities
		// Note, we do NOT want to put this property into a properties file,
		// which may be used by multiple
		// hosts sharing a common file system. Rather we pass in as a
		// command-line property
		// the property contains multiple capabilities, separated by comma,
		// colon, or semi-colon
		String capabilitiesString = System
				.getProperty(CAPABILITY_PROPERTY_NAME);
		List<String> capabilities = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(capabilitiesString, ",:;");
		while (st.hasMoreTokens()) {
			capabilities.add(st.nextToken());
		}

		AmazonSimpleWorkflow swfService = GEPWorkflowConfigHelper
				.getSWFClient();
		String domain = GEPWorkflowConfigHelper.getStack();

		for (String activityRequirement : capabilities) {
			String taskList;
			if (GEPWorkflow.ACTIVITY_REQUIREMENT_SMALL.equals(activityRequirement)) {
				taskList = GEPWorkflow.SMALL_ACTIVITY_TASK_LIST;
			} else if (GEPWorkflow.ACTIVITY_REQUIREMENT_MEDIUM.equals(activityRequirement)) {
				taskList = GEPWorkflow.MEDIUM_ACTIVITY_TASK_LIST;
			} else if (GEPWorkflow.ACTIVITY_REQUIREMENT_LARGE.equals(activityRequirement)) {
				taskList = GEPWorkflow.LARGE_ACTIVITY_TASK_LIST;
			} else if (GEPWorkflow.ACTIVITY_REQUIREMENT_EXTRA_LARGE.equals(activityRequirement)) {
				taskList = GEPWorkflow.EXTRA_LARGE_ACTIVITY_TASK_LIST;
			} else {
				throw new IllegalArgumentException("Unexpected "
						+ activityRequirement);
			}
			
			final ActivityWorker worker = new ActivityWorker(swfService,
					domain, taskList);

			// Create activity implementations
			GEPActivities activities = new GEPActivitiesImpl();
			worker.addActivitiesImplementation(activities);

			worker.start();

			System.out.println("Activity Worker Started for Task List: "
					+ worker.getTaskListToPoll());

			Runtime.getRuntime().addShutdownHook(new Thread() {

				@Override
				public void run() {
					try {
						worker
								.shutdownAndAwaitTermination(10,
										TimeUnit.MINUTES);
						System.out.println("Activity Worker Exited.");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
		}
		System.out.println("Please press any key to terminate service.");

		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}
