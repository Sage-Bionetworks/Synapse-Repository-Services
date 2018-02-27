package org.sagebionetworks.worker.job.tracking;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.MetricStats;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.IntervalStatistics;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public class JobIntervalProcessorTest {

	@Mock
	JobTracker mockJobTracker;

	@Mock
	Consumer mockConsumer;

	@Mock
	Clock mockClock;
	
	@Captor
	ArgumentCaptor<List<ProfileData>> profileCaptor;
	
	JobIntervalProcessor processor;
	
	String nameSpace;
	String units;
	String metricName;
	IntervalStatistics stats;
	IntervalStatistics statsTwo;
	String workerName;
	String workerNameTwo;
	Date timestamp;
	
	Map<String, IntervalStatistics> intervalMap;
	
	Set<String> allKnownJobNames;
	Map<String, Long> startedJobTimes;
	Map<String, List<Long>> finishedJobElapsTimes;
	
	TrackedData consumedData;
	
	@Before
	public void before(){
		processor = new JobIntervalProcessor();
		ReflectionTestUtils.setField(processor, "jobTracker", mockJobTracker);
		ReflectionTestUtils.setField(processor, "consumer", mockConsumer);
		ReflectionTestUtils.setField(processor, "clock", mockClock);
	
		when(mockClock.currentTimeMillis()).thenReturn(1l, 2L,3L,4L,5L,6L);
		
		nameSpace = "nameSpace";
		units = "units";
		metricName = "metricName";
		stats = new IntervalStatistics(0);
		stats.addValue(2);
		stats.addValue(5);
		workerName = "workerName";
		timestamp = new Date(1);
		
		intervalMap = new HashMap<String, IntervalStatistics>();
		intervalMap.put(workerName, stats);
		
		workerNameTwo ="workerTwo";
		statsTwo = new IntervalStatistics(777);
		intervalMap.put(workerNameTwo, statsTwo);	
		
		allKnownJobNames = Sets.newHashSet(workerName, workerNameTwo);
		startedJobTimes = new HashMap<String, Long>();
		startedJobTimes.put(workerName, 2L);
		finishedJobElapsTimes = new HashMap<String, List<Long>>();
		finishedJobElapsTimes.put(workerNameTwo, Lists.newArrayList(3L,4L));
		consumedData = new TrackedData(allKnownJobNames, startedJobTimes, finishedJobElapsTimes);
		
		when(mockJobTracker.consumeTrackedData()).thenReturn(consumedData);
	}
	
	@Test
	public void testCreateProfileData(){
		// call under test
		ProfileData results = JobIntervalProcessor.createProfileData(nameSpace, units, metricName, stats, workerName, timestamp);
		assertNotNull(results);
		assertEquals(nameSpace, results.getNamespace());
		assertEquals(metricName, results.getName());
		assertEquals(units, results.getUnit());
		// single value is not used for this case
		assertEquals(null, results.getValue());
		assertNotNull(results.getDimension());
		assertEquals(1, results.getDimension().size());
		assertEquals(workerName, results.getDimension().get(JobIntervalProcessor.DIMENSION_WORKER_NAME));
		MetricStats metricStats = results.getMetricStats();
		assertNotNull(metricStats);
		assertEquals(new Double(0), metricStats.getMinimum());
		assertEquals(new Double(5), metricStats.getMaximum());
		assertEquals(new Double(7), metricStats.getSum());
		assertEquals(new Double(3), metricStats.getCount());
	}
	
	@Test
	public void testCreateProfileDataForMap(){
		List<ProfileData> results = JobIntervalProcessor.createProfileDataForMap(intervalMap, nameSpace, metricName, units, timestamp);
		assertNotNull(results);
		assertEquals(2, results.size());
		ProfileData one = results.get(0);
		assertEquals(workerName, one.getDimension().get(JobIntervalProcessor.DIMENSION_WORKER_NAME));
		ProfileData two = results.get(1);
		assertEquals(workerNameTwo, two.getDimension().get(JobIntervalProcessor.DIMENSION_WORKER_NAME));
	}
	
	@Test
	public void testPublishAndClearStatistics(){
		assertEquals(2, intervalMap.size());
		// call under test
		processor.publishAndClearStatistics(intervalMap, nameSpace, metricName, units, timestamp);
		verify(mockConsumer).addProfileData(profileCaptor.capture());
		List<ProfileData> results = profileCaptor.getValue();
		assertNotNull(results);
		assertEquals(2, results.size());
		// the passed map should be cleared
		assertEquals(0, intervalMap.size());
	}
	
	@Test
	public void testAddValueToIntervalMapEmpty(){
		Map<String, IntervalStatistics> map = new HashMap<String, IntervalStatistics>();
		// call under test
		JobIntervalProcessor.addValueToIntervalMap(workerName, 123.4, map);
		assertEquals(1, map.size());
		IntervalStatistics stats = map.get(workerName);
		assertEquals(new Double(123.4), new Double(stats.getValueSum()));
	}

	@Test
	public void testAddValueToIntervalMapWithValue(){
		Map<String, IntervalStatistics> map = new HashMap<String, IntervalStatistics>();
		// call under test
		JobIntervalProcessor.addValueToIntervalMap(workerName, 1, map);
		// second call should add to the first
		JobIntervalProcessor.addValueToIntervalMap(workerName, 2, map);
		assertEquals(1, map.size());
		IntervalStatistics stats = map.get(workerName);
		assertEquals(new Double(3), new Double(stats.getValueSum()));
		assertEquals(2L, stats.getValueCount());
	}
	
	@Test
	public void testCalculateCompletedJobCount(){
		// call under test
		processor.calculateCompletedJobCount(consumedData);
		// stats should exist for both jobs
		assertEquals(2, processor.completedJobCountStatistics.size());
		IntervalStatistics oneStats = processor.completedJobCountStatistics.get(workerName);
		// the first worker does not have any completed jobs so should have a single zero value.
		assertEquals(new Double(0), new Double(oneStats.getValueSum()));
		assertEquals(1L, oneStats.getValueCount());
		
		IntervalStatistics twoStats = processor.completedJobCountStatistics.get(workerNameTwo);
		// the second worker has two completed jobs plus on default job
		assertEquals(new Double(2), new Double(twoStats.getValueSum()));
		assertEquals(3L, twoStats.getValueCount());
	}
	
	@Test
	public void testCalculateCumulativeRuntime(){
		long now = 5L;
		// call under test
		processor.calculateCumulativeRuntime(consumedData, now);
		// stats should exist for both jobs
		assertEquals(2, processor.cumulativeRuntimeStatisitsics.size());
		IntervalStatistics stats = processor.cumulativeRuntimeStatisitsics.get(workerName);
		// the worker was started at 2 MS and it is now 5 MS so runtime is 3
		assertEquals(new Double(3), new Double(stats.getValueSum()));
		assertEquals(1L, stats.getValueCount());

		stats = processor.cumulativeRuntimeStatisitsics.get(workerNameTwo);
		// the second worker is not running
		assertEquals(new Double(0), new Double(stats.getValueSum()));
		assertEquals(1L, stats.getValueCount());
	}
	
	@Test
	public void testCalculatePercentTimeRunning(){
		// call under test
		processor.calculatePercentTimeRunning(consumedData);
		// stats should exist for both jobs
		assertEquals(2, processor.percentTimeRunningStatistics.size());
		IntervalStatistics stats = processor.percentTimeRunningStatistics.get(workerName);
		// this worker is running so it is at 100%
		assertEquals(new Double(100), new Double(stats.getValueSum()));
		assertEquals(1L, stats.getValueCount());

		stats = processor.percentTimeRunningStatistics.get(workerNameTwo);
		// this worker is not running so it is at 0%
		assertEquals(new Double(0), new Double(stats.getValueSum()));
		assertEquals(1L, stats.getValueCount());
	}
	
	@Test
	public void testTimerFiredUnderPushTime(){
		// call under test
		processor.timerFired();
		processor.timerFired();
		assertEquals(2, processor.percentTimeRunningStatistics.size());
		assertEquals(2, processor.cumulativeRuntimeStatisitsics.size());
		assertEquals(2, processor.completedJobCountStatistics.size());
		// data should be be pushed
		verifyNoMoreInteractions(mockConsumer);
	}
	
	@Test
	public void testTimerFiredOverPushTime(){
		// setup clock such that the second timer fire is after the metric push frequency
		when(mockClock.currentTimeMillis()).thenReturn(1l, JobIntervalProcessor.METRIC_PUSH_FREQUENCY_MS+10);
		// call under test
		processor.timerFired();
		processor.timerFired();
		assertEquals(0, processor.percentTimeRunningStatistics.size());
		assertEquals(0, processor.cumulativeRuntimeStatisitsics.size());
		assertEquals(0, processor.completedJobCountStatistics.size());
		// data should be pushed three times (once for each type).
		verify(mockConsumer, times(3)).addProfileData(anyListOf(ProfileData.class));
	}
}
