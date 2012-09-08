package org.sagebionetworks.asynchronous.workers.swf.search.index;

import static org.sagebionetworks.asynchronous.workers.swf.SwfTaskUtils.WORKFLOW_EXECUTION_STARTED;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.swf.Decider;
import org.sagebionetworks.asynchronous.workers.swf.SwfTaskUtils;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.PollForDecisionTaskRequest;
import com.amazonaws.services.simpleworkflow.model.RespondDecisionTaskCompletedRequest;
import com.amazonaws.services.simpleworkflow.model.ScheduleActivityTaskDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.TaskList;
import com.amazonaws.services.simpleworkflow.model.TerminateWorkflowExecutionRequest;

/**
 * Decides how to proceed with the Creation and destruction of a search index.
 * 
 * @author John
 *
 */
public class SearchIndexDecider implements Decider {
	
	
	static private Log log = LogFactory.getLog(SearchIndexDecider.class);
	
	@Override
	public TaskList getTaskList() {
		return SearchIndexWorkFlow.DECIDER_TASK_LIST;
	}

	@Override
	public String getDomainName() {
		// This works at the base stack level
		return StackConfiguration.getStack();
	}

	@Override
	public AmazonWebServiceRequest makeDecision(DecisionTask dt, PollForDecisionTaskRequest pfdtr,
			AmazonSimpleWorkflowClient simpleWorkFlowClient) {
		try{
			// Get the event history
			HistoryEvent lastRealEvent = SwfTaskUtils.getLastNonDecisionType(dt.getEvents());
			log.info("Last real event: "+lastRealEvent);
			
			// Here is the work flow switch
			if(WORKFLOW_EXECUTION_STARTED.equals(lastRealEvent.getEventType())){
				// Schedule the activity that will create the search index.
				RespondDecisionTaskCompletedRequest request = new RespondDecisionTaskCompletedRequest();
				request.setTaskToken(dt.getTaskToken());
				List<Decision> list = new LinkedList<Decision>();
				Decision decision = new Decision();
				decision.setDecisionType(DecisionType.ScheduleActivityTask);
				decision.setScheduleActivityTaskDecisionAttributes(
						new ScheduleActivityTaskDecisionAttributes().
						withActivityType(new CreateSearchIndexActivity().getActivityType()).
						withActivityId(pfdtr.getDomain()+"-"+pfdtr.getIdentity()));
				list.add(decision);
				request.setDecisions(list);
				simpleWorkFlowClient.respondDecisionTaskCompleted(request);
				return request;
			}else{
				// This is an unknown event
				throw new IllegalStateException("Unknown event type: "+lastRealEvent);
			}
						
		}catch (Throwable e){
			// If anything goes wrong we need to kill this work flow.
			TerminateWorkflowExecutionRequest termRequest = SwfTaskUtils.createTerminateRequest(e, dt.getWorkflowExecution(), pfdtr.getDomain());
			simpleWorkFlowClient.terminateWorkflowExecution(termRequest);
			return termRequest;
		}			
	}
	

	

}
