package org.sagebionetworks.workflow;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;

import org.apache.log4j.Logger;
import org.sagebionetworks.repo.model.Layer;

import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;

/**
 * @author deflaux
 * 
 */
public class SageCommonsWorkflowImpl implements SageCommonsWorkflow {

	private static final Logger log = Logger.getLogger(SageCommonsWorkflow.class
			.getName());
	private static final String NOTIFICATION_SUBJECT = "Sage Commons Workflow Notification ";

	private SageCommonsActivitiesClient client;

	/**
	 * Default constructor
	 */
	public SageCommonsWorkflowImpl() {
		client = new SageCommonsActivitiesClientImpl();
	}

	/**
	 * Constructor for unit testing or if we are using Spring to wire this up
	 * 
	 * @param client
	 */
	public SageCommonsWorkflowImpl(SageCommonsActivitiesClient client) {
		this.client = client;
	}

	@Override
	public void processSubmission(final String submission) {
		
		new TryCatchFinally() {

			@Override
			protected void doTry() throws Throwable {
				
				if(submission.startsWith("http")) {
					client.processSpreadsheet(submission);
				}
				else {
					Promise<Layer> layer = client.getLayer(submission);
					processLayerSubmission(layer);

				}
			}

			@Override
			protected void doCatch(Throwable e) throws Throwable {
				log.error("processSubmission failed: ", e);
				throw e;
			}

			@Override
			protected void doFinally() throws Throwable {
				// nothing to clean up
				log.info("workflow complete");
			}
		};
	}
	
	@Asynchronous
	private void processLayerSubmission(Promise<Layer> layerPromise) {
		Layer layer = layerPromise.get();
		Promise<List<String>> jobs = client.processSpreadsheet(layer.getLocations().get(0).getPath());
		kickOffJobs(layer, jobs);
	}
	
	@Asynchronous
	private void kickOffJobs(Layer layer, Promise<List<String>> jobs) {
		int numJobs = 0;
		// Get a factory for these child workflows
		SageCommonsRScriptWorkflowClientFactory clientFactory = new SageCommonsRScriptWorkflowClientFactoryImpl();

		for(String job : jobs.get()) {
			SageCommonsRScriptWorkflowClient childWorkflow = clientFactory
					.getClient();
			childWorkflow.runRScript(SageCommonsConfigHelper.getWorkflowScript(), job);
			numJobs++;
			
			WorkflowExecution workflowExecution = childWorkflow.getWorkflowExecution();
			log.debug("Started runRScript workflow with workflowId=\""
					+ workflowExecution.getWorkflowId() + "\" and runId=\""
					+ workflowExecution.getRunId() + "\"");
		}		
		Promise<String> message = client.formulateNotificationMessage(Promise.asPromise(layer), Promise.asPromise(numJobs));
		client.notifyFollowers(Promise
		.asPromise(layer.getCreatedBy()), Promise
		.asPromise(NOTIFICATION_SUBJECT), message);		
	}
}
