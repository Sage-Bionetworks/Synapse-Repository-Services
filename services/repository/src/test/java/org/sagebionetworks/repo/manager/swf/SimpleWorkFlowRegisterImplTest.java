package org.sagebionetworks.repo.manager.swf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.DomainInfo;
import com.amazonaws.services.simpleworkflow.model.DomainInfos;
import com.amazonaws.services.simpleworkflow.model.ListDomainsRequest;
import com.amazonaws.services.simpleworkflow.model.RegistrationStatus;

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
}
