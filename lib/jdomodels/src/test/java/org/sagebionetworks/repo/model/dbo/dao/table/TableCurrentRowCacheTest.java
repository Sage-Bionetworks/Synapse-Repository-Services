package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dao.table.RowSetAccessor;
import org.sagebionetworks.repo.model.dao.table.TableRowCache;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ReflectionStaticTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-table-index-test-context.xml" })
public class TableCurrentRowCacheTest {

	private Set<Long> ALL_SET;

	@Autowired
	ConnectionFactory connectionFactory;

	@Autowired
	private TableRowTruthDAO tableRowTruthDao;

	@Autowired
	private TableRowCache tableRowCache;

	private TableIndexDAO tableIndexDao;

	protected String creatorUserGroupId;

	Object oldStackConfiguration;

	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception {
		Assume.assumeTrue(StackConfiguration.singleton().getTableEnabled());
		ALL_SET = mock(Set.class);
		when(ALL_SET.contains(any())).thenReturn(true);
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
		assertNotNull(creatorUserGroupId);
		oldStackConfiguration = ReflectionStaticTestUtils.getField(ReflectionStaticTestUtils.getField(tableRowTruthDao, "tableRowCache"),
				"stackConfiguration");
		StackConfiguration mockStackConfiguration = mock(StackConfiguration.class);
		when(mockStackConfiguration.getTableEnabled()).thenReturn(false);
		ReflectionStaticTestUtils.setField(ReflectionStaticTestUtils.getField(tableRowTruthDao, "tableRowCache"), "stackConfiguration",
				mockStackConfiguration);
		tableIndexDao = connectionFactory.getConnection("123");
		tableRowTruthDao.truncateAllRowData();
		tableIndexDao.deleteStatusTable("123");
		tableIndexDao.deleteTable("123");
	}

	@After
	public void after() throws Exception {
		if (!StackConfiguration.singleton().getTableEnabled()) {
			return;
		}
		tableRowTruthDao.truncateAllRowData();
		tableIndexDao.deleteStatusTable("123");
		tableIndexDao.deleteTable("123");
		ReflectionStaticTestUtils.setField(ReflectionStaticTestUtils.getField(tableRowTruthDao, "tableRowCache"), "stackConfiguration",
				oldStackConfiguration);
	}

	@Test
	public void testRowCache() throws Exception {
		String tableId = "syn123";
		// Create some test column models
		ColumnMapper mapper = TableModelTestUtils.createMapperForOneOfEachType();
		// create some test rows.
		List<Row> rows = TableModelTestUtils.createRows(mapper.getColumnModels(), 5, false);
		// Append this change set
		RowReferenceSet refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, mapper,
				new RawRowSet(TableModelUtils.getIds(mapper.getColumnModels()), null, tableId, rows));
		assertNotNull(refSet);
		RowSet rowSet = tableRowTruthDao.getRowSet(refSet, mapper);
		assertEquals(5, rowSet.getRows().size());

		Function<Row, Long> rowToRowId = new Function<Row, Long>() {
			@Override
			public Long apply(Row input) {
				return input.getRowId();
			}
		};

		Long rowIdZero = rowSet.getRows().get(0).getRowId();
		Long rowVersionZero = rowSet.getRows().get(0).getVersionNumber();
		Long headerIdZero = Long.parseLong(rowSet.getHeaders().get(0).getId());
		String valueZero = rowSet.getRows().get(0).getValues().get(0);

		// no data in index
		RowSetAccessor latestRows = tableRowTruthDao.getLatestVersionsWithRowData(tableId,
				Sets.newHashSet(Lists.transform(rowSet.getRows(), rowToRowId)), -1, mapper);
		assertEquals(valueZero, latestRows.getRow(rowIdZero).getCellById(headerIdZero));
		assertEquals(rowVersionZero, latestRows.getRow(rowIdZero).getVersionNumber());

		// Create the table
		tableIndexDao.createOrUpdateTable(mapper.getColumnModels(), tableId);
		// Now fill the table with data
		tableIndexDao.createOrUpdateOrDeleteRows(rowSet, mapper.getColumnModels());
		// And set the max version
		tableIndexDao.setMaxCurrentCompleteVersionForTable(tableId, rowSet.getRows().get(0).getVersionNumber());

		List<Map<String, Object>> result = tableIndexDao.getConnection().queryForList(
				"SELECT * FROM " + SQLUtils.getTableNameForId(tableId, TableType.INDEX));
		assertEquals(5, result.size());
		assertEquals(valueZero, rowSet.getRows().get(0).getValues().get(0));
		assertEquals(rowVersionZero, rowSet.getRows().get(0).getVersionNumber());

		// index is up to date
		latestRows = tableRowTruthDao.getLatestVersionsWithRowData(tableId,
				Sets.newHashSet(Lists.transform(rowSet.getRows(), rowToRowId)), -1, mapper);
		assertEquals(valueZero, latestRows.getRow(rowIdZero).getCellById(headerIdZero));
		assertEquals(rowVersionZero, latestRows.getRow(rowIdZero).getVersionNumber());

		// update some rows (but not the index)
		Row unchangedRow = rowSet.getRows().remove(4);
		for (int i = 0; i < 3; i++) {
			TableModelTestUtils.updateRow(mapper.getColumnModels(), rowSet.getRows().get(i), i);
		}
		refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, mapper,
				new RawRowSet(TableModelUtils.getIds(mapper.getColumnModels()), null, tableId, rowSet.getRows()));
		refSet.getRows().add(TableModelTestUtils.createRowReference(unchangedRow.getRowId(), unchangedRow.getVersionNumber()));
		RowSet updatedRowSet = tableRowTruthDao.getRowSet(refSet, mapper);
		assertEquals(5, updatedRowSet.getRows().size());

		Long updatedRowVersionZero = null;
		String updatedValueZero = null;
		for (Row row : updatedRowSet.getRows()) {
			if (row.getRowId().equals(rowIdZero)) {
				updatedRowVersionZero = row.getVersionNumber();
				updatedValueZero = row.getValues().get(0);
				break;
			}
		}

		// index is behind
		latestRows = tableRowTruthDao.getLatestVersionsWithRowData(tableId,
				Sets.newHashSet(Lists.transform(updatedRowSet.getRows(), rowToRowId)), -1, mapper);
		assertEquals(updatedValueZero, latestRows.getRow(rowIdZero).getCellById(headerIdZero));
		assertEquals(updatedRowVersionZero, latestRows.getRow(rowIdZero).getVersionNumber());

		// Now update the table with data
		tableIndexDao.createOrUpdateOrDeleteRows(updatedRowSet, mapper.getColumnModels());
		// And set the max version
		tableIndexDao.setMaxCurrentCompleteVersionForTable(tableId, updatedRowSet.getRows().get(0).getVersionNumber());

		result = tableIndexDao.getConnection().queryForList("SELECT * FROM " + SQLUtils.getTableNameForId(tableId, TableType.INDEX));
		assertEquals(5, result.size());
		assertEquals(updatedValueZero, rowSet.getRows().get(0).getValues().get(0));
		assertEquals(updatedRowVersionZero, rowSet.getRows().get(0).getVersionNumber());

		// index is up to date again
		latestRows = tableRowTruthDao.getLatestVersionsWithRowData(tableId,
				Sets.newHashSet(Lists.transform(updatedRowSet.getRows(), rowToRowId)), -1, mapper);
		assertEquals(updatedValueZero, latestRows.getRow(rowIdZero).getCellById(headerIdZero));
		assertEquals(updatedRowVersionZero, latestRows.getRow(rowIdZero).getVersionNumber());
	}
}
