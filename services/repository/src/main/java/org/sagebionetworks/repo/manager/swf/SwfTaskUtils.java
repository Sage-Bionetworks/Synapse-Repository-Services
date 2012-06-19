package org.sagebionetworks.repo.manager.swf;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.simpleworkflow.model.ActivityType;
import com.amazonaws.services.simpleworkflow.model.CompleteWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.RespondDecisionTaskCompletedRequest;
import com.amazonaws.services.simpleworkflow.model.ScheduleActivityTaskDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.TerminateWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;

/**
 * Utilities to help with AWS Simple Work Flow API objects.
 * 
 * @author jmhill
 *
 */
public class SwfTaskUtils {
	
	public static final String DECISION_TASK_SCHEDULED = "DecisionTaskScheduled";
	public static final String DECISION_TASK_STARTED = "DecisionTaskStarted";
	public static final String WORKFLOW_EXECUTION_STARTED = "WorkflowExecutionStarted";
	
	/**
	 * When we need to make a decision, the last two events on the history list are "DecisionTaskStarted"
	 * and  "DecisionTaskScheduled".  While we expect these to be there, it is the event before these
	 * two that tell us what actually triggered this decision.
	 * 
	 * @param history - The full history passed to the decider.
	 * @return
	 */
	public static HistoryEvent getLastNonDecisionType(List<HistoryEvent> history){
		if(history == null) throw new IllegalArgumentException("History cannot be null");
		if(history.size() < 3) throw new IllegalStateException("Expected at least three history events");
		HistoryEvent event = history.get(history.size()-1);
		if(!DECISION_TASK_STARTED.equals(event.getEventType())) throw createUnexpectedType("n-1", DECISION_TASK_STARTED, event.getEventType());
		event = history.get(history.size()-2);
		if(!DECISION_TASK_SCHEDULED.equals(event.getEventType())) throw createUnexpectedType("n-2", DECISION_TASK_SCHEDULED, event.getEventType());
		return history.get(history.size()-3);
	}
	
	/**
	 * Helper to build up an IllegalStateException for an unexpected type.
	 * 
	 * @param nth
	 * @param expected
	 * @param actual
	 * @return
	 */
	public static IllegalStateException createUnexpectedType(String nth, String expected, String actual){
		return new IllegalStateException("Unexpected HistoryEvent.  Expected the "+nth+" HistoryEvent to be of type: "+expected+" but was"+actual);
	}
	
	/**
	 * Build a RespondDecisionTaskCompletedRequest with a given message.
	 * 
	 * @param dt - The DecisionTask passed to the worker.
	 * @param resultMessage - This string will be used for the Decision results.
	 * @return
	 */
	public static RespondDecisionTaskCompletedRequest buildDecisionCompleteTask(DecisionTask dt, String resultMessage) {
		if(dt == null) throw new IllegalArgumentException("DecisionTask cannot be null");
		if(resultMessage == null) throw new IllegalArgumentException("Message cannot be null");
		RespondDecisionTaskCompletedRequest closeRequest = new RespondDecisionTaskCompletedRequest();
		Decision decision = new Decision();
		decision.setDecisionType(DecisionType.CompleteWorkflowExecution);
		CompleteWorkflowExecutionDecisionAttributes atts = new CompleteWorkflowExecutionDecisionAttributes();
		atts.setResult(resultMessage);
		decision.setCompleteWorkflowExecutionDecisionAttributes(atts);
		List<Decision> decisionList = new ArrayList<Decision>();
		decisionList.add(decision);
		closeRequest.setDecisions(decisionList);
		closeRequest.setTaskToken(dt.getTaskToken());
		return closeRequest;
	}
	
	public static RespondDecisionTaskCompletedRequest buildDecisionCompleteTaskScheduleActivityTask(DecisionTask dt, ActivityType type, String activityId) {
		if(dt == null) throw new IllegalArgumentException("DecisionTask cannot be null");
		// The decision
		Decision decision = new Decision();
		decision.setDecisionType(DecisionType.ScheduleActivityTask);
		ScheduleActivityTaskDecisionAttributes att = new ScheduleActivityTaskDecisionAttributes();
		att.setActivityId(activityId);
		att.setActivityType(type);
		decision.setScheduleActivityTaskDecisionAttributes(att);
		List<Decision> decisionList = new ArrayList<Decision>();
		decisionList.add(decision);
		// The request
		RespondDecisionTaskCompletedRequest closeRequest = new RespondDecisionTaskCompletedRequest();
		closeRequest.setDecisions(decisionList);
		closeRequest.setTaskToken(dt.getTaskToken());
		return closeRequest;
	}
	
	
	/**
	 * Build a TerminateWorkflowExecutionRequest for an Exception.
	 * 
	 * @param reason - The Exception thrown during execution.
	 * @param we - WorkflowExecution passed to the worker.
	 * @param domain - The name of the domain this work flow was running on.
	 * @return
	 */
	public static TerminateWorkflowExecutionRequest createTerminateRequest(Throwable reason, WorkflowExecution we, String domain){
		if(reason == null) throw new IllegalArgumentException("Reason cannot be null");
		if(we == null) throw new IllegalArgumentException("WorkflowExecution cannot be null");
		if(domain == null) throw new IllegalArgumentException("Domain cannot be null");
		TerminateWorkflowExecutionRequest termRequest = new TerminateWorkflowExecutionRequest();
		termRequest.setDomain(domain);
		termRequest.setRunId(we.getRunId());
		termRequest.setWorkflowId(we.getWorkflowId());
		termRequest.setReason("Exception thrown during execution");
		StringWriter writer = new StringWriter();
		reason.printStackTrace(new PrintWriter(writer));
		termRequest.setDetails(writer.toString());
		return termRequest;
	}

}
