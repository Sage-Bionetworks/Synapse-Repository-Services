package org.sagebionetworks.repo.manager.swf;

import org.springframework.scheduling.quartz.SimpleTriggerBean;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsyncClient;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;

public class Test {
	
	AmazonSimpleWorkflowClient client;
	AmazonSimpleWorkflowAsyncClient asynchClient;
	String one = "Simple Work Flow Register";
	SimpleTriggerBean test;

}
