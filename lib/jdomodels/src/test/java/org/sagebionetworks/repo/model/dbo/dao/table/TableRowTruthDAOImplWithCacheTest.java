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
import org.sagebionetworks.dynamo.dao.rowcache.RowCacheDao;
import org.sagebionetworks.dynamo.dao.rowcache.RowCacheDaoStub;
import org.sagebionetworks.repo.model.dao.table.CurrentRowCacheDao;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
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
	RowCacheDao rowCacheDao;

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
		((RowCacheDaoStub) rowCacheDao).isEnabled = true;
		((ConnectionFactoryStub) connectionFactory).isEnabled = true;
		tableRowTruthDao.truncateAllRowData();
	}

	@After
	public void disableCache() throws Exception {
		tableRowTruthDao.truncateAllRowData();
		((ConnectionFactoryStub) connectionFactory).isEnabled = false;
		((RowCacheDaoStub) rowCacheDao).isEnabled = false;
		ReflectionStaticTestUtils.setField(ReflectionStaticTestUtils.getField(tableRowTruthDao, "tableRowCache"), "stackConfiguration",
				oldStackConfiguration);
	}

	@Test
	public void testCacheIsUsed() throws Exception {
		// Create some test column models
		List<ColumnModel> models = TableModelTestUtils.createOneOfEachType();
		// create some test rows.
		List<Row> rows = TableModelTestUtils.createRows(models, 5);
		String tableId = "syn123";
		RowSet set = new RowSet();
		set.setHeaders(TableModelUtils.getHeaders(models));
		set.setRows(rows);
		set.setTableId(tableId);
		// Append this change set
		CurrentRowCacheDao currentRowCacheDao = connectionFactory.getCurrentRowCacheConnection(KeyFactory.stringToKey(tableId));
		RowReferenceSet refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, set);
		assertEquals(0, ((CurrentRowCacheDaoStub) currentRowCacheDao).latestVersionNumbers.size());
		tableRowTruthDao.updateLatestVersionCache(tableId, null);

		rows = TableModelTestUtils.createRows(models, 1);
		rows.get(0).setRowId(refSet.getRows().get(0).getRowId());
		rows.get(0).setVersionNumber(refSet.getRows().get(0).getVersionNumber());
		set.setRows(rows);
		set.setEtag(refSet.getEtag());
		RowReferenceSet refs = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, set);
		assertEquals(1, ((CurrentRowCacheDaoStub) currentRowCacheDao).latestVersionNumbers.size());
		assertEquals(5, Iterables.getOnlyElement(((CurrentRowCacheDaoStub) currentRowCacheDao).latestVersionNumbers.values()).size());
		
		tableRowTruthDao.getLatestVersionsWithRowData(tableId, Collections.<Long> emptySet(), 0L);
		assertEquals(1, ((CurrentRowCacheDaoStub) currentRowCacheDao).latestVersionNumbers.size());
		assertEquals(5, Iterables.getOnlyElement(((CurrentRowCacheDaoStub) currentRowCacheDao).latestVersionNumbers.values()).size());

		assertEquals(0, ((RowCacheDaoStub) rowCacheDao).rows.values().size());
		tableRowTruthDao.getRowSetOriginals(refs);
		assertEquals(1, ((RowCacheDaoStub) rowCacheDao).rows.values().size());
		tableRowTruthDao.getRowSetOriginals(refs);
		assertEquals(1, ((RowCacheDaoStub) rowCacheDao).rows.values().size());
	}

	@Test(expected = TableUnavilableException.class)
	public void testCacheBehindCheck() throws Exception {
		super.testCacheBehindCheck();
	}

}
