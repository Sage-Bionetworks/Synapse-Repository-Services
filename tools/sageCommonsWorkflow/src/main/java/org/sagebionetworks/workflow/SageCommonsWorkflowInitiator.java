package org.sagebionetworks.workflow;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;

/**
 * @author deflaux
 *
 */
public class SageCommonsWorkflowInitiator {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// Create the client for Simple Workflow Service
		AmazonSimpleWorkflow swfService = SageCommonsConfigHelper.getSWFClient();
		String domain = SageCommonsConfigHelper.getStack();

		SageCommonsWorkflowClientExternalFactory clientFactory = new SageCommonsWorkflowClientExternalFactoryImpl(
				swfService, domain);

		SageCommonsWorkflowClientExternal workflow = clientFactory
		.getClient();
		workflow.processSubmission(args[0]);
		System.out.println("Kicked off workflow for " + args[0]);

		System.exit(0);
	}

}
