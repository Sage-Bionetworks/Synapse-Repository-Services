package org.sagebionetworks.dynamo.dao.rowcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.dynamo.dao.DynamoAdminDao;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:dynamo-dao-spb.xml" })
public class RowCacheDaoAutowireTest {

	@Autowired
	private RowCacheDao rowCacheDao;
	@Autowired
	private DynamoAdminDao adminDao;

	private String tableName = "t" + new Random().nextLong();

	@Before
	public void setup() {
		StackConfiguration config = new StackConfiguration();
		Assume.assumeTrue(config.getDynamoEnabled());

		adminDao.clear(DboRowCache.TABLE_NAME, DboRowCache.HASH_KEY_NAME, DboRowCache.RANGE_KEY_NAME);
	}

	@Test
	public void testRoundTrip() throws Exception {
		Row row = TableModelTestUtils.createRow(12L, 1L, "a", "b");
		rowCacheDao.putRow(tableName, row);
		Row result = rowCacheDao.getRow(tableName, 12L, 1L);
		assertEquals(row, result);
	}

	@Test
	public void testRoundTripMultiple() throws Exception {
		Row row1 = TableModelTestUtils.createRow(11L, 1L, "a", "b");
		Row row2a = TableModelTestUtils.createRow(12L, 1L, "a", "b");
		Row row2 = TableModelTestUtils.createRow(12L, 2L, "c", "d");
		Row row3 = TableModelTestUtils.createRow(13L, 2L, "e", "f");
		rowCacheDao.putRow(tableName, row1);
		rowCacheDao.putRow(tableName, row2a);
		rowCacheDao.putRows(tableName, Lists.newArrayList(row2, row3));

		Map<Long, Long> rowsToGet = Maps.newHashMap();
		rowsToGet.put(11L, 1L);
		rowsToGet.put(12L, 2L);
		rowsToGet.put(13L, 2L);
		Map<Long, Row> result = rowCacheDao.getRows(tableName, rowsToGet);
		assertEquals(3, result.size());
		assertEquals(row1, result.get(11L));
		assertEquals(row2, result.get(12L));
		assertEquals(row3, result.get(13L));
	}

	@Test
	public void testGetNonExistentRow() throws Exception {
		Row row = rowCacheDao.getRow(tableName, 16L, 18L);
		assertNull(row);
	}

	@Test
	public void testGetNonExistentRows() throws Exception {
		Row row1 = TableModelTestUtils.createRow(12L, 1L, "a", "b");
		rowCacheDao.putRow(tableName, row1);
		Map<Long, Long> rowsToGet = Maps.newHashMap();
		rowsToGet.put(11L, 1L);
		rowsToGet.put(12L, 1L);
		rowsToGet.put(13L, 2L);
		Map<Long, Row> result = rowCacheDao.getRows(tableName, rowsToGet);
		assertEquals(1, result.size());
		assertEquals(row1, Iterables.getOnlyElement(result.entrySet()).getValue());
	}

	@Test
	public void testUpdateRow() throws Exception {
		Row row = TableModelTestUtils.createRow(12L, 1L, "a", "b");
		rowCacheDao.putRow(tableName, row);
		row.setValues(Lists.newArrayList("d", "b"));
		rowCacheDao.putRow(tableName, row);
		Row result = rowCacheDao.getRow(tableName, 12L, 1L);
		assertEquals(row, result);
	}

	@Test
	public void testNullValues() throws Exception {
		Row row1 = TableModelTestUtils.createRow(11L, 1L);
		row1.setValues(null);
		Row row2 = TableModelTestUtils.createRow(12L, 2L);
		Row row3 = TableModelTestUtils.createRow(13L, 3L, (String) null);
		Row row4 = TableModelTestUtils.createRow(14L, 4L, null, "a");
		Row row5 = TableModelTestUtils.createRow(15L, 5L, null, null);
		Row row6 = TableModelTestUtils.createRow(16L, 6L, "b", null);
		ArrayList<Row> rows = Lists.newArrayList(row1, row2, row3, row4, row5, row6);
		rowCacheDao.putRows(tableName, rows);

		Map<Long, Long> rowsToGet = Maps.newHashMap();
		rowsToGet.put(11L, 1L);
		rowsToGet.put(12L, 2L);
		rowsToGet.put(13L, 3L);
		rowsToGet.put(14L, 4L);
		rowsToGet.put(15L, 5L);
		rowsToGet.put(16L, 6L);
		Map<Long, Row> result = rowCacheDao.getRows(tableName, rowsToGet);
		assertEquals(6, result.size());
		for (Row row : rows) {
			assertEquals(row, result.get(row.getRowId()));
		}
	}
}
