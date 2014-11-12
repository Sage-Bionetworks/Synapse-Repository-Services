package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_CM_ORD_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_OWNER_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_BUCKET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_BOUND_COLUMN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_BOUND_COLUMN_ORDINAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_BOUND_COLUMN_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ROW_CHANGE;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableFileHandleResults;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.collect.Lists;

public class TableControllerAutowireTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	private FileHandleDao fileMetadataDao;
	@Autowired
	StackConfiguration config;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private AmazonS3Client s3Client;

	private Entity parent;
	private Long adminUserId;

	private List<S3FileHandle> handles = Lists.newArrayList();
	private List<String> entitiesToDelete = Lists.newArrayList();
	
	@Before
	public void before() throws Exception {
		Assume.assumeTrue(config.getTableEnabled());
	
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		parent = new Project();
		parent.setName(UUID.randomUUID().toString());
		parent = servletTestHelper.createEntity(dispatchServlet, parent, adminUserId);
		Assert.assertNotNull(parent);

		entitiesToDelete.add(parent.getId());
	}
	
	@After
	public void after(){
		if (config.getTableEnabled()) {
			for (String entity : entitiesToDelete) {
				try {
					servletTestHelper.deleteEntity(dispatchServlet, null, entity, adminUserId);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			for (S3FileHandle handle : handles) {
				fileMetadataDao.delete(handle.getId());
			}
		}
	}

	@Test
	public void testCreateGetDeleteColumnModel() throws ServletException, Exception{
		ColumnModel cm = new ColumnModel();
		cm.setName("TableControllerAutowireTest One");
		cm.setColumnType(ColumnType.STRING);
		// Save the column
		cm = servletTestHelper.createColumnModel(dispatchServlet, cm, adminUserId);
		assertNotNull(cm);
		assertNotNull(cm.getId());
		// Make sure we can get it
		ColumnModel clone = servletTestHelper.getColumnModel(dispatchServlet, cm.getId(), adminUserId);
		assertEquals(cm, clone);
	}
	
	@Test
	public void testCreateTableEntity() throws Exception{
		// Create a table with two ColumnModels
		// one
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = servletTestHelper.createColumnModel(dispatchServlet, one, adminUserId);
		// two
		ColumnModel two = new ColumnModel();
		two.setName("two");
		two.setColumnType(ColumnType.STRING);
		two = servletTestHelper.createColumnModel(dispatchServlet, two, adminUserId);
		// Now create a TableEntity with these Columns
		TableEntity table = new TableEntity();
		table.setName("TableEntity");
		table.setParentId(parent.getId());
		List<String> idList = new LinkedList<String>();
		idList.add(one.getId());
		idList.add(two.getId());
		table.setColumnIds(idList);
		table = servletTestHelper.createEntity(dispatchServlet, table, adminUserId);
		entitiesToDelete.add(table.getId());
		assertNotNull(table);
		assertNotNull(table.getId());
		TableEntity clone = servletTestHelper.getEntity(dispatchServlet, TableEntity.class, table.getId(), adminUserId);
		assertNotNull(clone);
		assertEquals(table, clone);
		// Now make sure we can get the list of columns for this entity
		List<ColumnModel> cols = servletTestHelper.getColumnModelsForTableEntity(dispatchServlet, table.getId(),
				adminUserId);
		assertNotNull(cols);
		assertEquals(2, cols.size());
		List<ColumnModel> expected = new LinkedList<ColumnModel>();
		expected.add(one);
		expected.add(two);
		assertEquals(expected, cols);
		
		// Add some rows to the table.
		RowSet set = new RowSet();
		List<Row> rows = TableModelTestUtils.createRows(cols, 3);
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(cols));
		set.setTableId(table.getId());
		RowReferenceSet results = servletTestHelper.appendTableRows(dispatchServlet, set, adminUserId);
		assertNotNull(results);
		assertNotNull(results.getRows());
		assertEquals(3, results.getRows().size());
		assertEquals(table.getId(), results.getTableId());
		assertEquals(TableModelUtils.getHeaders(cols), results.getHeaders());

		// delete a row
		RowSelection toDelete = new RowSelection();
		toDelete.setEtag(results.getEtag());
		toDelete.setTableId(results.getTableId());
		toDelete.setRowIds(Lists.newArrayList(results.getRows().get(1).getRowId()));
		servletTestHelper.deleteTableRows(dispatchServlet, toDelete, adminUserId);

		// get the rows
		results.getRows().remove(1);
		RowSet rowsAfter = servletTestHelper.getTableRows(dispatchServlet, results, adminUserId);
		set.getRows().remove(1);
		set.getRows().get(0).setRowId(results.getRows().get(0).getRowId());
		set.getRows().get(0).setVersionNumber(results.getRows().get(0).getVersionNumber());
		set.getRows().get(1).setRowId(results.getRows().get(1).getRowId());
		set.getRows().get(1).setVersionNumber(results.getRows().get(1).getVersionNumber());
		set.setEtag(results.getEtag());
		assertEquals(set, rowsAfter);
	}
	
	@Test
	public void testDeleteAllColumns() throws Exception {
		// Create a table with two ColumnModels
		ColumnModel one = servletTestHelper.createColumnModel(dispatchServlet,
				TableModelTestUtils.createColumn(0L, "one", ColumnType.STRING),
				adminUserId);
		ColumnModel two = servletTestHelper.createColumnModel(dispatchServlet,
				TableModelTestUtils.createColumn(0L, "two", ColumnType.STRING),
				adminUserId);
		// Now create a TableEntity with these Columns
		TableEntity table = new TableEntity();
		table.setName("TableEntity");
		table.setParentId(parent.getId());
		table.setColumnIds(Lists.newArrayList(one.getId(), two.getId()));
		table = servletTestHelper.createEntity(dispatchServlet, table, adminUserId);
		entitiesToDelete.add(table.getId());

		table.setColumnIds(Lists.<String>newArrayList());
		table = servletTestHelper.updateEntity(dispatchServlet, table, adminUserId);
		assertEquals(0, table.getColumnIds().size());
	}

	@Test
	public void testColumnNameCaseSensitiveCreateTableEntity() throws Exception{
		// create two columns that differ only by case.
		ColumnModel one = new ColumnModel();
		one.setName("Abc");
		one.setColumnType(ColumnType.STRING);
		one = servletTestHelper.createColumnModel(dispatchServlet, one, adminUserId);
		// two
		ColumnModel two = new ColumnModel();
		two.setName("aBC");
		two.setColumnType(ColumnType.STRING);
		two = servletTestHelper.createColumnModel(dispatchServlet, two, adminUserId);
		// Now create a TableEntity with these Columns
		TableEntity table = new TableEntity();
		table.setName("TableEntity");
		table.setParentId(parent.getId());
		List<String> idList = new LinkedList<String>();
		idList.add(one.getId());
		idList.add(two.getId());
		table.setColumnIds(idList);
		table = servletTestHelper.createEntity(dispatchServlet, table, adminUserId);
		entitiesToDelete.add(table.getId());
		assertNotNull(table);
		assertNotNull(table.getId());
		TableEntity clone = servletTestHelper.getEntity(dispatchServlet, TableEntity.class, table.getId(), adminUserId);
		assertNotNull(clone);
		assertEquals(table, clone);
		// Now make sure we can get the list of columns for this entity
		List<ColumnModel> cols = servletTestHelper.getColumnModelsForTableEntity(dispatchServlet, table.getId(),
				adminUserId);
		assertNotNull(cols);
		assertEquals(2, cols.size());
		List<ColumnModel> expected = new LinkedList<ColumnModel>();
		expected.add(one);
		expected.add(two);
		assertEquals(expected, cols);
		
		// Add some rows to the table.
		RowSet set = new RowSet();
		List<Row> rows = TableModelTestUtils.createRows(cols, 2);
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(cols));
		set.setTableId(table.getId());
		RowReferenceSet results = servletTestHelper.appendTableRows(dispatchServlet, set, adminUserId);
		assertNotNull(results);
		assertNotNull(results.getRows());
		assertEquals(2, results.getRows().size());
		assertEquals(table.getId(), results.getTableId());
		assertEquals(TableModelUtils.getHeaders(cols), results.getHeaders());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testColumnNameDuplicateId() throws Exception {
		// create two columns that differ only by case.
		ColumnModel one = new ColumnModel();
		one.setName("Abc");
		one.setColumnType(ColumnType.STRING);
		one = servletTestHelper.createColumnModel(dispatchServlet, one, adminUserId);
		// two
		ColumnModel two = new ColumnModel();
		two.setName("aBC");
		two.setColumnType(ColumnType.STRING);
		two = servletTestHelper.createColumnModel(dispatchServlet, two, adminUserId);
		// Now create a TableEntity with these Columns
		TableEntity table = new TableEntity();
		table.setName("TableEntity");
		table.setParentId(parent.getId());
		List<String> idList = Lists.newArrayList(one.getId(), two.getId(), one.getId());
		table.setColumnIds(idList);
		servletTestHelper.createEntity(dispatchServlet, table, adminUserId);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateRowUpdateFails() throws Exception {
		// Create a few columns to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = servletTestHelper.createColumnModel(dispatchServlet, one, adminUserId);

		// now create a table entity
		TableEntity table = new TableEntity();
		table.setName("Table");
		table.setColumnIds(Lists.newArrayList(one.getId()));
		table.setParentId(parent.getId());
		table = servletTestHelper.createEntity(dispatchServlet, table, adminUserId);
		entitiesToDelete.add(table.getId());
		List<ColumnModel> columns = servletTestHelper.getColumnModelsForTableEntity(dispatchServlet, table.getId(),
				adminUserId);

		// Append some rows
		RowSet set = new RowSet();
		List<Row> rows = TableModelTestUtils.createRows(columns, 2);
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(columns));
		set.setTableId(table.getId());
		RowReferenceSet results1 = servletTestHelper.appendTableRows(dispatchServlet, set, adminUserId);

		// update one row twice
		RowSet update = new RowSet();
		Row row1 = TableModelTestUtils.createRow(results1.getRows().get(0).getRowId(), results1.getRows().get(0).getVersionNumber(), rows
				.get(0).getValues().toArray(new String[0]));
		Row row2 = TableModelTestUtils.createRow(results1.getRows().get(0).getRowId(), results1.getRows().get(0).getVersionNumber(), rows
				.get(0).getValues().toArray(new String[0]));
		update.setRows(Lists.newArrayList(row1, row2));
		update.setTableId(results1.getTableId());
		update.setHeaders(results1.getHeaders());
		update.setEtag(results1.getEtag());
		servletTestHelper.appendTableRows(dispatchServlet, update, adminUserId);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyRowUpdateFails() throws Exception {
		// Create a few columns to add to a table entity
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = servletTestHelper.createColumnModel(dispatchServlet, one, adminUserId);

		// now create a table entity
		TableEntity table = new TableEntity();
		table.setName("Table");
		table.setColumnIds(Lists.newArrayList(one.getId()));
		table.setParentId(parent.getId());
		table = servletTestHelper.createEntity(dispatchServlet, table, adminUserId);
		entitiesToDelete.add(table.getId());
		List<ColumnModel> columns = servletTestHelper.getColumnModelsForTableEntity(dispatchServlet, table.getId(),
				adminUserId);

		// Append some rows
		RowSet set = new RowSet();
		List<Row> rows = TableModelTestUtils.createRows(columns, 2);
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(columns));
		set.setTableId(table.getId());
		RowReferenceSet results1 = servletTestHelper.appendTableRows(dispatchServlet, set, adminUserId);

		// update one row with empty values
		RowSet update = new RowSet();
		Row row1 = new Row();
		row1.setRowId(results1.getRows().get(0).getRowId());
		row1.setVersionNumber(results1.getRows().get(0).getVersionNumber());
		row1.setValues(Lists.<String> newArrayList());
		update.setRows(Lists.newArrayList(row1));
		update.setTableId(results1.getTableId());
		update.setHeaders(results1.getHeaders());
		update.setEtag(results1.getEtag());
		servletTestHelper.appendTableRows(dispatchServlet, update, adminUserId);
	}

	@Test
	public void testGetFileHandleURL() throws Exception {

		S3FileHandle handleOne = new S3FileHandle();
		handleOne.setCreatedBy(adminUserId.toString());
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("EntityControllerTest.mainFileKeyOne");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne.setContentMd5("handleOneContentMd5");
		handleOne = fileMetadataDao.createFile(handleOne);
		handles.add(handleOne);

		S3FileHandle handleTwo = new S3FileHandle();
		handleTwo.setCreatedBy(adminUserId.toString());
		handleTwo.setCreatedOn(new Date());
		handleTwo.setBucketName("bucket");
		handleTwo.setKey("EntityControllerTest.mainFileKeyTwo");
		handleTwo.setEtag("etag");
		handleTwo.setFileName("foo2.bar");
		handleTwo = fileMetadataDao.createFile(handleTwo);
		handles.add(handleTwo);

		// Create a table with two ColumnModels
		// one
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = servletTestHelper.createColumnModel(dispatchServlet, one, adminUserId);
		// two
		ColumnModel two = new ColumnModel();
		two.setName("two");
		two.setColumnType(ColumnType.FILEHANDLEID);
		two = servletTestHelper.createColumnModel(dispatchServlet, two, adminUserId);
		// Now create a TableEntity with these Columns
		TableEntity table = new TableEntity();
		table.setName("TableEntity");
		table.setParentId(parent.getId());
		List<String> idList = new LinkedList<String>();
		idList.add(one.getId());
		idList.add(two.getId());
		table.setColumnIds(idList);
		table = servletTestHelper.createEntity(dispatchServlet, table, adminUserId);
		entitiesToDelete.add(table.getId());

		// Add some rows to the table.
		List<ColumnModel> cols = servletTestHelper.getColumnModelsForTableEntity(dispatchServlet, table.getId(),
				adminUserId);
		RowSet set = new RowSet();
		set.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, "x", handleOne.getId()),
				TableModelTestUtils.createRow(null, null, "x", handleTwo.getId())));
		set.setHeaders(TableModelUtils.getHeaders(cols));
		set.setTableId(table.getId());
		RowReferenceSet results = servletTestHelper.appendTableRows(dispatchServlet, set, adminUserId);

		RowReference rowRef = TableModelTestUtils.createRowReference(results.getRows().get(0).getRowId(), results.getRows().get(0)
				.getVersionNumber());
		String url = servletTestHelper.getTableFileHandleUrl(dispatchServlet, table.getId(), rowRef, two.getId(),
				adminUserId, false);
		assertTrue(url.startsWith("https://bucket.s3.amazonaws.com/EntityControllerTest.mainFileKeyOne?"));

		rowRef = TableModelTestUtils.createRowReference(results.getRows().get(1).getRowId(), results.getRows().get(1).getVersionNumber());
		url = servletTestHelper.getTableFileHandleUrl(dispatchServlet, table.getId(), rowRef, two.getId(),
				adminUserId,
				false);
		assertTrue(url.startsWith("https://bucket.s3.amazonaws.com/EntityControllerTest.mainFileKeyTwo?"));

		try {
			servletTestHelper.getTableFileHandleUrl(dispatchServlet, table.getId(), rowRef, one.getId(), adminUserId,
					false);
			fail("Should have thrown illegal column");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testGetFileHandlePreviewURL() throws Exception {

		S3FileHandle handleOne = new S3FileHandle();
		handleOne.setCreatedBy(adminUserId.toString());
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("EntityControllerTest.mainFileKeyOne");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne.setContentMd5("handleOneContentMd5");
		handleOne = fileMetadataDao.createFile(handleOne);
		handles.add(handleOne);

		S3FileHandle handleTwo = new S3FileHandle();
		handleTwo.setCreatedBy(adminUserId.toString());
		handleTwo.setCreatedOn(new Date());
		handleTwo.setBucketName("bucket");
		handleTwo.setKey("EntityControllerTest.mainFileKeyTwo");
		handleTwo.setEtag("etag");
		handleTwo.setFileName("foo2.bar");
		handleTwo.setPreviewId(handleOne.getId());
		handleTwo = fileMetadataDao.createFile(handleTwo);
		handles.add(handleTwo);

		// Create a table with onw ColumnModel
		// one
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.FILEHANDLEID);
		one = servletTestHelper.createColumnModel(dispatchServlet, one, adminUserId);

		// Now create a TableEntity with this Column
		TableEntity table = new TableEntity();
		table.setName("TableEntity");
		table.setParentId(parent.getId());
		List<String> idList = Lists.newArrayList(one.getId());
		table.setColumnIds(idList);
		table = servletTestHelper.createEntity(dispatchServlet, table, adminUserId);
		entitiesToDelete.add(table.getId());

		// Add a rows to the table.
		List<ColumnModel> cols = servletTestHelper.getColumnModelsForTableEntity(dispatchServlet, table.getId(),
				adminUserId);
		RowSet set = new RowSet();
		set.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, handleTwo.getId())));
		set.setHeaders(TableModelUtils.getHeaders(cols));
		set.setTableId(table.getId());
		RowReferenceSet results = servletTestHelper.appendTableRows(dispatchServlet, set, adminUserId);

		RowReference rowRef = TableModelTestUtils.createRowReference(results.getRows().get(0).getRowId(), results.getRows().get(0)
				.getVersionNumber());
		String url = servletTestHelper.getTableFileHandleUrl(dispatchServlet, table.getId(), rowRef, one.getId(),
				adminUserId, false);
		assertTrue(url.startsWith("https://bucket.s3.amazonaws.com/EntityControllerTest.mainFileKeyTwo?"));

		url = servletTestHelper.getTableFileHandleUrl(dispatchServlet, table.getId(), rowRef, one.getId(),
				adminUserId,
				true);
		assertTrue(url.startsWith("https://bucket.s3.amazonaws.com/EntityControllerTest.mainFileKeyOne?"));
	}

	@Test
	public void testGetFileHandles() throws Exception {
		for (int i = 0; i < 4; i++) {
			S3FileHandle handle = new S3FileHandle();
			handle.setCreatedBy(adminUserId.toString());
			handle.setCreatedOn(new Date());
			handle.setBucketName("bucket");
			handle.setKey("EntityControllerTest.mainFileKey" + i);
			handle.setEtag("etag" + i);
			handle.setFileName("foo.bar");
			handle.setContentMd5("handleContentMd5_" + i);
			handle = fileMetadataDao.createFile(handle);
			handles.add(handle);
		}

		// Create a table with three ColumnModels
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.FILEHANDLEID);
		one = servletTestHelper.createColumnModel(dispatchServlet, one, adminUserId);
		// two
		ColumnModel two = new ColumnModel();
		two.setName("two");
		two.setColumnType(ColumnType.STRING);
		two = servletTestHelper.createColumnModel(dispatchServlet, two, adminUserId);
		// three
		ColumnModel three = new ColumnModel();
		three.setName("three");
		three.setColumnType(ColumnType.FILEHANDLEID);
		three = servletTestHelper.createColumnModel(dispatchServlet, three, adminUserId);
		// Now create a TableEntity with these Columns
		TableEntity table = new TableEntity();
		table.setName("TableEntity");
		table.setParentId(parent.getId());
		table.setColumnIds(Lists.newArrayList(one.getId(), two.getId(), three.getId()));
		table = servletTestHelper.createEntity(dispatchServlet, table, adminUserId);
		entitiesToDelete.add(table.getId());

		// Add some rows to the table.
		List<ColumnModel> cols = servletTestHelper.getColumnModelsForTableEntity(dispatchServlet, table.getId(),
				adminUserId);
		RowSet set = new RowSet();
		set.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, handles.get(0).getId(), null, handles.get(1).getId()),
				TableModelTestUtils.createRow(null, null, handles.get(2).getId(), null, null),
				TableModelTestUtils.createRow(null, null, null, null, handles.get(3).getId()),
				TableModelTestUtils.createRow(null, null, null, null, null)));
		set.setHeaders(TableModelUtils.getHeaders(cols));
		set.setTableId(table.getId());
		RowReferenceSet results = servletTestHelper.appendTableRows(dispatchServlet, set, adminUserId);

		// remove the string column
		results.setHeaders(Lists.newArrayList(three.getId(), one.getId()));
		TableFileHandleResults tableFileHandles = servletTestHelper.getTableFileHandles(dispatchServlet, results,
				adminUserId);
		assertEquals(2, tableFileHandles.getHeaders().size());
		assertEquals(three.getId(), tableFileHandles.getHeaders().get(0));
		assertEquals(one.getId(), tableFileHandles.getHeaders().get(1));
		assertEquals(4, tableFileHandles.getRows().size());
		int row = 0;
		assertEquals(2, tableFileHandles.getRows().get(row).getList().size());
		assertEquals(handles.get(1).getId(), tableFileHandles.getRows().get(row).getList().get(0).getId());
		assertEquals(handles.get(0).getId(), tableFileHandles.getRows().get(row).getList().get(1).getId());
		row++;
		assertEquals(2, tableFileHandles.getRows().get(row).getList().size());
		assertEquals(null, tableFileHandles.getRows().get(row).getList().get(0));
		assertEquals(handles.get(2).getId(), tableFileHandles.getRows().get(row).getList().get(1).getId());
		row++;
		assertEquals(2, tableFileHandles.getRows().get(row).getList().size());
		assertEquals(handles.get(3).getId(), tableFileHandles.getRows().get(row).getList().get(0).getId());
		assertEquals(null, tableFileHandles.getRows().get(row).getList().get(1));
		row++;
		assertEquals(2, tableFileHandles.getRows().get(row).getList().size());
		assertEquals(null, tableFileHandles.getRows().get(row).getList().get(0));
		assertEquals(null, tableFileHandles.getRows().get(row).getList().get(1));
	}

	@Test
	public void testListColumnModels() throws ServletException, Exception{
		ColumnModel one = new ColumnModel();
		String prefix = UUID.randomUUID().toString();
		one.setName(prefix+"a");
		one.setColumnType(ColumnType.STRING);
		one = servletTestHelper.createColumnModel(dispatchServlet, one, adminUserId);
		// two
		ColumnModel two = new ColumnModel();
		two.setName(prefix+"b");
		two.setColumnType(ColumnType.STRING);
		two = servletTestHelper.createColumnModel(dispatchServlet, two, adminUserId);
		// three
		ColumnModel three = new ColumnModel();
		three.setName(prefix+"bb");
		three.setColumnType(ColumnType.STRING);
		three = servletTestHelper.createColumnModel(dispatchServlet, three, adminUserId);
		// Now make sure we can find our columns
		PaginatedColumnModels pcm = servletTestHelper.listColumnModels(dispatchServlet, adminUserId, null, null, null);
		assertNotNull(pcm);
		assertTrue(pcm.getTotalNumberOfResults() >= 3);
		// filter by our prefix
		pcm = servletTestHelper.listColumnModels(dispatchServlet, adminUserId, prefix, null, null);
		assertNotNull(pcm);
		List<ColumnModel> expected = new LinkedList<ColumnModel>();
		expected.add(one);
		expected.add(two);
		expected.add(three);
		assertEquals(new Long(3), pcm.getTotalNumberOfResults());
		assertEquals(expected, pcm.getResults());
		// Now try pagination.
		pcm = servletTestHelper.listColumnModels(dispatchServlet, adminUserId, prefix, 1l, 2l);
		assertNotNull(pcm);
		assertEquals(new Long(3), pcm.getTotalNumberOfResults());
		expected.clear();
		expected.add(three);
		assertEquals(expected, pcm.getResults());
	}
	
	@Test
	public void testDeleteTableEntity() throws Exception {
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = servletTestHelper.createColumnModel(dispatchServlet, one, adminUserId);
		ColumnModel two = new ColumnModel();
		two.setName("two");
		two.setColumnType(ColumnType.STRING);
		two = servletTestHelper.createColumnModel(dispatchServlet, two, adminUserId);
		// Now create a TableEntity with these Columns
		TableEntity table = new TableEntity();
		table.setName("TableEntity");
		table.setParentId(parent.getId());
		table.setColumnIds(Lists.newArrayList(one.getId(), two.getId()));
		table = servletTestHelper.createEntity(dispatchServlet, table, adminUserId);
		List<ColumnModel> cols = servletTestHelper.getColumnModelsForTableEntity(dispatchServlet, table.getId(), adminUserId);
		// Add some rows to the table.
		RowSet set = new RowSet();
		List<Row> rows = TableModelTestUtils.createRows(cols, 3);
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(cols));
		set.setTableId(table.getId());
		servletTestHelper.appendTableRows(dispatchServlet, set, adminUserId);
		set = new RowSet();
		rows = TableModelTestUtils.createRows(cols, 3);
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(cols));
		set.setTableId(table.getId());
		servletTestHelper.appendTableRows(dispatchServlet, set, adminUserId);

		Long tableId = KeyFactory.stringToKey(table.getId());
		// find the s3 bucket and keys
		String s3Bucket = jdbcTemplate.queryForObject("select distinct " + COL_TABLE_ROW_BUCKET + " from " + TABLE_ROW_CHANGE + " where "
				+ COL_TABLE_ROW_TABLE_ID + " = ?", String.class, tableId);
		List<String> s3Keys = jdbcTemplate.queryForList("select " + COL_TABLE_ROW_KEY + " from " + TABLE_ROW_CHANGE + " where "
				+ COL_TABLE_ROW_TABLE_ID + " = ?", String.class, tableId);
		for (String s3Key : s3Keys) {
			s3Client.getObjectMetadata(s3Bucket, s3Key);
		}

		checkCount(2L, "select count(*) from " + TABLE_ROW_CHANGE + " where " + COL_TABLE_ROW_TABLE_ID + " = ?", tableId);
		checkCount(2L, "select count(*) from " + TABLE_BOUND_COLUMN + " where " + COL_BOUND_CM_OBJECT_ID + " = ?", tableId);
		checkCount(2L, "select count(*) from " + TABLE_BOUND_COLUMN_ORDINAL + " where " + COL_BOUND_CM_ORD_OBJECT_ID + " = ?", tableId);
		checkCount(1L, "select count(*) from " + TABLE_BOUND_COLUMN_OWNER + " where " + COL_BOUND_OWNER_OBJECT_ID + " = ?", tableId);

		servletTestHelper.deleteEntity(dispatchServlet, TableEntity.class, table.getId(), adminUserId,
				Collections.singletonMap("skipTrashCan", "true"));

		checkCount(0L, "select count(*) from " + TABLE_ROW_CHANGE + " where " + COL_TABLE_ROW_TABLE_ID + " = ?", tableId);
		checkCount(0L, "select count(*) from " + TABLE_BOUND_COLUMN + " where " + COL_BOUND_CM_OBJECT_ID + " = ?", tableId);
		checkCount(0L, "select count(*) from " + TABLE_BOUND_COLUMN_ORDINAL + " where " + COL_BOUND_CM_ORD_OBJECT_ID + " = ?", tableId);
		checkCount(0L, "select count(*) from " + TABLE_BOUND_COLUMN_OWNER + " where " + COL_BOUND_OWNER_OBJECT_ID + " = ?", tableId);
		for (String s3Key : s3Keys) {
			try {
				s3Client.getObjectMetadata(s3Bucket, s3Key);
				fail("s3 object " + s3Key + " should no longer exist");
			} catch (AmazonClientException e) {
			}
		}
	}

	private void checkCount(long expected, String sql, Object... args) {
		assertEquals(expected, jdbcTemplate.queryForObject(sql, Long.class, args).longValue());
	}
}
