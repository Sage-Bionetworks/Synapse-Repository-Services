package org.sagebionetworks.repo.manager.swf;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.PollForDecisionTaskRequest;

/**
 * An abstraction for a decider.
 * 
 * Note: Implementations must provide a default constructor, as reflection
 * is used to create a new instance of a decider when it is time to make a decision.
 * A single instance of a decider must be stateless.
 * 
 * @author John
 *
 */
public interface Decider extends Task {
	
	/**
	 * This method is called when there is a decision that the decider must make.
	 * Note: Only the first page of DecisionTask is passed. If there are multiple pages then it is up to the decider to
	 * fetch the rest of the pages by calling AmazonSimpleWorkflowClient.pollForDecisionTask();
	 * @param dt - The results from the last poll.
	 * @param pfdtr - The request used to generate the DecisionTask parameter.
	 * @param simpleWorkFlowClient - The AmazonSimpleWorkflowClient need to get more pages, make decisions, and close workflows.
	 */
	public void makeDecision(DecisionTask dt, PollForDecisionTaskRequest pfdtr,	AmazonSimpleWorkflowClient simpleWorkFlowClient);

}
