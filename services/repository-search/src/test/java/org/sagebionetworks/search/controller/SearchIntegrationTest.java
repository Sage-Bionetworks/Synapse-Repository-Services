package org.sagebionetworks.search.controller;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.LinkedList;

import javax.servlet.ServletException;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.SchedulerException;
import org.quartz.impl.StdScheduler;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.BeansException;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * End to end test for the search services.
 * @author jmhill
 *
 */
public class SearchIntegrationTest {
	
	private MessageReceiver reciever;
	
	@Before
	public void before() throws BeansException, ServletException, SchedulerException{
		reciever = (MessageReceiver) DispatchServletSingleton.getInstance().getWebApplicationContext().getBean("searchQueueMessageReveiver");
		SimpleTriggerBean trigger = (SimpleTriggerBean) DispatchServletSingleton.getInstance().getWebApplicationContext().getBean("searchQueueMessageReveiverTrigger");
		StdScheduler scheduler = (StdScheduler) DispatchServletSingleton.getInstance().getWebApplicationContext().getBean("mainScheduler");
		SchedulerFactoryBean bean;
//		scheduler.start();
		System.out.println(scheduler);
	}
	
	
	@Test
	public void test() throws ServletException, IOException, JSONException, JSONObjectAdapterException, InterruptedException {
	
		assertNotNull(reciever);
		reciever.triggerFired();
		// First run query
		SearchQuery query = new SearchQuery();
		query.setBooleanQuery(new LinkedList<KeyValue>());
		KeyValue kv = new KeyValue();
		kv.setKey("id");
		kv.setValue("syn123");
		query.getBooleanQuery().add(kv);
		// Execute the query
		SearchResults results = ServletTestHelper.getSearchResults(TestUserDAO.TEST_USER_NAME, query);
		assertNotNull(results);
		
		while(true){
			Thread.sleep(1000);
			System.out.println("Sleeping");
		}
	}
	
}
