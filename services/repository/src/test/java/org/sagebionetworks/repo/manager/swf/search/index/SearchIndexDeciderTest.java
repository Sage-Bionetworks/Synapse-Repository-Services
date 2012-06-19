package org.sagebionetworks.repo.manager.swf.search.index;


import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;

import static org.sagebionetworks.repo.manager.swf.SwfTaskUtils.*;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.PollForDecisionTaskRequest;
import com.amazonaws.services.simpleworkflow.model.RespondDecisionTaskCompletedRequest;
import com.amazonaws.services.simpleworkflow.model.TerminateWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;

/**
 * 
 * @author jmhill
 *
 */
public class SearchIndexDeciderTest {
	
	private AmazonSimpleWorkflowClient mockClient;
	private SearchIndexDecider decider;
	private PollForDecisionTaskRequest pfdtr;
	private DecisionTask dt;
	
	@Before
	public void before(){
		mockClient = Mockito.mock(AmazonSimpleWorkflowClient.class);
		// This is a unit test so the decider should not be created by spring.
		decider = new SearchIndexDecider();
		pfdtr = new PollForDecisionTaskRequest().withDomain("UnitTestDomain");
		dt = new DecisionTask();
		dt.setWorkflowExecution(new WorkflowExecution());
	}
	
	@Test
	public void testFailedDecision(){
		// Adding a work flow started to the history is not enough. This will
		// trigger the decider to throw an exception which should then trigger
		// a termination decision.
		List<HistoryEvent> history = new ArrayList<HistoryEvent>();
		history.add(new HistoryEvent().withEventType(WORKFLOW_EXECUTION_STARTED));
		dt.setEvents(history);
		// make the failed called
		AmazonWebServiceRequest request = decider.makeDecision(dt, pfdtr, mockClient);
		assertNotNull(request);
		assertTrue(request instanceof TerminateWorkflowExecutionRequest);
		TerminateWorkflowExecutionRequest termRequest = (TerminateWorkflowExecutionRequest) request;
		assertNotNull(termRequest.getDetails());
		assertNotNull(termRequest.getReason());
		assertEquals(pfdtr.getDomain(), termRequest.getDomain());
		System.out.println(termRequest);
		// Was it passed to the client?
		verify(mockClient).terminateWorkflowExecution(termRequest);
		
	}
	
	@Test
	public void testCreateIndex(){
		// When the work flow starts the first thing we do is create a search index.
		// Set the history with the starting state.
		List<HistoryEvent> history = new ArrayList<HistoryEvent>();
		history.add(new HistoryEvent().withEventType(WORKFLOW_EXECUTION_STARTED));
		history.add(new HistoryEvent().withEventType(DECISION_TASK_SCHEDULED));
		history.add(new HistoryEvent().withEventType(DECISION_TASK_STARTED));
		dt.setEvents(history);
		// This call should trigger the creating of a start index decision
		AmazonWebServiceRequest request = decider.makeDecision(dt, pfdtr, mockClient);
		assertNotNull(request);
		assertTrue(request instanceof RespondDecisionTaskCompletedRequest);
		RespondDecisionTaskCompletedRequest activityRequest = (RespondDecisionTaskCompletedRequest) request;
		assertNotNull(activityRequest.getDecisions());
		assertEquals(1, activityRequest.getDecisions().size());
	
	}

}
