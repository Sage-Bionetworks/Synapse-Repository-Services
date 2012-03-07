/*
 * Copyright 2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.services.simpleworkflow.flow.examples.helloworld;

import org.sagebionetworks.workflow.WorkflowTemplatedConfiguration;
import org.sagebionetworks.workflow.WorkflowTemplatedConfigurationImpl;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;

/**
 * @author deflaux
 *
 */
public class WorkflowExecutionStarter {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		WorkflowTemplatedConfiguration config = new WorkflowTemplatedConfigurationImpl();
		config.reloadConfiguration();
		AmazonSimpleWorkflow swfService = config.getSWFClient();
		String domain = config.getStack();

		HelloWorldWorkflowClientExternalFactory clientFactory = new HelloWorldWorkflowClientExternalFactoryImpl(
				swfService, domain);
		HelloWorldWorkflowClientExternal workflow = clientFactory.getClient();

		// Start Wrokflow Execution
		workflow.helloWorld("SanityCheckUser");

		// WorkflowExecution is available after workflow creation
		WorkflowExecution workflowExecution = workflow.getWorkflowExecution();
		System.out.println("Started helloWorld workflow with workflowId=\""
				+ workflowExecution.getWorkflowId() + "\" and runId=\""
				+ workflowExecution.getRunId() + "\"");
	}

}
