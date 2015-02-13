package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dao.table.CurrentVersionCacheDao;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ReflectionStaticTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Iterables;

public class TableRowTruthDAOImplWithCacheTest extends TableRowTruthDAOImplTest {

	@Autowired
	ConnectionFactory connectionFactory;

	@Autowired
	private TableRowTruthDAO tableRowTruthDao;

	Object oldStackConfiguration;

	@Before
	public void enableCache() throws Exception {
		oldStackConfiguration = ReflectionStaticTestUtils.getField(ReflectionStaticTestUtils.getField(tableRowTruthDao, "tableRowCache"),
				"stackConfiguration");
		StackConfiguration mockStackConfiguration = mock(StackConfiguration.class);
		when(mockStackConfiguration.getTableEnabled()).thenReturn(true);
		ReflectionStaticTestUtils.setField(ReflectionStaticTestUtils.getField(tableRowTruthDao, "tableRowCache"), "stackConfiguration",
				mockStackConfiguration);
		((ConnectionFactoryStub) connectionFactory).isEnabled = true;
		tableRowTruthDao.truncateAllRowData();
	}

	@After
	public void disableCache() throws Exception {
		tableRowTruthDao.truncateAllRowData();
		((ConnectionFactoryStub) connectionFactory).isEnabled = false;
		ReflectionStaticTestUtils.setField(ReflectionStaticTestUtils.getField(tableRowTruthDao, "tableRowCache"), "stackConfiguration",
				oldStackConfiguration);
	}

	@Test
	public void testCacheIsUsed() throws Exception {
		// Create some test column models
		ColumnMapper mapper = TableModelTestUtils.createMapperForOneOfEachType();
		// create some test rows.
		List<Row> rows = TableModelTestUtils.createRows(mapper.getColumnModels(), 5);
		String tableId = "syn123";
		RawRowSet set = new RawRowSet(TableModelUtils.getIds(mapper.getColumnModels()), null, tableId, rows);
		// Append this change set
		CurrentVersionCacheDao currentRowCacheDao = connectionFactory.getCurrentVersionCacheConnection(KeyFactory.stringToKey(tableId));
		RowReferenceSet refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, mapper, set);
		assertEquals(0, ((CurrentVersionCacheDaoStub) currentRowCacheDao).latestVersionNumbers.size());
		tableRowTruthDao.updateLatestVersionCache(tableId, null);

		rows = TableModelTestUtils.createRows(mapper.getColumnModels(), 1);
		rows.get(0).setRowId(refSet.getRows().get(0).getRowId());
		rows.get(0).setVersionNumber(refSet.getRows().get(0).getVersionNumber());
		set = new RawRowSet(TableModelUtils.getIds(mapper.getColumnModels()), refSet.getEtag(), tableId, rows);
		RowReferenceSet refs = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, mapper, set);
		assertEquals(1, ((CurrentVersionCacheDaoStub) currentRowCacheDao).latestVersionNumbers.size());
		assertEquals(5, Iterables.getOnlyElement(((CurrentVersionCacheDaoStub) currentRowCacheDao).latestVersionNumbers.values()).size());
		
		tableRowTruthDao.getLatestVersionsWithRowData(tableId, Collections.<Long> emptySet(), 0L, mapper);
		assertEquals(1, ((CurrentVersionCacheDaoStub) currentRowCacheDao).latestVersionNumbers.size());
		assertEquals(5, Iterables.getOnlyElement(((CurrentVersionCacheDaoStub) currentRowCacheDao).latestVersionNumbers.values()).size());

		tableRowTruthDao.getRowSetOriginals(refs, mapper);
	}

	@Test(expected = TableUnavilableException.class)
	public void testCacheBehindCheck() throws Exception {
		super.testCacheBehindCheck();
	}

}
