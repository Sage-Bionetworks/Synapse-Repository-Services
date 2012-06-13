package org.sagebionetworks.repo.manager.swf;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
	
	static private Log log = LogFactory.getLog(SimpleWorkFlowRegisterImpl.class);
	
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
	 * Make an asynchronous poll for either an activity or decider.
	 * 
	 * @param task
	 * @return
	 */
	protected Future<Void> pollForTask(final Task task) {
		Future<Void> future;
		future = executors.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				// This will either be a decider or activity
				if(task instanceof Decider){
					// This is a decider
					Decider decider = (Decider) task;
					pollForDecider(decider);
				}else if(task instanceof Activity){
					// This is an activity.
					Activity activity = (Activity) task;
					pollForActivity(activity);
				}else{
					throw new IllegalArgumentException("Unknown task type: "+task.getClass().getName());
				}
				// done
				return null;
			}
		});
		return future;
	}
	
	/**
	 * Poll for a decider.
	 * @param key
	 * @param decider
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	protected DecisionWorker pollForDecider(Decider decider)throws InstantiationException, IllegalAccessException {
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
			// Let the decider use this thread to work
			DecisionWorker worker = new DecisionWorker(decider.getClass().newInstance(), dt, pfdtr, simpleWorkFlowClient);
			// Do the work on another thread.
			executors.submit(worker);
			return worker;
		}else{
			if(log.isTraceEnabled()){
				log.trace("Long poll for decider: "+decider.getClass().getName()+" return null task token.  Not decisions needed.");
			}
			// Nothing needs to be done.
			return null;
		}

	}
	
	/**
	 * Poll for a decider.
	 * @param key
	 * @param decider
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
			// Let the activity use this thread to work
			Activity instance = activity.getClass().newInstance();
			ActivityWorker worker = new ActivityWorker(instance, at, pfatr, simpleWorkFlowClient);
			// Do the work on another thread.
			executors.submit(worker);
			return worker;
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
