package org.sagebionetworks.dynamo.dao.rowcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.dao.table.CurrentRowCacheDao;
import org.sagebionetworks.repo.model.table.CurrentRowCacheStatus;
import org.sagebionetworks.util.ProgressCallback;
import org.sagebionetworks.util.ReflectionStaticTestUtils;
import org.sagebionetworks.util.TestClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.DescribeTableResult;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughputDescription;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodb.model.TableDescription;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class CurrentRowCacheDaoAutowireTest {

	@Autowired
	private CurrentRowCacheDao currentRowCacheDao;

	private Object oldClock;
	private Object oldMapper;
	private Object oldDynamoClient;

	private Long tableId = new Random().nextLong();

	@BeforeClass
	public static void beforeClass() {
		StackConfiguration config = new StackConfiguration();
		Assume.assumeTrue(config.getDynamoEnabled());
	}

	@Before
	public void setup() throws Exception {
		currentRowCacheDao.deleteCurrentTable(tableId);
		oldClock = ReflectionTestUtils.getField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)),
				"clock");
		oldMapper = ReflectionTestUtils.getField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)),
				"mapper");
		oldDynamoClient = ReflectionTestUtils.getField(
				((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)), "dynamoClient");
	}

	@After
	public void destroy() throws Exception {
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)), "clock",
				oldClock);
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)), "mapper",
				oldMapper);
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)),
				"dynamoClient", oldDynamoClient);
		currentRowCacheDao.deleteCurrentTable(tableId);
	}

	@Test
	public void testRoundTrip() {
		currentRowCacheDao.putCurrentVersion(tableId, 12L, 1L);
		Long version = currentRowCacheDao.getCurrentVersion(tableId, 12L);
		assertEquals(1L, version.longValue());
	}

	@Test
	public void testRoundTripMultiple() {
		currentRowCacheDao.putCurrentVersion(tableId, 12L, 1L);
		Map<Long, Long> map = Maps.newHashMap();
		map.put(14L, 144L);
		map.put(15L, 155L);
		currentRowCacheDao.putCurrentVersions(tableId, map, null);

		Map<Long, Long> result = currentRowCacheDao.getCurrentVersions(tableId, Lists.newArrayList(12L, 13L, 14L, 15L));
		map.put(12L, 1L);
		assertEquals(map, result);
	}

	@Test
	public void testRanges() {
		Map<Long, Long> map = Maps.newHashMap();
		map.put(13L, 144L);
		map.put(14L, 144L);
		map.put(15L, 155L);
		currentRowCacheDao.putCurrentVersions(tableId, map, null);

		// uses range query
		Map<Long, Long> result = currentRowCacheDao.getCurrentVersions(tableId, Lists.newArrayList(12L, 13L, 14L, 15L, 16L));
		assertEquals(map, result);

		// uses batch load
		result = currentRowCacheDao.getCurrentVersions(tableId, Lists.newArrayList(12L, 13L, 14L, 15L, 1600000L));
		assertEquals(map, result);

		result = currentRowCacheDao.getCurrentVersions(tableId, Lists.newArrayList(12L, 16L));
		assertEquals(Collections.emptyMap(), result);

		result = currentRowCacheDao.getCurrentVersions(tableId, Lists.<Long> newArrayList());
		assertEquals(Collections.emptyMap(), result);

		map.remove(14L);
		result = currentRowCacheDao.getCurrentVersions(tableId, Lists.newArrayList(12L, 13L, 15L, 16L));
		assertEquals(map, result);

		result = currentRowCacheDao.getCurrentVersions(tableId, Lists.newArrayList(13L, 15L));
		assertEquals(map, result);

		map.remove(13L);
		result = currentRowCacheDao.getCurrentVersions(tableId, Lists.newArrayList(15L));
		assertEquals(map, result);
	}

	@Test
	public void testGetAllCurrent() {
		Map<Long, Long> map = Maps.newHashMap();
		map.put(13L, 144L);
		map.put(14L, 144L);
		map.put(15L, 155L);
		currentRowCacheDao.putCurrentVersions(tableId, map, null);

		Map<Long, Long> result = currentRowCacheDao.getCurrentVersions(tableId, 0L, 100L);
		assertEquals(map, result);

		result = currentRowCacheDao.getCurrentVersions(tableId, 13L, 3L);
		assertEquals(map, result);

		ImmutableMap<Long, Long> firstTwo = ImmutableMap.<Long, Long> builder().put(13L, 144L).put(14L, 144L).build();

		result = currentRowCacheDao.getCurrentVersions(tableId, 1L, 14L);
		assertEquals(firstTwo, result);

		result = currentRowCacheDao.getCurrentVersions(tableId, 15L, 100L);
		assertEquals(Collections.singletonMap(15L, 155L), result);

		currentRowCacheDao.deleteCurrentVersion(tableId, 13L);
		map.remove(13L);
		result = currentRowCacheDao.getCurrentVersions(tableId, 0L, 100L);
		assertEquals(map, result);
	}

	@Test
	public void testGetNonExistentRow() {
		Long version = currentRowCacheDao.getCurrentVersion(tableId, 13L);
		assertNull(version);
	}

	@Test
	public void testUpdateRow() {
		currentRowCacheDao.putCurrentVersion(tableId, 12L, 1L);
		currentRowCacheDao.putCurrentVersion(tableId, 12L, 2L);
		Long version = currentRowCacheDao.getCurrentVersion(tableId, 12L);
		assertEquals(2L, version.longValue());
	}

	@Test
	public void testDeleteRow() {
		Map<Long, Long> map = Maps.newHashMap();
		map.put(13L, 133L);
		map.put(14L, 144L);
		map.put(15L, 155L);
		map.put(16L, 166L);
		map.put(17L, 177L);
		currentRowCacheDao.putCurrentVersions(tableId, map, null);
		ArrayList<Long> allRowIdsAndMore = Lists.newArrayList(12L, 13L, 14L, 15L, 16L, 17L, 18L);
		assertEquals(map, currentRowCacheDao.getCurrentVersions(tableId, allRowIdsAndMore));
		currentRowCacheDao.deleteCurrentVersion(tableId, 12L);
		assertEquals(map, currentRowCacheDao.getCurrentVersions(tableId, allRowIdsAndMore));
		currentRowCacheDao.deleteCurrentVersion(tableId, 13L);
		map.remove(13L);
		assertEquals(map, currentRowCacheDao.getCurrentVersions(tableId, allRowIdsAndMore));
		currentRowCacheDao.deleteCurrentVersions(tableId, Lists.newArrayList(14L, 15L));
		map.remove(14L);
		map.remove(15L);
		assertEquals(map, currentRowCacheDao.getCurrentVersions(tableId, allRowIdsAndMore));
		currentRowCacheDao.deleteCurrentTable(tableId);
		assertEquals(0, currentRowCacheDao.getCurrentVersions(tableId, allRowIdsAndMore).size());
	}

	@Test
	public void testTruncate() {
		Map<Long, Long> map = Maps.newHashMap();
		map.put(13L, 133L);
		map.put(14L, 144L);
		currentRowCacheDao.putCurrentVersions(tableId, map, null);
		currentRowCacheDao.putCurrentVersions(tableId + 2, map, null);
		map.put(14L, 155L);
		currentRowCacheDao.putCurrentVersions(tableId, map, null);
		ArrayList<Long> allRowIdsAndMore = Lists.newArrayList(12L, 13L, 14L, 15L, 16L, 17L, 18L);
		assertEquals(2, currentRowCacheDao.getCurrentVersions(tableId, allRowIdsAndMore).size());
		assertEquals(2, currentRowCacheDao.getCurrentVersions(tableId + 2, allRowIdsAndMore).size());
		currentRowCacheDao.truncateAllData();
		assertEquals(0, currentRowCacheDao.getCurrentVersions(tableId, allRowIdsAndMore).size());
		assertEquals(0, currentRowCacheDao.getCurrentVersions(tableId + 2, allRowIdsAndMore).size());
	}

	@Test
	public void testLatestVersion() {
		CurrentRowCacheStatus status = currentRowCacheDao.getLatestCurrentVersionNumber(tableId);
		assertNull(status.getLatestCachedVersionNumber());
		currentRowCacheDao.setLatestCurrentVersionNumber(status, 200L);
		status = currentRowCacheDao.getLatestCurrentVersionNumber(tableId);
		assertEquals(200L, status.getLatestCachedVersionNumber().longValue());
		currentRowCacheDao.setLatestCurrentVersionNumber(status, 300L);
		status = currentRowCacheDao.getLatestCurrentVersionNumber(tableId);
		assertEquals(300L, status.getLatestCachedVersionNumber().longValue());
	}

	@Test(expected = ConflictingUpdateException.class)
	public void testLatestVersionFailsOptimisticLock() {
		CurrentRowCacheStatus status = currentRowCacheDao.getLatestCurrentVersionNumber(tableId);
		assertNull(status.getLatestCachedVersionNumber());
		currentRowCacheDao.setLatestCurrentVersionNumber(status, 200L);
		status = currentRowCacheDao.getLatestCurrentVersionNumber(tableId);
		assertEquals(200L, status.getLatestCachedVersionNumber().longValue());
		status = new CurrentRowCacheStatus(tableId, 300L, status.getRecordVersion());
		currentRowCacheDao.setLatestCurrentVersionNumber(status, 300L);
		status = new CurrentRowCacheStatus(tableId, 300L, status.getRecordVersion() - 1L);
		currentRowCacheDao.setLatestCurrentVersionNumber(status, 300L);
	}

	@Test
	public void testBatching() throws Exception {
		DynamoDBMapper mockMapper = mock(DynamoDBMapper.class);
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)), "mapper",
				mockMapper);
		Map<Long, Long> map = Maps.newHashMap();
		for (long i = 0; i < 51; i++) {
			map.put(i, 1000 + i);
		}
		currentRowCacheDao.putCurrentVersions(tableId, map, null);
		verify(mockMapper, times(3)).batchSave(anyListOf(Object.class));
	}

	@Test(expected = ProvisionedThroughputExceededException.class)
	public void testBatchingFailsEventually() throws Exception {
		TestClock testClock = new TestClock();
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)), "clock",
				testClock);

		DynamoDBMapper mockMapper = mock(DynamoDBMapper.class);
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)), "mapper",
				mockMapper);
		doThrow(new ProvisionedThroughputExceededException("dummy")).when(mockMapper).batchSave(anyListOf(Object.class));
		Map<Long, Long> map = Maps.newHashMap();
		map.put(13L, 133L);
		map.put(14L, 144L);
		currentRowCacheDao.putCurrentVersions(tableId, map, null);
	}

	@Test
	public void testBatchingProgressSuccess() throws Exception {
		DynamoDBMapper mockMapper = mock(DynamoDBMapper.class);
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)), "mapper",
				mockMapper);
		Map<Long, Long> map = Maps.newHashMap();
		for (long i = 0; i < 51; i++) {
			map.put(i, 1000 + i);
		}
		final AtomicLong count = new AtomicLong(0);
		currentRowCacheDao.putCurrentVersions(tableId, map, new ProgressCallback<Long>() {
			@Override
			public void progressMade(Long message) {
				count.incrementAndGet();
			}
		});
		verify(mockMapper, times(3)).batchSave(anyListOf(Object.class));
		assertEquals(3L, count.get());
	}

	@Test
	public void testBatchingProgressRetry() throws Exception {
		DynamoDBMapper mockMapper = mock(DynamoDBMapper.class);
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)), "mapper",
				mockMapper);
		TestClock testClock = new TestClock();
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)), "clock",
				testClock);
		Random mockRandom = mock(Random.class);
		when(mockRandom.nextInt(30000)).thenReturn(20000);
		when(mockRandom.nextInt(120000)).thenReturn(80000);
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)), "random",
				mockRandom);
		doThrow(new ProvisionedThroughputExceededException("dummy")).doThrow(new ProvisionedThroughputExceededException("dummy"))
				.doThrow(new ProvisionedThroughputExceededException("dummy")).doNothing().when(mockMapper).batchSave(anyListOf(Object.class));
		Map<Long, Long> map = Maps.newHashMap();
		map.put(13L, 133L);
		map.put(14L, 144L);
		final AtomicLong count = new AtomicLong(0);
		currentRowCacheDao.putCurrentVersions(tableId, map, new ProgressCallback<Long>() {
			@Override
			public void progressMade(Long message) {
				count.incrementAndGet();
			}
		});
		verify(mockMapper, times(4)).batchSave(anyListOf(Object.class));
		assertEquals(7L, count.get());
	}

	@Test
	public void testBatchingProgress() throws Exception {
		DynamoDBMapper mockMapper = mock(DynamoDBMapper.class);
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)), "mapper",
				mockMapper);
		Map<Long, Long> map = Maps.newHashMap();
		for (long i = 0; i < 51; i++) {
			map.put(i, 1000 + i);
		}
		final AtomicLong messageRef = new AtomicLong(0);
		currentRowCacheDao.putCurrentVersions(tableId, map, new ProgressCallback<Long>() {
			@Override
			public void progressMade(Long message) {
				messageRef.set(message);
			}
		});
		verify(mockMapper, times(3)).batchSave(anyListOf(Object.class));
		assertEquals(51L, messageRef.get());
	}

	@Test
	public void testBackoff() throws Exception {
		DynamoDBMapper mockMapper = mock(DynamoDBMapper.class);
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)), "mapper",
				mockMapper);
		Random mockRandom = mock(Random.class);
		when(mockRandom.nextInt(5000)).thenReturn(5000);
		when(mockRandom.nextInt(20000)).thenReturn(15000);
		when(mockRandom.nextInt(45000)).thenReturn(30000);
		when(mockRandom.nextInt(80000)).thenReturn(40000);
		when(mockRandom.nextInt(125000)).thenReturn(150000);
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)), "random",
				mockRandom);
		TestClock testClock = new TestClock();
		long start = testClock.currentTimeMillis();
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)), "clock",
				testClock);
		doThrow(new ProvisionedThroughputExceededException("dummy")).doThrow(new ProvisionedThroughputExceededException("dummy"))
				.doThrow(new ProvisionedThroughputExceededException("dummy")).doThrow(new ProvisionedThroughputExceededException("dummy"))
				.doThrow(new ProvisionedThroughputExceededException("dummy")).doNothing().when(mockMapper).batchSave(anyListOf(Object.class));
		Map<Long, Long> map = Maps.newHashMap();
		map.put(13L, 133L);
		map.put(14L, 144L);
		currentRowCacheDao.putCurrentVersions(tableId, map, null);
		verify(mockMapper, times(6)).batchSave(anyListOf(Object.class));
		assertEquals(790000L + 605L, testClock.currentTimeMillis() - start);
	}

	@Test
	public void testThroughputUpdating() throws Exception {
		AmazonDynamoDB mockDynamoClient = mock(AmazonDynamoDB.class);
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)),
				"dynamoClient", mockDynamoClient);
		when(mockDynamoClient.describeTable(any(DescribeTableRequest.class))).thenReturn(
				new DescribeTableResult().withTable(new TableDescription().withProvisionedThroughput(new ProvisionedThroughputDescription()
						.withWriteCapacityUnits(50L))));

		TestClock testClock = new TestClock();
		// set test clock to current time, because the throughput was updated at bean initialization, and the last
		// update time was marked at that time
		testClock.setCurrentTime(System.currentTimeMillis());
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)), "clock",
				testClock);
		Map<Long, Long> map = Maps.newHashMap();
		map.put(13L, 133L);
		map.put(14L, 144L);

		long start = testClock.currentTimeMillis();
		currentRowCacheDao.putCurrentVersions(tableId, map, null);
		// should be 1000 / 5 * 3 + 5
		assertEquals(605L, testClock.currentTimeMillis() - start);

		// check is every 20 minutes
		testClock.warpForward(1200000);
		start = testClock.currentTimeMillis();
		currentRowCacheDao.putCurrentVersions(tableId, map, null);
		// should be 1000 / 50 * 3 + 5
		assertEquals(65L, testClock.currentTimeMillis() - start);

		// check is every 20 minutes, reset the write throughput value
		ReflectionTestUtils.setField(((CurrentRowCacheDaoImpl) ReflectionStaticTestUtils.getTargetObject(currentRowCacheDao)),
				"dynamoClient", oldDynamoClient);
		testClock.warpForward(1200000);
		start = testClock.currentTimeMillis();
		currentRowCacheDao.putCurrentVersions(tableId, map, null);
		// should be 1000 / 50 * 3 + 5
		assertEquals(605L, testClock.currentTimeMillis() - start);
	}

	@Ignore
	@Test
	public void loadTest() throws Exception {
		final int COUNT = 1;
		final int SIZE = 4000;
		final int REPEAT = 15;
		ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(COUNT);
		List<Future<Long>> tasks = Lists.newArrayList();
		for(int i = 0; i < COUNT; i++){
			final int index = i;
			tasks.add(newFixedThreadPool.submit(new Callable<Long>() {
				@Override
				public Long call() throws Exception {
					Map<Long, Long> map = Maps.newHashMap();
					for (long i = 0; i < SIZE; i++) {
						map.put(100000L * index + i, 100000L * index + i + 100000000L);
					}
					final AtomicLong maxTime = new AtomicLong(0);
					for (int i = 0; i < REPEAT; i++) {
						System.err.println(Thread.currentThread().getId() + " " + i);
						long start = System.currentTimeMillis();
						currentRowCacheDao.putCurrentVersions(tableId, map, new ProgressCallback<Long>() {
							long lastTime = System.currentTimeMillis();

							@Override
							public void progressMade(Long message) {
								long diff = System.currentTimeMillis() - lastTime;
								lastTime = System.currentTimeMillis();
								maxTime.set(Math.max(maxTime.get(), diff));
							}
						});
						System.out.println("took " + ((System.currentTimeMillis() - start) / 1000) + " seconds");
					}
					return maxTime.get();
				}
			}));
		}

		for (Future<Long> task : tasks) {
			long maxTime = task.get();
			System.out.println("Maxtime: " + maxTime);
		}
	}
}
