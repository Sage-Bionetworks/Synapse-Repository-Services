package org.sagebionetworks.workflow;

import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.workflow.Constants;

import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;

/**
 * @author deflaux
 * 
 */
public class SageCommonsWorkflowImpl implements SageCommonsWorkflow {

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
				throw e;
			}

			@Override
			protected void doFinally() throws Throwable {
				// do nothing
			}
		};
	}
	
	@Asynchronous
	private void processLayerSubmission(Promise<Layer> layerPromise) {
		Layer layer = layerPromise.get();
		Promise<Integer> numJobsDispatched = client.processSpreadsheet(layer.getLocations().get(0).getPath());
		Promise<String> message = client.formulateNotificationMessage(Promise.asPromise(layer), numJobsDispatched);
		client.notifyFollowers(Promise
		.asPromise(layer.getCreatedBy()), Promise
		.asPromise(NOTIFICATION_SUBJECT), message);		
	}
}
