package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.dynamo.dao.rowcache.CurrentRowCacheDao;
import org.sagebionetworks.dynamo.dao.rowcache.CurrentRowCacheDaoStub;
import org.sagebionetworks.dynamo.dao.rowcache.RowCacheDao;
import org.sagebionetworks.dynamo.dao.rowcache.RowCacheDaoStub;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Iterables;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class TableRowTruthDAOImplWithCacheTest extends TableRowTruthDAOImplTest {

	@Autowired
	CurrentRowCacheDao currentRowCacheDao;

	@Autowired
	RowCacheDao rowCacheDao;

	@Autowired
	private TableRowTruthDAO tableRowTruthDao;

	@Before
	public void enableCache(){
		((CurrentRowCacheDaoStub) currentRowCacheDao).isEnabled = true;
		((RowCacheDaoStub) rowCacheDao).isEnabled = true;
		tableRowTruthDao.truncateAllRowData();
	}

	@After
	public void disableCache() {
		tableRowTruthDao.truncateAllRowData();
		((CurrentRowCacheDaoStub) currentRowCacheDao).isEnabled = false;
		((RowCacheDaoStub) rowCacheDao).isEnabled = false;
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
		RowReferenceSet refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, set, false);
		assertEquals(0, ((CurrentRowCacheDaoStub) currentRowCacheDao).latestVersionNumbers.size());

		rows = TableModelTestUtils.createRows(models, 1);
		rows.get(0).setRowId(refSet.getRows().get(0).getRowId());
		rows.get(0).setVersionNumber(refSet.getRows().get(0).getVersionNumber());
		set.setRows(rows);
		set.setEtag(refSet.getEtag());
		RowReferenceSet refs = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, set, false);
		assertEquals(1, ((CurrentRowCacheDaoStub) currentRowCacheDao).latestVersionNumbers.size());
		assertEquals(5, Iterables.getOnlyElement(((CurrentRowCacheDaoStub) currentRowCacheDao).latestVersionNumbers.values()).size());
		
		tableRowTruthDao.getLatestVersions(tableId, Collections.<Long> emptySet(), "etag");
		assertEquals(1, ((CurrentRowCacheDaoStub) currentRowCacheDao).latestVersionNumbers.size());
		assertEquals(5, Iterables.getOnlyElement(((CurrentRowCacheDaoStub) currentRowCacheDao).latestVersionNumbers.values()).size());

		assertEquals(0, ((RowCacheDaoStub) rowCacheDao).rows.values().size());
		tableRowTruthDao.getRowSetOriginals(refs);
		assertEquals(1, ((RowCacheDaoStub) rowCacheDao).rows.values().size());
		tableRowTruthDao.getRowSetOriginals(refs);
		assertEquals(1, ((RowCacheDaoStub) rowCacheDao).rows.values().size());
	}
}
