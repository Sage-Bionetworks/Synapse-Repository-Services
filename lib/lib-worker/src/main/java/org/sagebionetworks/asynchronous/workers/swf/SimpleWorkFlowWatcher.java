package org.sagebionetworks.asynchronous.workers.swf;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.ActivityTask;
import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.PollForActivityTaskRequest;
import com.amazonaws.services.simpleworkflow.model.PollForDecisionTaskRequest;
import com.amazonaws.services.simpleworkflow.model.TaskList;

/**
 * Polls for both Decisions and Activity tasks that are registered with the SimpleWorkFlowRegister.
 * When a poll request returns a token, then a Decider or Activity is instantiated and given
 * a chance to either make a decision or do activity work.
 * After a poll expires or work is done, a new poll request will be kicked off in 
 * an infinite loop.
 * 
 * @author John
 *
 */
public class SimpleWorkFlowWatcher {
	
	static private Logger log = LogManager.getLogger(SimpleWorkFlowRegisterImpl.class);
	
	@Autowired
	AmazonSimpleWorkflowClient simpleWorkFlowClient;

	@Autowired
	SimpleWorkFlowRegister swfRegister;	
	
	/**
	 * This is the constructor used by spring.
	 */
	public SimpleWorkFlowWatcher(){
	}
	
	/**
	 * This constructor is used for unit testing.
	 * @param simpleWorkFlowClient
	 * @param swfRegister
	 * @param executors
	 */
	public SimpleWorkFlowWatcher(AmazonSimpleWorkflowClient simpleWorkFlowClient,
			SimpleWorkFlowRegister swfRegister, ExecutorService executors) {
		super();
		this.simpleWorkFlowClient = simpleWorkFlowClient;
		this.swfRegister = swfRegister;
		this.executors = executors;
	}

	/**
	 * This is our thread pool.
	 */
	ExecutorService executors = Executors.newCachedThreadPool();
	

	/**
	 * This is the main loop.
	 * 
	 * This loop will start a poll request for each registered task (Decider and Activity).
	 * 
	 * @throws InterruptedException 
	 */
	public void loop() throws InterruptedException{
		// Get the list of tasks from the register.
		List<Task> deciderList = swfRegister.getTaskList();
		// Now start the loop
		Map<String, Future<Void>> futureMap = new HashMap<String, Future<Void>>();
		// this loop should run for the life of the application.
		while(true){
			kickOffAllTasks(deciderList, futureMap);
			// Let other threads run
			Thread.yield();
		}
	}

	/**
	 * Kick off a poll request for each task.
	 * 
	 * @param deciderList
	 * @param futureMap
	 * @throws InterruptedException
	 */
	private void kickOffAllTasks(List<Task> deciderList,
			Map<String, Future<Void>> futureMap) throws InterruptedException {
		// Keep polling for each decider.
		for(Task task: deciderList){
			// Kick off this task.
			kickOffTask(futureMap, task);
			// Let other threads run
			Thread.yield();
		}
		// Let other threads run.
		Thread.sleep(1000);
	}

	/**
	 * Kick off a single task.
	 * @param futureMap
	 * @param task
	 */
	protected void kickOffTask(Map<String, Future<Void>> futureMap, Task task) {
		TaskList tl = task.getTaskList();
		if(tl == null) throw new IllegalArgumentException("TaskList for "+task.getClass().getName()+" was null");
		if(tl.getName() == null) throw new IllegalArgumentException("TaskList.name for "+task.getClass().getName()+" was null");
		// First determine if we have a future for this decider
		Future<Void> future = futureMap.get(tl.getName());
		if(future == null){
			future = pollForTask(task);
			futureMap.put(tl.getName(), future);
		}
		// Is this future done
		if(future.isDone()){
			// get the value to determine if an exception was thrown.
			try {
				future.get();
			} catch (InterruptedException e) {
				log.error("Failed: running Decider: "+task.getClass().getName(), e);
			} catch (ExecutionException e) {
				log.error("Failed: running Decider: "+task.getClass().getName(), e);
			}
			// Start it up again.
			future = pollForTask(task);
			futureMap.put(tl.getName(), future);
		}
	}

	/**
	 * This is a non-blocking method for polling for a Task. When a task token is
	 * returned from   The resulting Future
	 * can be used to determine if the polling has finished.  When polling generates
	 * a task token a worker will be created and run on separate thread.
	 * @param task - The task to poll for.
	 * @return The returned future can be used to determine when polling has finished.
	 * Note: When Future.isDone() returns true, you know it has finished polling, and 
	 * possible started a worker on another thread.  The worker could still be running
	 * after the resulting Future.isDone() returns true.
	 */
	public Future<Void> pollForTask(final Task task) {
		Future<Void> future;
		future = executors.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				Runnable worker = null;
				// This will either be a decider or activity
				if(task instanceof Decider){
					// This is a decider
					Decider decider = (Decider) task;
					worker = pollForDecision(decider);
				}else if(task instanceof Activity){
					// This is an activity.
					Activity activity = (Activity) task;
					worker = pollForActivity(activity);
				}else{
					throw new IllegalArgumentException("Unknown task type: "+task.getClass().getName());
				}
				// If a worker was produced then we need to run it
				if(worker != null){
					// Schedule the worker
					executors.execute(worker);
				}
				// done
				return null;
			}
		});
		return future;
	}
	
	/**
	 * This method will block while polling SWF for the given decider. If SWF returns a task
	 * token then a worker will be created to handle the decision.  This method will not
	 * run the worker!  It is up to the caller to actually run the worker.
	 * @param decider - The TaksList and domain name are used to poll SWF for a decision.
	 * @return If SWF returns a task token then a worker will be created to handle the decision.
	 * This method will not run the worker!  If a token was not returned from the polling, then null will
	 * be returned.
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public DecisionWorker pollForDecision(Decider decider)throws InstantiationException, IllegalAccessException {
		// The ID is a combination of the address, process id and thread id.
		String key = getAddressProcessIdThreadId();
		PollForDecisionTaskRequest pfdtr = new PollForDecisionTaskRequest();
		pfdtr.setDomain(decider.getDomainName());
		pfdtr.setIdentity(key);
		pfdtr.setTaskList(decider.getTaskList());
		DecisionTask dt = simpleWorkFlowClient.pollForDecisionTask(pfdtr);
		if(dt.getTaskToken() != null){
			if(log.isTraceEnabled()){
				log.trace("Long poll for decider: "+decider.getClass().getName()+" return a task token:"+dt.getTaskToken()+", starting decider");
			}
			// Create a worker to make a decision.
			return new DecisionWorker(decider, dt, pfdtr, simpleWorkFlowClient);
		}else{
			if(log.isTraceEnabled()){
				log.trace("Long poll for decider: "+decider.getClass().getName()+" return null task token.  Not decisions needed.");
			}
			// Nothing needs to be done.
			return null;
		}

	}
	

	/**
	 * This method will block while polling SWF for the given activity. If SWF returns a task
	 * token then a worker will be created to handle the activity.  This method will not
	 * run the worker!  It is up to the caller to actually run the worker.
	 * 
	 * @param activity - The activity to poll for.  The activity task list will be used for polling SWF.
	 * @return If SWF returns a task token then a worker will be created to handle the activity.
	 * This method will not run the worker!  If a token was not returned from the polling, then null will
	 * be returned.
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	protected ActivityWorker pollForActivity(Activity activity)throws InstantiationException, IllegalAccessException {
		// The ID is a combination of the address, process id and thread id.
		String key = getAddressProcessIdThreadId();
		PollForActivityTaskRequest pfatr = new PollForActivityTaskRequest();
		pfatr.setDomain(activity.getDomainName());
		pfatr.setIdentity(key);
		pfatr.setTaskList(activity.getTaskList());
		ActivityTask at = simpleWorkFlowClient.pollForActivityTask(pfatr);
		if(at.getTaskToken() != null){
			if(log.isTraceEnabled()){
				log.trace("Long poll for activity: "+activity.getClass().getName()+" return a task token:"+at.getTaskToken()+", starting decider");
			}
			// Create the worker that will handle the activity.
			return new ActivityWorker(activity, at, pfatr, simpleWorkFlowClient);
		}else{
			if(log.isTraceEnabled()){
				log.trace("Long poll for activity: "+activity.getClass().getName()+" return null task token.  Not activity work needed.");
			}
			// Nothing needs to be done.
			return null;
		}
	}
	
	/**
	 * This string is a concatenation of <local_ip_address>+<process_id>+<thread_id>
	 * @return
	 */
	public static String getAddressProcessIdThreadId(){
		return StackConfiguration.getIpAddress().getHostAddress()+"-"+ManagementFactory.getRuntimeMXBean().getName()+"-"+Thread.currentThread().getId();
	}

}
