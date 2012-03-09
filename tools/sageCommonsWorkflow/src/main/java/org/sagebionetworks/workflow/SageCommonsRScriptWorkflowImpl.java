package org.sagebionetworks.workflow;

import org.apache.log4j.Logger;

import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;

/**
 * @author deflaux
 * 
 */
public class SageCommonsRScriptWorkflowImpl implements
		SageCommonsRScriptWorkflow {

	private static final Logger log = Logger
			.getLogger(SageCommonsRScriptWorkflow.class.getName());

	private SageCommonsActivitiesClient client;

	/**
	 * Default constructor
	 */
	public SageCommonsRScriptWorkflowImpl() {
		client = new SageCommonsActivitiesClientImpl();
	}

	/**
	 * Constructor for unit testing or if we are using Spring to wire this up
	 * 
	 * @param client
	 */
	public SageCommonsRScriptWorkflowImpl(SageCommonsActivitiesClient client) {
		this.client = client;
	}

	@Override
	public void runRScript(final String script, final String spreadsheetData) {

		new TryCatchFinally() {

			@Override
			protected void doTry() throws Throwable {
				client.runRScript(script, spreadsheetData);
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
}
