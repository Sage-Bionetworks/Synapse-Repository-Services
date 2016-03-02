package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class CachingTableRowTruthDAOImplTest {

	@Autowired
	private TableRowTruthDAO tableRowTruthDao;
	
	protected String creatorUserGroupId;
	
	@Before
	public void before(){
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
	}
	
	@After
	public void after() throws Exception {
		if(tableRowTruthDao != null) {
			tableRowTruthDao.truncateAllRowData();
		}
	}
	
	@Test
	public void testCheckForRowLevelConflict() throws IOException{
		// Create some test column models
		ColumnMapper mapper = TableModelTestUtils.createMapperForOneOfEachType();
		// create some test rows.
		List<Row> rows = TableModelTestUtils.createRows(mapper.getColumnModels(), 5, false);
		String tableId = "syn123";
		RawRowSet set = new RawRowSet(TableModelUtils.getIds(mapper.getColumnModels()), null, tableId, rows);
		// Append this change set
		RowReferenceSet refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, mapper, set);
		// Fetch the rows back.
		List<RawRowSet> rawList = tableRowTruthDao.getRowSetOriginals(refSet, mapper);
		assertNotNull(rawList);
		assertEquals(1, rawList.size());
		RawRowSet updatedSet = rawList.get(0);
		RawRowSet updatedSetNoEtag = new RawRowSet(updatedSet.getIds(), null, tableId, updatedSet.getRows());
		// should pass since all match. 
		tableRowTruthDao.checkForRowLevelConflict(tableId, updatedSet);
		// It should also work without the etag
		tableRowTruthDao.checkForRowLevelConflict(tableId, updatedSetNoEtag);
		// Append the same changes to the table again
		refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, mapper, set);
		 // Now if we try to use the original set it should fail with a conflict
		try {
			tableRowTruthDao.checkForRowLevelConflict(tableId, updatedSet);
			fail("Should have failed as there are conflicts.");
		} catch (ConflictingUpdateException e) {
			// expected
		}
		// Should also fail without an etag
		try {
			tableRowTruthDao.checkForRowLevelConflict(tableId, updatedSetNoEtag);
			fail("Should have failed as there are conflicts.");
		} catch (ConflictingUpdateException e) {
			// expected
		}
	}
	
	/**
	 * This is a test for PLFM-3355
	 * @throws IOException
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testCheckForRowLevelConflictNullVersionNumber() throws IOException{
		// Create some test column models
		ColumnMapper mapper = TableModelTestUtils.createMapperForOneOfEachType();
		// create some test rows.
		List<Row> rows = TableModelTestUtils.createRows(mapper.getColumnModels(), 1, false);
		String tableId = "syn123";
		RawRowSet set = new RawRowSet(TableModelUtils.getIds(mapper.getColumnModels()), null, tableId, rows);
		// Append this change set
		RowReferenceSet refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, mapper, set);
		// Fetch the rows back.
		List<RawRowSet> rawList = tableRowTruthDao.getRowSetOriginals(refSet, mapper);
		assertNotNull(rawList);
		assertEquals(1, rawList.size());
		RawRowSet updatedSet = rawList.get(0);
		RawRowSet updatedSetNoEtag = new RawRowSet(updatedSet.getIds(), null, tableId, updatedSet.getRows());
		updatedSetNoEtag.getRows().get(0).setVersionNumber(null);
		// This should fail as a null version number is passed in. 
		tableRowTruthDao.checkForRowLevelConflict(tableId, updatedSetNoEtag);
	}
	
}
