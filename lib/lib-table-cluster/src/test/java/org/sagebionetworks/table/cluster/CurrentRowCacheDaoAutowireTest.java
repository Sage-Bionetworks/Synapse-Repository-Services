package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
import org.sagebionetworks.repo.model.dao.table.CurrentRowCacheDao;
import org.sagebionetworks.util.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:table-cluster-spb.xml" })
public class CurrentRowCacheDaoAutowireTest {

	@Autowired
	private ConnectionFactory tableConnectionFactory;

	private CurrentRowCacheDao currentRowCacheDao;

	private Long tableId = (long) new Random().nextInt(10000000);

	@BeforeClass
	public static void beforeClass() {
		Assume.assumeTrue(StackConfiguration.singleton().getTableEnabled());
	}

	@Before
	public void setup() throws Exception {
		// First get a connection for this table
		currentRowCacheDao = tableConnectionFactory.getCurrentRowCacheConnection(tableId);

		currentRowCacheDao.deleteCurrentTable(tableId);
	}

	@After
	public void destroy() throws Exception {
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

	@Ignore
	@Test
	public void loadTest() throws Exception {
		final int COUNT = 5;
		final int SIZE = 4000;
		final int REPEAT = 3;
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
