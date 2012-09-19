package org.sagebionetworks.asynchronous.workers.swf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.ActivityTask;
import com.amazonaws.services.simpleworkflow.model.ActivityType;
import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.PollForActivityTaskRequest;
import com.amazonaws.services.simpleworkflow.model.PollForDecisionTaskRequest;
import com.amazonaws.services.simpleworkflow.model.RegisterActivityTypeRequest;
import com.amazonaws.services.simpleworkflow.model.TaskList;

/**
 * The unit test for SimpleWorkFlowWatcher.s
 * 
 * @author John
 *
 */
public class SimpleWorkFlowWatcherTest {
	
	AmazonSimpleWorkflowClient mockSWFClient;
	SimpleWorkFlowRegister mockRegister;	
	ExecutorService mockExecutor;
	SimpleWorkFlowWatcher watcher;
	
	@Before
	public void before(){
		mockSWFClient = Mockito.mock(AmazonSimpleWorkflowClient.class);
		mockRegister = Mockito.mock(SimpleWorkFlowRegister.class);
		mockExecutor = Mockito.mock(ExecutorService.class);
		// Create the stubbed watcher.
		watcher = new SimpleWorkFlowWatcher(mockSWFClient, mockRegister, mockExecutor);
		
	}

	@Test
	public void testGetAddressProcessIdThreadId(){
		String key = SimpleWorkFlowWatcher.getAddressProcessIdThreadId();
		assertNotNull(key);
		System.out.println(key);
	}
	
	/**
	 * When there is no token returned then we should not call the activity.
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	@Test
	public void testPollForActivityNullToken() throws InstantiationException, IllegalAccessException{
		StubActivity stubActivity = new StubActivity();
		// For this case we have a null token, and therefore should not create an activity.
		when(mockSWFClient.pollForActivityTask(any(PollForActivityTaskRequest.class))).thenReturn(new ActivityTask().withTaskToken(null));
		ActivityWorker worker = watcher.pollForActivity(stubActivity);
		// this case should return a null activity.
		assertEquals(null, worker);
	}
	
	/**
	 * When there is a token returned then we should call the activity.
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	@Test
	public void testPollForActivityNonNullToken() throws InstantiationException, IllegalAccessException{
		StubActivity stubActivity = new StubActivity();
		// For this case we have a null token, and therefore should not create an activity.
		String token = "this is a token";
		ActivityTask activityTask = new ActivityTask().withTaskToken(token);
		when(mockSWFClient.pollForActivityTask(any(PollForActivityTaskRequest.class))).thenReturn(activityTask);
		ActivityWorker worker = watcher.pollForActivity(stubActivity);
		// this case should return a null activity.
		assertNotNull("A new activity should have been created when passed a token", worker);
		assertTrue(worker.activity instanceof StubActivity);
		StubActivity result = (StubActivity) worker.activity;
		// run the worker
		worker.run();
		assertNotNull(result.task);
		assertNotNull(result.simpleWorkFlowClient);
		assertNotNull(result.pfdar);
		assertEquals(activityTask, result.task);
		System.out.println(result.pfdar);
		assertEquals(stubActivity.getDomainName(), result.pfdar.getDomain());
		assertEquals(stubActivity.getTaskList(), result.pfdar.getTaskList());
		// This is the key that should have been used.
		String key = SimpleWorkFlowWatcher.getAddressProcessIdThreadId();
		assertEquals(key, result.pfdar.getIdentity());
	}
	
	/**
	 * When there is no token returned then we should not call the activity.
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	@Test
	public void testPollForDeciderNullToken() throws InstantiationException, IllegalAccessException{
		StubDecider stubDecider = new StubDecider();
		// For this case we have a null token, and therefore should not create an activity.
		when(mockSWFClient.pollForDecisionTask(any(PollForDecisionTaskRequest.class))).thenReturn(new DecisionTask().withTaskToken(null));
		DecisionWorker worker = watcher.pollForDecision(stubDecider);
		// this case should return a null activity.
		assertEquals("When the taks token is null then the decider should not be called.",null, worker);
	}
	
	/**
	 * When there is a token returned then we should call the decider.
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	@Test
	public void testPollForDeciderNonNullToken() throws InstantiationException, IllegalAccessException{
		StubDecider stubDecider = new StubDecider();
		// For this case we have a null token, and therefore should not create an activity.
		String token = "this is a token";
		DecisionTask decisionTask = new DecisionTask().withTaskToken(token);
		when(mockSWFClient.pollForDecisionTask(any(PollForDecisionTaskRequest.class))).thenReturn(decisionTask);
		DecisionWorker worker = watcher.pollForDecision(stubDecider);
		// this case should return a null activity.
		assertNotNull("A new decider should have been created when passed a token", worker);
		assertTrue(worker.decider instanceof StubDecider);
		// Run the worker
		worker.run();
		StubDecider result = (StubDecider) worker.decider;
		assertNotNull(result.task);
		assertNotNull(result.simpleWorkFlowClient);
		assertNotNull(result.pfdtr);
		assertEquals(decisionTask, result.task);
		System.out.println(result.pfdtr);
		assertEquals(stubDecider.getDomainName(), result.pfdtr.getDomain());
		assertEquals(stubDecider.getTaskList(), result.pfdtr.getTaskList());
		// This is the key that should have been used.
		String key = SimpleWorkFlowWatcher.getAddressProcessIdThreadId();
		assertEquals(key, result.pfdtr.getIdentity());
	}
	
	@Test
	public void testKickOffTaskFirstTime(){
		// The first time the kick off occurs the map is empty
		Map<String, Future<Void>> futureMap = new HashMap<String, Future<Void>>();
		StubDecider stubDecider = new StubDecider();
		Future<Void> mockFuture = (Future<Void>)Mockito.mock(Future.class);
		when(mockExecutor.submit(Matchers.<Callable<Void>>any())).thenReturn(mockFuture);
		when(mockFuture.isDone()).thenReturn(false);
		// This should submit the worker and put it on the map.
		watcher.kickOffTask(futureMap, stubDecider);
		assertEquals(1, futureMap.size());
		assertNotNull(futureMap.get(stubDecider.getTaskList().getName()));
		assertEquals(mockFuture, futureMap.get(stubDecider.getTaskList().getName()));
	}
	
	@Test
	public void testKickOffNotDone(){
		// The first time the kick off occurs the map is empty
		Map<String, Future<Void>> futureMap = new HashMap<String, Future<Void>>();
		StubDecider stubDecider = new StubDecider();
		Future<Void> mockFuture = (Future<Void>)Mockito.mock(Future.class);
		// put this future in the map
		futureMap.put(stubDecider.getTaskList().getName(), mockFuture);
		// The future is not done yet
		when(mockFuture.isDone()).thenReturn(false);
		when(mockExecutor.submit(Matchers.<Callable<Void>>any())).thenThrow(new IllegalStateException("A job was currently running so another should not have been kicked off."));
		// This should submit the worker and put it on the map.
		watcher.kickOffTask(futureMap, stubDecider);
		assertEquals(1, futureMap.size());
		assertNotNull(futureMap.get(stubDecider.getTaskList().getName()));
		assertEquals(mockFuture, futureMap.get(stubDecider.getTaskList().getName()));
	}
	
	@Test
	public void testKickOffInterruptedException() throws InterruptedException, ExecutionException{
		// The first time the kick off occurs the map is empty
		Map<String, Future<Void>> futureMap = new HashMap<String, Future<Void>>();
		StubDecider stubDecider = new StubDecider();
		Future<Void> mockFuture = (Future<Void>)Mockito.mock(Future.class);
		// put this future in the map
		futureMap.put(stubDecider.getTaskList().getName(), mockFuture);
		// The future is not done yet
		when(mockFuture.isDone()).thenReturn(true);
		// A failure should not kill the processs, but be logged.
		when(mockFuture.get()).thenThrow(new InterruptedException("Simulating an interupt exception"));
		// This should get kicked off next.
		Future<Void> mockFuture2 = (Future<Void>)Mockito.mock(Future.class);
		when(mockExecutor.submit(Matchers.<Callable<Void>>any())).thenReturn(mockFuture2);
		// This should submit the worker and put it on the map.
		watcher.kickOffTask(futureMap, stubDecider);
		assertEquals(1, futureMap.size());
		assertNotNull(futureMap.get(stubDecider.getTaskList().getName()));
		assertEquals(mockFuture2, futureMap.get(stubDecider.getTaskList().getName()));
	}
	
	@Test
	public void testKickOffExecutionException() throws InterruptedException, ExecutionException{
		// The first time the kick off occurs the map is empty
		Map<String, Future<Void>> futureMap = new HashMap<String, Future<Void>>();
		StubDecider stubDecider = new StubDecider();
		Future<Void> mockFuture = (Future<Void>)Mockito.mock(Future.class);
		// put this future in the map
		futureMap.put(stubDecider.getTaskList().getName(), mockFuture);
		// The future is not done yet
		when(mockFuture.isDone()).thenReturn(true);
		// A failure should not kill the processs, but be logged.
		when(mockFuture.get()).thenThrow(new ExecutionException("Simulating an ExecutionException", new IllegalArgumentException()));
		// This should get kicked off next.
		Future<Void> mockFuture2 = (Future<Void>)Mockito.mock(Future.class);
		when(mockExecutor.submit(Matchers.<Callable<Void>>any())).thenReturn(mockFuture2);
		// This should submit the worker and put it on the map.
		watcher.kickOffTask(futureMap, stubDecider);
		assertEquals(1, futureMap.size());
		assertNotNull(futureMap.get(stubDecider.getTaskList().getName()));
		assertEquals(mockFuture2, futureMap.get(stubDecider.getTaskList().getName()));
	}
	
	/**
	 * Stub used for testing.
	 * @author John
	 *
	 */
	public static class StubActivity implements Activity{
		
		public StubActivity(){
			
		}

		ActivityTask task;
		PollForActivityTaskRequest pfdar;
		AmazonSimpleWorkflowClient simpleWorkFlowClient;
		
		@Override
		public TaskList getTaskList() {
			return new TaskList().withName("StubActivityList");
		}

		@Override
		public String getDomainName() {
			return "StubActivityDomain";
		}

		@Override
		public void doWork(ActivityTask task, PollForActivityTaskRequest pfdar,
				AmazonSimpleWorkflowClient simpleWorkFlowClient) {
			this.task = task;
			this.pfdar = pfdar;
			this.simpleWorkFlowClient = simpleWorkFlowClient;
		}

		@Override
		public RegisterActivityTypeRequest getRegisterRequest() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ActivityType getActivityType() {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	/**
	 * Stub used for testing.
	 * @author John
	 *
	 */
	public static class StubDecider implements Decider{
		DecisionTask task;
		PollForDecisionTaskRequest pfdtr;
		AmazonSimpleWorkflowClient simpleWorkFlowClient;
		
		@Override
		public TaskList getTaskList() {
			return new TaskList().withName("StubDeciderList");
		}

		@Override
		public String getDomainName() {
			return "StubDeciderDomain";
		}

		@Override
		public AmazonWebServiceRequest makeDecision(DecisionTask dt,
				PollForDecisionTaskRequest pfdtr,
				AmazonSimpleWorkflowClient simpleWorkFlowClient) {
			this.task = dt;
			this.pfdtr = pfdtr;
			this.simpleWorkFlowClient = simpleWorkFlowClient;
			return null;
		}

	}
}
