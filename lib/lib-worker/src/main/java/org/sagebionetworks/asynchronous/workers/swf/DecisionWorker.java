package org.sagebionetworks.asynchronous.workers.swf;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.PollForDecisionTaskRequest;

/**
 * A worker for a decision.
 * 
 * @author John
 *
 */
public class DecisionWorker implements Runnable {
	
	Decider decider;
	DecisionTask decisionTask;
	PollForDecisionTaskRequest decisionTaskRequest;
	AmazonSimpleWorkflowClient simpleWorkFlowClient;

	/**
	 * Create a new worker
	 * @param instance
	 * @param dt
	 * @param pfdtr
	 * @param simpleWorkFlowClient
	 */
	public DecisionWorker(Decider instance, DecisionTask dt, PollForDecisionTaskRequest pfdtr,
			AmazonSimpleWorkflowClient simpleWorkFlowClient) {
		super();
		if(instance == null) throw new IllegalArgumentException("Instance cannot be null");
		if(dt == null) throw new IllegalArgumentException("DecisionTask cannot be null");
		if(pfdtr == null) throw new IllegalArgumentException("PollForDecisionTaskRequest cannot be null");
		if(simpleWorkFlowClient == null) throw new IllegalArgumentException("AmazonSimpleWorkflowClient cannot be nul");
		this.decider = instance;
		this.decisionTask = dt;
		this.decisionTaskRequest = pfdtr;
		this.simpleWorkFlowClient = simpleWorkFlowClient;
	}

	@Override
	public void run() {
		// Call the decide methods
		decider.makeDecision(decisionTask, decisionTaskRequest, simpleWorkFlowClient);

	}

}
