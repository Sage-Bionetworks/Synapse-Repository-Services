package org.sagebionetworks.workflow;

import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;

/**
 * @author deflaux
 * 
 */
public class SageCommonsRScriptWorkflowImpl implements SageCommonsRScriptWorkflow {

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
				// TODO notify someone?
				throw e;
			}

			@Override
			protected void doFinally() throws Throwable {
				// do nothing
			}
		};
	}
}
