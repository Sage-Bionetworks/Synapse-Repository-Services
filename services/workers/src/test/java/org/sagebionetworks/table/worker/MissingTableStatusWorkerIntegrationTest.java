package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MissingTableStatusWorkerIntegrationTest {
	
	@Autowired
	private Scheduler scheduler;
	
	private Trigger dispatcherTrigger;
	
	@BeforeEach
	public void before() throws SchedulerException {
		dispatcherTrigger = scheduler.getTrigger(new TriggerKey("missingTableStatusWorkerTrigger"));
		
		assertNotNull(dispatcherTrigger);
	}
	
	@Test
	public void test() throws SchedulerException {
		// Manually trigger the job since the start time is very long
		scheduler.triggerJob(dispatcherTrigger.getJobKey(), dispatcherTrigger.getJobDataMap());
		System.out.println("Just for a breakpoint");
	}

}
