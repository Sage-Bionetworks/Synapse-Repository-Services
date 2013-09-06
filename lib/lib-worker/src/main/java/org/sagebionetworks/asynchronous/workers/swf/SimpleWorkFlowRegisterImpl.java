package org.sagebionetworks.asynchronous.workers.swf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.DomainAlreadyExistsException;
import com.amazonaws.services.simpleworkflow.model.RegisterActivityTypeRequest;
import com.amazonaws.services.simpleworkflow.model.RegisterDomainRequest;
import com.amazonaws.services.simpleworkflow.model.RegisterWorkflowTypeRequest;
import com.amazonaws.services.simpleworkflow.model.TypeAlreadyExistsException;

/**
 * Implementation of the SimpleWorkFlowRegister.  This will do all of the one time work flow registration.
 * @author John
 *
 */
public class SimpleWorkFlowRegisterImpl implements SimpleWorkFlowRegister {
	
	static private Logger log = LogManager.getLogger(SimpleWorkFlowRegisterImpl.class);
	
	@Autowired
	AmazonSimpleWorkflowClient simpleWorkFlowClient;
	
	List<WorkFlow> workFlowList;
	
	/**
	 * Injected via Spring.
	 * 
	 * @param workFlowList
	 */
	public void setWorkFlowList(List<WorkFlow> workFlowList) {
		this.workFlowList = workFlowList;
	}
	
	/**
	 * This is the flat list of all tasks (Decider and Activities).
	 */
	List<Task> taskList;
	
	/**
	 * Spring will call this method upon startup after all properties are set.
	 */
	public void init(){
		// This is the flat list of all tasks (Deciders and Activities)
		taskList = new ArrayList<Task>();
		// Look at all of the work flows
		Set<String> domainSet = new HashSet<String>();
		List<RegisterActivityTypeRequest> activityTypesToRegister = new LinkedList<RegisterActivityTypeRequest>();
		List<RegisterWorkflowTypeRequest> workFlowsToRegister = new LinkedList<RegisterWorkflowTypeRequest>();
		// Build up the list of tasks
		for(WorkFlow workFlow: workFlowList){
			// All this to the list.
			RegisterWorkflowTypeRequest request = workFlow.getWorkFlowTypeRequest();
			workFlowsToRegister.add(request);
			// ensure this domain exists
			domainSet.add(request.getDomain());
			// Decider
			Decider decider = workFlow.getDecider();
			// Add the decider for this work flow.
			taskList.add(decider);
			// Add this domain to the set
			if(decider.getDomainName() == null) throw new IllegalArgumentException("Decider "+decider.getClass().getName()+" cannot have a null domainName ");
			// ensure this domain exists
			domainSet.add(decider.getDomainName());
			// Activities 
			List<Activity> activities = workFlow.getActivityList();
			// Add all of these activities to the task list
			taskList.addAll(activities);
			for(Activity activity: activities){
				// Add this domain to the set
				if(activity.getDomainName() == null) throw new IllegalArgumentException("Activity "+activity.getClass().getName()+" cannot have a null domainName ");
				if(activity.getRegisterRequest() == null) throw new IllegalArgumentException("Activity "+activity.getClass().getName()+" cannot have a null registerRequest");
				domainSet.add(activity.getDomainName());
				activityTypesToRegister.add(activity.getRegisterRequest());
			}
		}
		
		// Now make sure all domains are registered
		for(String domainName: domainSet){
			RegisterDomainRequest domainReg = new RegisterDomainRequest();
			domainReg.setName(domainName);
			domainReg.setWorkflowExecutionRetentionPeriodInDays(StackConfiguration.getWorkflowExecutionRetentionPeriodInDays());
			domainReg.setDescription("Generated domain for Syanpse work flows: "+domainName);
			try{
				simpleWorkFlowClient.registerDomain(domainReg);
				log.info("Created a new AWS Simple Work Flow domain: "+domainName);
			}catch(DomainAlreadyExistsException e){
				log.info("AWS Simple Work Flow domain: "+domainName+" already exists");
			}
		}
		
		// First register the work flows
		for(RegisterWorkflowTypeRequest request: workFlowsToRegister){
			try{
				simpleWorkFlowClient.registerWorkflowType(request);
				log.info("Created a new AWS Simple Work Flow : "+request.getName());
			}catch(TypeAlreadyExistsException e){
				log.info("AWS Simple Work Flow: "+request.getName()+" already exists");
			}
		}
		
		// Now register all Activity types.
		for(RegisterActivityTypeRequest request: activityTypesToRegister){
			try{
				// Try to register this type.
				simpleWorkFlowClient.registerActivityType(request);
				log.info("Registered new AWS SWF Activity type:"+request);
			}catch(TypeAlreadyExistsException e){
				log.info("AWS SWF Activity type already exists: "+request);
			}
		}
	}

	@Override
	public List<Task> getTaskList() {
		return taskList;
	}

	@Override
	public List<WorkFlow> getWorkFlowList() {
		return workFlowList;
	}

}
