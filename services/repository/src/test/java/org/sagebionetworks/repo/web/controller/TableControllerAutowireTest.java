package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableControllerAutowireTest {

	@Autowired
	private FileHandleDao fileMetadataDao;

	private Entity parent;
	private Long adminUserId;

	private S3FileHandle handleOne = null;
	private S3FileHandle handleTwo = null;
	
	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		parent = new Project();
		parent.setName(UUID.randomUUID().toString());
		parent = ServletTestHelper.createEntity(DispatchServletSingleton.getInstance(), parent, adminUserId);
		Assert.assertNotNull(parent);
	}
	
	@After
	public void after(){
		if(parent != null){
			try {
				ServletTestHelper.deleteEntity(DispatchServletSingleton.getInstance(), Project.class, parent.getId(), adminUserId);
			} catch (Exception e) {} 
		}
		if (handleOne != null) {
			fileMetadataDao.delete(handleOne.getId());
		}
		if (handleTwo != null) {
			fileMetadataDao.delete(handleTwo.getId());
		}
	}

	@Test
	public void testCreateGetDeleteColumnModel() throws ServletException, Exception{
		ColumnModel cm = new ColumnModel();
		cm.setName("TableControllerAutowireTest One");
		cm.setColumnType(ColumnType.STRING);
		// Save the column
		cm = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), cm, adminUserId);
		assertNotNull(cm);
		assertNotNull(cm.getId());
		// Make sure we can get it
		ColumnModel clone = ServletTestHelper.getColumnModel(DispatchServletSingleton.getInstance(), cm.getId(), adminUserId);
		assertEquals(cm, clone);
	}
	
	@Test
	public void testCreateTableEntity() throws Exception{
		// Create a table with two ColumnModels
		// one
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), one, adminUserId);
		// two
		ColumnModel two = new ColumnModel();
		two.setName("two");
		two.setColumnType(ColumnType.STRING);
		two = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), two, adminUserId);
		// Now create a TableEntity with these Columns
		TableEntity table = new TableEntity();
		table.setName("TableEntity");
		table.setParentId(parent.getId());
		List<String> idList = new LinkedList<String>();
		idList.add(one.getId());
		idList.add(two.getId());
		table.setColumnIds(idList);
		table = ServletTestHelper.createEntity(DispatchServletSingleton.getInstance(), table, adminUserId);
		assertNotNull(table);
		assertNotNull(table.getId());
		TableEntity clone = ServletTestHelper.getEntity(DispatchServletSingleton.getInstance(), TableEntity.class, table.getId(), adminUserId);
		assertNotNull(clone);
		assertEquals(table, clone);
		// Now make sure we can get the list of columns for this entity
		List<ColumnModel> cols = ServletTestHelper.getColumnModelsForTableEntity(DispatchServletSingleton.getInstance(), table.getId(), adminUserId);
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
		RowReferenceSet results = ServletTestHelper.appendTableRows(DispatchServletSingleton.getInstance(), set, adminUserId);
		assertNotNull(results);
		assertNotNull(results.getRows());
		assertEquals(2, results.getRows().size());
		assertEquals(table.getId(), results.getTableId());
		assertEquals(TableModelUtils.getHeaders(cols), results.getHeaders());

		// delete a row
		results.getRows().remove(0);
		ServletTestHelper.deleteTableRows(DispatchServletSingleton.getInstance(), results, adminUserId);
	}

	@Test
	public void testGetFileHandleURL() throws Exception {

		handleOne = new S3FileHandle();
		handleOne.setCreatedBy(adminUserId.toString());
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("EntityControllerTest.mainFileKeyOne");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne.setContentMd5("handleOneContentMd5");
		handleOne = fileMetadataDao.createFile(handleOne);

		handleTwo = new S3FileHandle();
		handleTwo.setCreatedBy(adminUserId.toString());
		handleTwo.setCreatedOn(new Date());
		handleTwo.setBucketName("bucket");
		handleTwo.setKey("EntityControllerTest.mainFileKeyTwo");
		handleTwo.setEtag("etag");
		handleTwo.setFileName("foo2.bar");
		handleTwo = fileMetadataDao.createFile(handleTwo);

		// Create a table with two ColumnModels
		// one
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), one, adminUserId);
		// two
		ColumnModel two = new ColumnModel();
		two.setName("two");
		two.setColumnType(ColumnType.FILEHANDLEID);
		two = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), two, adminUserId);
		// Now create a TableEntity with these Columns
		TableEntity table = new TableEntity();
		table.setName("TableEntity");
		table.setParentId(parent.getId());
		List<String> idList = new LinkedList<String>();
		idList.add(one.getId());
		idList.add(two.getId());
		table.setColumnIds(idList);
		table = ServletTestHelper.createEntity(DispatchServletSingleton.getInstance(), table, adminUserId);

		// Add some rows to the table.
		List<ColumnModel> cols = ServletTestHelper.getColumnModelsForTableEntity(DispatchServletSingleton.getInstance(), table.getId(),
				adminUserId);
		RowSet set = new RowSet();
		set.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, "x", handleOne.getId()),
				TableModelTestUtils.createRow(null, null, "x", handleTwo.getId())));
		set.setHeaders(TableModelUtils.getHeaders(cols));
		set.setTableId(table.getId());
		RowReferenceSet results = ServletTestHelper.appendTableRows(DispatchServletSingleton.getInstance(), set, adminUserId);

		RowReference rowRef = TableModelTestUtils.createRowReference(results.getRows().get(0).getRowId(), results.getRows().get(0)
				.getVersionNumber());
		String url = ServletTestHelper.getTableFileHandle(DispatchServletSingleton.getInstance(), table.getId(), rowRef, two.getId(),
				adminUserId, false);
		assertTrue(url.startsWith("https://bucket.s3.amazonaws.com/EntityControllerTest.mainFileKeyOne?"));

		rowRef = TableModelTestUtils.createRowReference(results.getRows().get(1).getRowId(), results.getRows().get(1).getVersionNumber());
		url = ServletTestHelper.getTableFileHandle(DispatchServletSingleton.getInstance(), table.getId(), rowRef, two.getId(), adminUserId,
				false);
		assertTrue(url.startsWith("https://bucket.s3.amazonaws.com/EntityControllerTest.mainFileKeyTwo?"));

		try {
			ServletTestHelper.getTableFileHandle(DispatchServletSingleton.getInstance(), table.getId(), rowRef, one.getId(), adminUserId,
					false);
			fail("Should have thrown illegal column");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testGetFileHandlePreviewURL() throws Exception {

		handleOne = new S3FileHandle();
		handleOne.setCreatedBy(adminUserId.toString());
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("EntityControllerTest.mainFileKeyOne");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne.setContentMd5("handleOneContentMd5");
		handleOne = fileMetadataDao.createFile(handleOne);

		handleTwo = new S3FileHandle();
		handleTwo.setCreatedBy(adminUserId.toString());
		handleTwo.setCreatedOn(new Date());
		handleTwo.setBucketName("bucket");
		handleTwo.setKey("EntityControllerTest.mainFileKeyTwo");
		handleTwo.setEtag("etag");
		handleTwo.setFileName("foo2.bar");
		handleTwo.setPreviewId(handleOne.getId());
		handleTwo = fileMetadataDao.createFile(handleTwo);

		// Create a table with onw ColumnModel
		// one
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.FILEHANDLEID);
		one = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), one, adminUserId);

		// Now create a TableEntity with this Column
		TableEntity table = new TableEntity();
		table.setName("TableEntity");
		table.setParentId(parent.getId());
		List<String> idList = Lists.newArrayList(one.getId());
		table.setColumnIds(idList);
		table = ServletTestHelper.createEntity(DispatchServletSingleton.getInstance(), table, adminUserId);

		// Add a rows to the table.
		List<ColumnModel> cols = ServletTestHelper.getColumnModelsForTableEntity(DispatchServletSingleton.getInstance(), table.getId(),
				adminUserId);
		RowSet set = new RowSet();
		set.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, handleTwo.getId())));
		set.setHeaders(TableModelUtils.getHeaders(cols));
		set.setTableId(table.getId());
		RowReferenceSet results = ServletTestHelper.appendTableRows(DispatchServletSingleton.getInstance(), set, adminUserId);

		RowReference rowRef = TableModelTestUtils.createRowReference(results.getRows().get(0).getRowId(), results.getRows().get(0)
				.getVersionNumber());
		String url = ServletTestHelper.getTableFileHandle(DispatchServletSingleton.getInstance(), table.getId(), rowRef, one.getId(),
				adminUserId, false);
		assertTrue(url.startsWith("https://bucket.s3.amazonaws.com/EntityControllerTest.mainFileKeyTwo?"));

		url = ServletTestHelper.getTableFileHandle(DispatchServletSingleton.getInstance(), table.getId(), rowRef, one.getId(), adminUserId,
				true);
		assertTrue(url.startsWith("https://bucket.s3.amazonaws.com/EntityControllerTest.mainFileKeyOne?"));
	}

	@Test
	public void testListColumnModels() throws ServletException, Exception{
		ColumnModel one = new ColumnModel();
		String prefix = UUID.randomUUID().toString();
		one.setName(prefix+"a");
		one.setColumnType(ColumnType.STRING);
		one = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), one, adminUserId);
		// two
		ColumnModel two = new ColumnModel();
		two.setName(prefix+"b");
		two.setColumnType(ColumnType.STRING);
		two = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), two, adminUserId);
		// three
		ColumnModel three = new ColumnModel();
		three.setName(prefix+"bb");
		three.setColumnType(ColumnType.STRING);
		three = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), three, adminUserId);
		// Now make sure we can find our columns
		PaginatedColumnModels pcm = ServletTestHelper.listColumnModels(DispatchServletSingleton.getInstance(), adminUserId, null, null, null);
		assertNotNull(pcm);
		assertTrue(pcm.getTotalNumberOfResults() >= 3);
		// filter by our prefix
		pcm = ServletTestHelper.listColumnModels(DispatchServletSingleton.getInstance(), adminUserId, prefix, null, null);
		assertNotNull(pcm);
		List<ColumnModel> expected = new LinkedList<ColumnModel>();
		expected.add(one);
		expected.add(two);
		expected.add(three);
		assertEquals(new Long(3), pcm.getTotalNumberOfResults());
		assertEquals(expected, pcm.getResults());
		// Now try pagination.
		pcm = ServletTestHelper.listColumnModels(DispatchServletSingleton.getInstance(), adminUserId, prefix, 1l, 2l);
		assertNotNull(pcm);
		assertEquals(new Long(3), pcm.getTotalNumberOfResults());
		expected.clear();
		expected.add(three);
		assertEquals(expected, pcm.getResults());
	}
	
	@Test
	public void testTableQueryTableUnavailable() throws ServletException, Exception{
		// Create a table with two ColumnModels
		// one
		ColumnModel one = new ColumnModel();
		one.setName("foo");
		one.setColumnType(ColumnType.LONG);
		one = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), one, adminUserId);
		// two
		ColumnModel two = new ColumnModel();
		two.setName("bar");
		two.setColumnType(ColumnType.STRING);
		two = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), two, adminUserId);
		// Now create a TableEntity with these Columns
		TableEntity table = new TableEntity();
		table.setName("TableEntity2");
		table.setParentId(parent.getId());
		List<String> idList = new LinkedList<String>();
		idList.add(one.getId());
		idList.add(two.getId());
		table.setColumnIds(idList);
		table = ServletTestHelper.createEntity(DispatchServletSingleton.getInstance(), table, adminUserId);
		assertNotNull(table);
		assertNotNull(table.getId());
		TableEntity clone = ServletTestHelper.getEntity(DispatchServletSingleton.getInstance(), TableEntity.class, table.getId(), adminUserId);
		assertNotNull(clone);
		assertEquals(table, clone);
		// Now make sure we can get the list of columns for this entity
		List<ColumnModel> cols = ServletTestHelper.getColumnModelsForTableEntity(DispatchServletSingleton.getInstance(), table.getId(), adminUserId);
		assertNotNull(cols);
		
		// Add some rows to the table.
		RowSet set = new RowSet();
		List<Row> rows = TableModelTestUtils.createRows(cols, 2);
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(cols));
		set.setTableId(table.getId());
		ServletTestHelper.appendTableRows(DispatchServletSingleton.getInstance(), set, adminUserId);
		
		// Since the worker is not working on the table, this should fail
		try{
			Query query = new Query();
			query.setSql("select * from "+table.getId()+" limit 2");
			ServletTestHelper.tableQuery(DispatchServletSingleton.getInstance(), adminUserId, query);
			fail("This should have failed");
		}catch(TableUnavilableException e){
			TableStatus status = e.getStatus();
			assertNotNull(status);
			assertEquals(TableState.PROCESSING, status.getState());
			assertEquals(KeyFactory.stringToKey(table.getId()).toString(), status.getTableId());
			// expected
			System.out.println(e.getStatus());
		}
		
	}
}
