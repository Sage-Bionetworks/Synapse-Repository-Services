package org.sagebionetworks.worker.job.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.util.Clock;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class JobTrackerImplTest {
	
	@Mock
	Clock mockClock;
	
	JobTrackerImpl tracker;

	@Before
	public void before(){
		tracker = new JobTrackerImpl();
		ReflectionTestUtils.setField(tracker, "clock", mockClock);
		
		when(mockClock.currentTimeMillis()).thenReturn(1l, 2L,3L,4L,5L,6L);
	}
	
	@Test
	public void testStartJob(){
		String jobOne =  "one";
		String jobTwo =  "two";
		// call under test
		tracker.jobStarted(jobOne);
		tracker.jobStarted(jobTwo);
		TrackedData data = tracker.consumeTrackedData();
		assertNotNull(data);
		assertNotNull(data.getStartedJobTimes());
		Long startTime = data.getStartedJobTimes().get(jobOne);
		assertEquals(new Long(1), startTime);
		startTime = data.getStartedJobTimes().get(jobTwo);
		assertEquals(new Long(2), startTime);
		
		assertEquals(2, data.getAllKnownJobNames().size());
		assertTrue(data.getAllKnownJobNames().contains(jobOne));
		assertTrue(data.getAllKnownJobNames().contains(jobTwo));
		// there are no finished jobs.
		assertEquals(0, data.getFinishedJobElapsTimes().size());
	}
	
	@Test
	public void testEndJob(){
		String jobOne =  "one";
		String jobTwo =  "two";
		tracker.jobStarted(jobTwo);
		tracker.jobStarted(jobOne);
		// call under test
		tracker.jobEnded(jobTwo);
		tracker.jobStarted(jobTwo);
		// call under test
		tracker.jobEnded(jobTwo);
		
		TrackedData data = tracker.consumeTrackedData();
		assertNotNull(data);
		assertNotNull(data.getStartedJobTimes());
		assertEquals(1, data.getFinishedJobElapsTimes().size());
		List<Long> elapseTimes = data.getFinishedJobElapsTimes().get(jobTwo);
		assertNotNull(elapseTimes);
		assertEquals(2, elapseTimes.size());
		
		assertEquals(new Long(2), elapseTimes.get(0));
		assertEquals(new Long(1), elapseTimes.get(1));
	}

}
