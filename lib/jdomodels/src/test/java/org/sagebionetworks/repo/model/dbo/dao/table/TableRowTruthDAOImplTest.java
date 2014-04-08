package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.dao.table.RowAccessor;
import org.sagebionetworks.repo.model.dao.table.RowSetAccessor;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class TableRowTruthDAOImplTest {
	
	@Autowired
	private TableRowTruthDAO tableRowTruthDao;
		
	private String creatorUserGroupId;

	@Before
	public void before(){
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
		assertNotNull(creatorUserGroupId);
	}
	
	@After
	public void after(){
		if(tableRowTruthDao != null) tableRowTruthDao.truncateAllRowData();
	}

	@Test
	public void testReserveIdsInRange(){
		String tableId = "123";
		IdRange range = tableRowTruthDao.reserveIdsInRange(tableId, 3);
		assertNotNull(range);
		assertEquals(new Long(0), range.getMinimumId());
		assertEquals(new Long(2), range.getMaximumId());
		assertEquals(new Long(0), range.getVersionNumber());
		assertTrue(range.getMaximumUpdateId() < 0);
		assertNotNull(range.getEtag());
		// Now reserver 1 more
		range = tableRowTruthDao.reserveIdsInRange(tableId, 1);
		assertNotNull(range);
		assertEquals(new Long(3), range.getMinimumId());
		assertEquals(new Long(3), range.getMaximumId());
		assertEquals(new Long(1), range.getVersionNumber());
		assertEquals(new Long(2), range.getMaximumUpdateId());
		assertNotNull(range.getEtag());
		// two more
		range = tableRowTruthDao.reserveIdsInRange(tableId, 2);
		assertNotNull(range);
		assertEquals(new Long(4), range.getMinimumId());
		assertEquals(new Long(5), range.getMaximumId());
		assertEquals(new Long(2), range.getVersionNumber());
		assertEquals(new Long(3), range.getMaximumUpdateId());
		assertNotNull(range.getEtag());
		// zero
		range = tableRowTruthDao.reserveIdsInRange(tableId, 0);
		assertNotNull(range);
		assertEquals(null, range.getMinimumId());
		assertEquals(null, range.getMaximumId());
		assertEquals(new Long(5), range.getMaximumUpdateId());
		assertEquals(new Long(3), range.getVersionNumber());
		assertNotNull(range.getEtag());
	}
	
	@Test
	public void testAppendRows() throws Exception {
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
		RowReferenceSet refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, set);
		assertNotNull(refSet);
		// Validate each row has an ID
		assertEquals(set.getHeaders(), refSet.getHeaders());
		assertEquals(tableId, refSet.getTableId());
		assertNotNull(refSet.getRows());
		assertEquals(set.getRows().size(), refSet.getRows().size());
		long expectedId = 0;
		Long version = new Long(0);
		for(RowReference ref: refSet.getRows()){
			assertEquals(new Long(expectedId), ref.getRowId());
			assertEquals(version, ref.getVersionNumber());
			expectedId++;
		}
		assertEquals(set, tableRowTruthDao.getRowSet(refSet, models));
	}
	
	@Test
	public void testNullValues() throws Exception {
		// Create some test column models
		List<ColumnModel> models = TableModelTestUtils.createOneOfEachType();
		// create some test rows.
		List<Row> rows = TableModelTestUtils.createNullRows(models, 5);

		String tableId = "syn123";
		RowSet set = new RowSet();
		set.setHeaders(TableModelUtils.getHeaders(models));
		set.setRows(rows);
		set.setTableId(tableId);
		// Append this change set
		RowReferenceSet refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, set);
		assertEquals(set, tableRowTruthDao.getRowSet(refSet, models));
	}

	@Test
	public void testListRowSetsForTable() throws IOException{
		List<ColumnModel> models = TableModelTestUtils.createOneOfEachType();
		// create some test rows.
		List<Row> rows = TableModelTestUtils.createRows(models, 5);
		String tableId = "syn123";
		// Before we start there should be no changes
		List<TableRowChange> results = tableRowTruthDao.listRowSetsKeysForTable(tableId);
		assertNotNull(results);
		assertEquals(0, results.size());
		RowSet set = new RowSet();
		set.setHeaders(TableModelUtils.getHeaders(models));
		set.setRows(rows);
		set.setTableId(tableId);
		// Append this change set
		RowReferenceSet refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, set);
		assertNotNull(refSet);
		// Add some more rows
		set.setRows(TableModelTestUtils.createRows(models, 2));
		refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, set);
		// There should now be two version of the data
		results = tableRowTruthDao.listRowSetsKeysForTable(tableId);
		assertNotNull(results);
		assertEquals(2, results.size());
		// Validate the results
		// The first change should be version zero
		TableRowChange zero = results.get(0);
		assertEquals(new Long(0), zero.getRowVersion());
		assertEquals(tableId, zero.getTableId());
		assertEquals(set.getHeaders(), zero.getHeaders());
		assertEquals(creatorUserGroupId, zero.getCreatedBy());
		assertNotNull(zero.getCreatedOn());
		assertTrue(zero.getCreatedOn().getTime() > 0);
		assertNotNull(zero.getBucket());
		assertNotNull(zero.getKey());
		assertNotNull(zero.getEtag());
		assertEquals(new Long(5), zero.getRowCount());
		// Next is should be version one.
		// The first change should be version zero
		TableRowChange one = results.get(1);
		assertEquals(new Long(1), one.getRowVersion());
		assertNotNull(one.getEtag());
		assertFalse("Two changes cannot have the same Etag",zero.getEtag().equals(one.getEtag()));
		
		// Listing all versions greater than zero should be the same as all
		List<TableRowChange> greater = tableRowTruthDao.listRowSetsKeysForTableGreaterThanVersion(tableId, -1l);
		assertNotNull(greater);
		assertEquals(results, greater);
		// Now limit to greater than version zero
		greater = tableRowTruthDao.listRowSetsKeysForTableGreaterThanVersion(tableId, 0l);
		assertNotNull(greater);
		assertEquals(1, greater.size());
		assertEquals(new Long(1), greater.get(0).getRowVersion());
	}
	
	
	@Test
	public void testGetRowSet() throws IOException, NotFoundException{
		List<ColumnModel> models = TableModelTestUtils.createOneOfEachType();
		// create some test rows.
		List<Row> rows = TableModelTestUtils.createRows(models, 5);
		String tableId = "syn123";
		RowSet set = new RowSet();
		set.setHeaders(TableModelUtils.getHeaders(models));
		set.setRows(rows);
		set.setTableId(tableId);
		// Append this change set
		RowReferenceSet refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, set);
		assertNotNull(refSet);
		// Get the rows back
		RowSet fetched = tableRowTruthDao.getRowSet(tableId, 0l);
		assertNotNull(fetched);
		assertEquals(set.getHeaders(), fetched.getHeaders());
		assertEquals(tableId, fetched.getTableId());
		assertNotNull(fetched.getRows());
		assertNotNull(fetched.getEtag());
		// Lookup the version for this etag
		long versionForEtag = tableRowTruthDao.getVersionForEtag(tableId, fetched.getEtag());
		assertEquals(0l, versionForEtag);
		try{
			tableRowTruthDao.getVersionForEtag(tableId, "wrong etag");
			fail("should have failed");
		}catch(IllegalArgumentException e){
			// expected
		}
		assertEquals(set.getRows().size(), fetched.getRows().size());
		long expectedId = 0;
		Long version = new Long(0);
		for(int i=0; i<fetched.getRows().size(); i++){
			Row row = fetched.getRows().get(i);
			assertEquals(new Long(expectedId), row.getRowId());
			assertEquals(version, row.getVersionNumber());
			List<String> expectedValues = set.getRows().get(i).getValues();
			assertEquals(expectedValues, row.getValues());
			expectedId++;
		}
		// Version two does not exists so a not found should be thrown
		try{
			tableRowTruthDao.getRowSet(tableId, 1l);
			fail("Should have failed");
		}catch (NotFoundException e){
			// expected;
		}
	}
	
	
	@Test
	public void testGetRowSetOriginals() throws IOException, NotFoundException{
		List<ColumnModel> models = TableModelTestUtils.createOneOfEachType();
		// create some test rows.
		List<Row> rows = TableModelTestUtils.createRows(models, 5);
		
		String tableId = "syn123";
		RowSet set = new RowSet();
		set.setHeaders(TableModelUtils.getHeaders(models));
		set.setRows(rows);
		set.setTableId(tableId);
		// Append this change set
		RowReferenceSet refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, set);
		assertNotNull(refSet);
		// Get the rows for this set
		RowSet back = tableRowTruthDao.getRowSet(refSet, models);
		assertNotNull(back);
		assertEquals(set.getHeaders(), back.getHeaders());
		assertEquals(tableId, back.getTableId());
		assertNotNull(back.getRows());;
		assertEquals(set.getRows().size(), back.getRows().size());
		// The order should match the request
		for(int i=0; i<refSet.getRows().size(); i++){
			RowReference ref = refSet.getRows().get(i);
			Row row = back.getRows().get(i);
			assertEquals(ref.getRowId(), row.getRowId());
			assertEquals(ref.getVersionNumber(), row.getVersionNumber());
		}
	}
	
	
	@Test
	public void testAppendRowsUpdate() throws IOException, NotFoundException{
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
		RowReferenceSet refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, set);
		assertNotNull(refSet);
		// Now fetch the rows for an update
		RowSet toUpdate = tableRowTruthDao.getRowSet(tableId, 0l);
		// remove a few rows
		toUpdate.getRows().remove(0);
		toUpdate.getRows().remove(1);
		toUpdate.getRows().remove(1);
		// Update the remaining rows
		TableModelTestUtils.updateRow(models, toUpdate.getRows().get(0), 15);
		TableModelTestUtils.updateRow(models, toUpdate.getRows().get(1), 18);

		// create some new rows
		rows = TableModelTestUtils.createRows(models, 2);
		// Add them to the update
		toUpdate.getRows().addAll(rows);
		// Now append the changes.
		refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, toUpdate);
		assertNotNull(refSet);
		// Now get the second version and validate it is what we expect
		RowSet updated = tableRowTruthDao.getRowSet(tableId, 1l);
		System.out.println(updated);
		assertNotNull(updated);
		assertNotNull(updated.getRows());
		assertNotNull(updated.getEtag());
		assertEquals(4, updated.getRows().size());
	}
	
	@Test
	public void testAppendRowsUpdateAndGetLatest() throws IOException, NotFoundException {
		Map<Long, Long> rowVersions = Maps.newHashMap();
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
		RowReferenceSet refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, set);
		assertNotNull(refSet);
		for (RowReference ref : refSet.getRows()) {
			rowVersions.put(ref.getRowId(), ref.getVersionNumber());
		}

		// Now fetch the rows for an update
		RowSet toUpdate = tableRowTruthDao.getRowSet(tableId, 0l);
		// remove a few rows
		toUpdate.getRows().remove(0);
		toUpdate.getRows().remove(1);
		TableModelTestUtils.updateRow(models, toUpdate.getRows().get(0), 15);
		TableModelTestUtils.updateRow(models, toUpdate.getRows().get(1), 18);

		// create some new rows
		rows = TableModelTestUtils.createRows(models, 2);
		// Add them to the update
		toUpdate.getRows().addAll(rows);
		// Now append the changes.
		refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, toUpdate);
		assertNotNull(refSet);
		for (RowReference ref : refSet.getRows()) {
			rowVersions.put(ref.getRowId(), ref.getVersionNumber());
		}

		toUpdate = tableRowTruthDao.getRowSet(tableId, 1l);
		// remove a few rows
		TableModelTestUtils.updateRow(models, toUpdate.getRows().get(0), 19);
		TableModelTestUtils.updateRow(models, toUpdate.getRows().get(1), 21);
		// Now append the changes.
		refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, toUpdate);
		assertNotNull(refSet);
		for (RowReference ref : refSet.getRows()) {
			rowVersions.put(ref.getRowId(), ref.getVersionNumber());
		}

		assertEquals(7, rowVersions.size());
		RowSetAccessor latestVersions = tableRowTruthDao.getLatestVersions(tableId, rowVersions.keySet());
		assertEquals(7, latestVersions.getRows().size());
		for (RowAccessor row : latestVersions.getRows()) {
			assertEquals(row.getRow().getVersionNumber(), rowVersions.get(row.getRow().getRowId()));
		}
	}

	@Test
	public void testAppendRowsUpdateNoConflicted() throws IOException, NotFoundException{
		// Create some test column models
		List<ColumnModel> models = TableModelTestUtils.createOneOfEachType();
		// create some test rows.
		List<Row> rows = TableModelTestUtils.createRows(models, 3);
		String tableId = "syn123";
		RowSet set = new RowSet();
		set.setHeaders(TableModelUtils.getHeaders(models));
		set.setRows(rows);
		set.setTableId(tableId);
		// Append this change set
		RowReferenceSet refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, set);
		assertNotNull(refSet);
		// Now fetch the rows for an update
		RowSet toUpdateOne = tableRowTruthDao.getRowSet(tableId, 0l);
		RowSet toUpdateTwo = tableRowTruthDao.getRowSet(tableId, 0l);
		RowSet toUpdateThree = tableRowTruthDao.getRowSet(tableId, 0l);
		// For this case each update will change a different row so there is no conflict.
		// update row one
		Row toUpdate = toUpdateOne.getRows().get(0);
		TableModelTestUtils.updateRow(models, toUpdate, 100);
		toUpdateOne.getRows().clear();
		toUpdateOne.getRows().add(toUpdate);
		tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, toUpdateOne);
		// update row two
		toUpdate = toUpdateTwo.getRows().get(1);
		TableModelTestUtils.updateRow(models, toUpdate, 101);
		toUpdateTwo.getRows().clear();
		toUpdateTwo.getRows().add(toUpdate);
		tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, toUpdateTwo);
		// update row three
		toUpdate = toUpdateThree.getRows().get(2);
		TableModelTestUtils.updateRow(models, toUpdate, 102);
		toUpdateThree.getRows().clear();
		toUpdateThree.getRows().add(toUpdate);
		tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, toUpdateThree);
	}
	
	@Test
	public void testAppendRowsUpdateWithConflicts() throws IOException, NotFoundException{
		// Create some test column models
		List<ColumnModel> models = TableModelTestUtils.createOneOfEachType();
		// create some test rows.
		List<Row> rows = TableModelTestUtils.createRows(models, 3);
		String tableId = "syn123";
		RowSet set = new RowSet();
		set.setHeaders(TableModelUtils.getHeaders(models));
		set.setRows(rows);
		set.setTableId(tableId);
		// Append this change set
		RowReferenceSet refSet = tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, set);
		assertNotNull(refSet);
		// Now fetch the rows for an update
		RowSet toUpdateOne = tableRowTruthDao.getRowSet(tableId, 0l);
		RowSet toUpdateTwo = tableRowTruthDao.getRowSet(tableId, 0l);
		RowSet toUpdateThree = tableRowTruthDao.getRowSet(tableId, 0l);
		// For this case each update will change a different row so there is no conflict.
		// update row one
		Row toUpdate = toUpdateOne.getRows().get(0);
		TableModelTestUtils.updateRow(models, toUpdate, 100);
		toUpdateOne.getRows().clear();
		toUpdateOne.getRows().add(toUpdate);
		tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, toUpdateOne);
		// update row two
		toUpdate = toUpdateTwo.getRows().get(1);
		TableModelTestUtils.updateRow(models, toUpdate, 101);
		toUpdateTwo.getRows().clear();
		toUpdateTwo.getRows().add(toUpdate);
		tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, toUpdateTwo);
		// update row one again
		toUpdate = toUpdateThree.getRows().get(0);
		TableModelTestUtils.updateRow(models, toUpdate, 102);
		toUpdateThree.getRows().clear();
		toUpdateThree.getRows().add(toUpdate);
		try{
			// This should trigger a row level conflict with the first update.
			tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, toUpdateThree);
			fail("Should have triggered a row level conflict with row zero");
		}catch(ConflictingUpdateException e){
			// expected
			assertEquals("Row id: 0 has been changes since lasted read.  Please get the latest value for this row and then attempt to update it again.", e.getMessage());
		}

	}
	
	@Test
	public void testAppendRowIdOutOfRange() throws IOException, NotFoundException{
		// create some test rows.
		List<ColumnModel> models = TableModelTestUtils.createOneOfEachType();
			
		// create some test rows.
		List<Row> rows = TableModelTestUtils.createRows(models, 1);
		// Set the ID of the row to be beyond the valid range
		String tableId = "syn123";
		RowSet set = new RowSet();
		set.setHeaders(TableModelUtils.getHeaders(models));
		set.setRows(rows);
		set.setTableId(tableId);
		tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, set);
		// get the rows back
		RowSet toUpdateOne = tableRowTruthDao.getRowSet(tableId, 0l);
		// Create a row with an ID that is beyond the current max ID for the table
		Row toAdd = TableModelTestUtils.createRows(models, 1).get(0);
		toAdd.setRowId(toUpdateOne.getRows().get(0).getRowId()+1);
		toUpdateOne.getRows().add(toAdd);
		try{
			tableRowTruthDao.appendRowSetToTable(creatorUserGroupId, tableId, models, toUpdateOne);
			fail("should have failed since one of the row IDs was beyond the current max");
		}catch(IllegalArgumentException e){
			assertEquals("Cannot update row: 1 because it does not exist.", e.getMessage());
		}
	}
	
}
