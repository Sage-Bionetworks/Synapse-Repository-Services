package org.sagebionetworks.repo.manager.swf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.ActivityTypeInfos;
import com.amazonaws.services.simpleworkflow.model.DomainInfo;
import com.amazonaws.services.simpleworkflow.model.DomainInfos;
import com.amazonaws.services.simpleworkflow.model.ListActivityTypesRequest;
import com.amazonaws.services.simpleworkflow.model.ListDomainsRequest;
import com.amazonaws.services.simpleworkflow.model.ListWorkflowTypesRequest;
import com.amazonaws.services.simpleworkflow.model.RegisterActivityTypeRequest;
import com.amazonaws.services.simpleworkflow.model.RegisterWorkflowTypeRequest;
import com.amazonaws.services.simpleworkflow.model.RegistrationStatus;
import com.amazonaws.services.simpleworkflow.model.WorkflowTypeInfo;
import com.amazonaws.services.simpleworkflow.model.WorkflowTypeInfos;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:swf-manager-spb.xml" })
public class SimpleWorkFlowRegisterImplTest {

	@Autowired
	SimpleWorkFlowRegister swfRegister;


	@Autowired
	AmazonSimpleWorkflowClient simpleWorkFlowClient;
	
	
	@Test
	public void testDomain() throws Exception{
		assertNotNull(simpleWorkFlowClient);
		// Give AWS a second to have the registration.
		Thread.sleep(1000);
		// Get the list of tasks
		for(Task task: swfRegister.getTaskList()){
			// Make sure the domain is registered
			DomainInfos resutls = simpleWorkFlowClient.listDomains(new ListDomainsRequest().withRegistrationStatus(RegistrationStatus.REGISTERED));
			assertNotNull(resutls);
			assertTrue(resutls.getDomainInfos().size() > 0);
			// Find our domain
			boolean found = false;
			for(DomainInfo info: resutls.getDomainInfos()){
				if(task.getDomainName().equals(info.getName())){
					found = true;
					break;
				}
			}
			assertTrue("Failed to find our domain: "+task.getDomainName(), found);
		}
	}
	
	@Test
	public void testRegisterWorkFlowAndActivityTypes() throws InterruptedException{
		assertNotNull(simpleWorkFlowClient);
		// Give AWS a second to have the registration.
		Thread.sleep(5000);
		for(WorkFlow workFlow: swfRegister.getWorkFlowList()){
			// Make sure the domain is registered
			RegisterWorkflowTypeRequest typeRequest = workFlow.getWorkFlowTypeRequest();
			assertNotNull(typeRequest);
			ListWorkflowTypesRequest query = new ListWorkflowTypesRequest().withDomain(typeRequest.getDomain()).withName(typeRequest.getName()).withRegistrationStatus(RegistrationStatus.REGISTERED);
			WorkflowTypeInfos resutls = simpleWorkFlowClient.listWorkflowTypes(query);
			assertNotNull(resutls);
			assertTrue(resutls.getTypeInfos().size() > 0);
			// Find our domain
			boolean found = false;
			for(WorkflowTypeInfo info: resutls.getTypeInfos()){
				if(info.getWorkflowType().getVersion().equals(typeRequest.getVersion())){
					found = true;
					// We should also be able to find the activities for this type.
					for(Activity activity: workFlow.getActivityList()){
						
						RegisterActivityTypeRequest activityRegisterRequest = activity.getRegisterRequest();
						ListActivityTypesRequest activityTypeRequest = new ListActivityTypesRequest();
						
						activityTypeRequest.withDomain(typeRequest.getDomain()).withName(activityRegisterRequest.getName()).withRegistrationStatus(RegistrationStatus.REGISTERED);
						ActivityTypeInfos activityTypeInfos = simpleWorkFlowClient.listActivityTypes(activityTypeRequest);
						assertTrue(activityTypeInfos.getTypeInfos().size() > 0);
					}
					break;
				}
			}
			assertTrue("Failed to find our domain: "+typeRequest, found);
		}
	}
}
