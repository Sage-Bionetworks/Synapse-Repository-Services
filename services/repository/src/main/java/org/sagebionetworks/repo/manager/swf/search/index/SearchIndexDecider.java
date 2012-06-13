package org.sagebionetworks.repo.manager.swf.search.index;

import java.util.List;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.swf.Decider;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.PollForDecisionTaskRequest;
import com.amazonaws.services.simpleworkflow.model.TaskList;

/**
 * Decides how to proceed with the Creation and destruction of a search index.
 * 
 * @author John
 *
 */
public class SearchIndexDecider implements Decider {
	
	public static String VERSION = "1.0";

	@Override
	public TaskList getTaskList() {
		return new TaskList().withName(SearchIndexDecider.class.getName()+"-"+VERSION);
	}

	@Override
	public String getDomainName() {
		// This works at the base stack level
		return StackConfiguration.getStack();
	}

	@Override
	public void makeDecision(DecisionTask dt, PollForDecisionTaskRequest pfdtr,
			AmazonSimpleWorkflowClient simpleWorkFlowClient) {
		
		// Get the event history
		List<HistoryEvent> history = dt.getEvents();
		// Get all history for this work flow
//		while(dt.getNextPageToken() != null){
//			// This will get the next token
//			pfdtr.setNextPageToken(dt.getNextPageToken());
//			dt = simpleWorkFlowClient.pollForDecisionTask(pfdtr);
//			// Append this to the local list
//			history.addAll(dt.getEvents());
//			// Play nice with other threads.
//			Thread.yield();
//		}
		// We should now have the full history
		
		// Look at the first event
		HistoryEvent event = history.get(0);

		
	}

}
