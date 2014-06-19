package org.sagebionetworks.dynamo.dao.rowcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.table.CurrentRowCacheStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.dynamodb.model.ConditionalCheckFailedException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class CurrentRowCacheDaoAutowireTest {

	@Autowired
	private CurrentRowCacheDao currentRowCacheDao;

	private Long tableId = new Random().nextLong();

	@BeforeClass
	public static void beforeClass() {
		StackConfiguration config = new StackConfiguration();
		Assume.assumeTrue(config.getDynamoEnabled());
	}

	@Before
	public void setup() {
		currentRowCacheDao.deleteCurrentTable(tableId);
	}

	@After
	public void destroy() {
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
		currentRowCacheDao.putCurrentVersions(tableId, map);

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
		currentRowCacheDao.putCurrentVersions(tableId, map);

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
		currentRowCacheDao.putCurrentVersions(tableId, map);

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
		currentRowCacheDao.putCurrentVersions(tableId, map);
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
		currentRowCacheDao.putCurrentVersions(tableId, map);
		currentRowCacheDao.putCurrentVersions(tableId + 2, map);
		map.put(14L, 155L);
		currentRowCacheDao.putCurrentVersions(tableId, map);
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

	@Test(expected = ConditionalCheckFailedException.class)
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
}
