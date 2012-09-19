package org.sagebionetworks.search.workers.swf.search.index;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.asynchronous.workers.swf.DecisionWorker;
import org.sagebionetworks.asynchronous.workers.swf.SimpleWorkFlowRegister;
import org.sagebionetworks.asynchronous.workers.swf.SimpleWorkFlowWatcher;
import org.sagebionetworks.search.workers.swf.search.index.SearchIndexWorkFlow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.Run;
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:swf-manager-spb.xml" })
public class SearchIndexWorkFlowTest {
	
	@Autowired
	SearchIndexWorkFlow searchIndexWorkFlow;
	
	@Autowired
	SimpleWorkFlowRegister swfRegister;
	
	@Autowired
	AmazonSimpleWorkflowClient simpleWorkFlowClient;
	
	@Autowired
	SimpleWorkFlowWatcher workFlowWatcher;
	
	@Ignore // This test is not ready to run.
	@Test
	public void testStartWorkFlow() throws InterruptedException, InstantiationException, IllegalAccessException{
			
		// Start this work flow
		Run run = searchIndexWorkFlow.startWorkFlow();
		assertNotNull(run);
		System.out.println(run);
		// Run a decider
		DecisionWorker worker = workFlowWatcher.pollForDecision(searchIndexWorkFlow.getDecider());
		assertNotNull(worker);
		// Run the worker
		worker.run();

	}

}
