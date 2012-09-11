package org.sagebionetworks.asynchronous.workers.swf;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.sagebionetworks.asynchronous.workers.swf.SwfTaskUtils.*;

import org.junit.Test;
import org.sagebionetworks.asynchronous.workers.swf.SwfTaskUtils;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.RespondDecisionTaskCompletedRequest;
import com.amazonaws.services.simpleworkflow.model.TerminateWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;
/**
 * Test for SwfTaskUtils.
 * @author jmhill
 *
 */
public class SwfTaskUtilsTest {
	

	@Test (expected=IllegalArgumentException.class)
	public void testGetLastNonDecisionTypeNull(){
		SwfTaskUtils.getLastNonDecisionType(null);
	}

	@Test (expected=IllegalStateException.class)
	public void testGetLastNonDecisionTypeEmpty(){
		List<HistoryEvent> list = new LinkedList<HistoryEvent>();
		SwfTaskUtils.getLastNonDecisionType(list);
	}
	
	@Test (expected=IllegalStateException.class)
	public void testGetLastNonDecisionTypeTooSmall(){
		List<HistoryEvent> list = new LinkedList<HistoryEvent>();
		list.add(new HistoryEvent().withEventType(DECISION_TASK_STARTED));
		SwfTaskUtils.getLastNonDecisionType(list);
	}
	
	@Test (expected=IllegalStateException.class)
	public void testGetLastNonDecisionTypeStillTooSmall(){
		List<HistoryEvent> list = new LinkedList<HistoryEvent>();
		list.add(new HistoryEvent().withEventType(DECISION_TASK_SCHEDULED));
		list.add(new HistoryEvent().withEventType(DECISION_TASK_STARTED));
		SwfTaskUtils.getLastNonDecisionType(list);
	}
	
	@Test
	public void testGetLastNonDecisionType(){
		List<HistoryEvent> list = new LinkedList<HistoryEvent>();
		list.add(new HistoryEvent().withEventType(WORKFLOW_EXECUTION_STARTED));
		list.add(new HistoryEvent().withEventType(DECISION_TASK_SCHEDULED));
		list.add(new HistoryEvent().withEventType(DECISION_TASK_STARTED));

		HistoryEvent event = SwfTaskUtils.getLastNonDecisionType(list);
		assertNotNull(event);
		assertEquals(WORKFLOW_EXECUTION_STARTED, event.getEventType());
		
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBuildDecisionCompleteTaskDTNull(){
		SwfTaskUtils.buildDecisionCompleteTask(null, "not null");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBuildDecisionCompleteTaskMessageNull(){
		DecisionTask dt = new DecisionTask();
		SwfTaskUtils.buildDecisionCompleteTask(dt, null);
	}
	
	@Test
	public void testBuildDecisionCompleteTask(){
		DecisionTask dt = new DecisionTask().withTaskToken("task token");
		String message = "message string";
		RespondDecisionTaskCompletedRequest request = SwfTaskUtils.buildDecisionCompleteTask(dt, message);
		assertNotNull(request);
		assertEquals(dt.getTaskToken(), request.getTaskToken());
		assertNotNull(request.getDecisions());
		assertEquals(1, request.getDecisions().size());
		Decision decision = request.getDecisions().get(0);
		assertNotNull(decision);
		assertEquals(DecisionType.CompleteWorkflowExecution.toString(), decision.getDecisionType());
		assertNotNull(decision.getCompleteWorkflowExecutionDecisionAttributes());
		assertEquals(message, decision.getCompleteWorkflowExecutionDecisionAttributes().getResult());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateTerminateRequestNullThrowable(){
		SwfTaskUtils.createTerminateRequest(null, new WorkflowExecution(), "domain");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateTerminateRequestNullWorkflowExecution(){
		SwfTaskUtils.createTerminateRequest(new Throwable(), null, "domain");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateTerminateRequestNullDomain(){
		SwfTaskUtils.createTerminateRequest(new Throwable(), new WorkflowExecution(), null);
	}
	
	public void testCreateTerminateRequest(){
		Throwable reason = new Throwable("test message");
		WorkflowExecution we = new WorkflowExecution().withRunId("runId").withWorkflowId("workId");
		String domain = "domain name";
		TerminateWorkflowExecutionRequest request = SwfTaskUtils.createTerminateRequest(reason, new WorkflowExecution(), domain);
		assertNotNull(request);
		assertEquals(domain, request.getDomain());
		assertEquals(we.getRunId(), request.getRunId());
		assertEquals(we.getWorkflowId(), request.getWorkflowId());
		assertNotNull(request.getReason());
		assertNotNull(request.getDetails());	
	}

}
