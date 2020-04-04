package org.sagebionetworks.repo.web.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.collect.Lists;

import junit.framework.Assert;

public class TableControllerAutowireTest extends AbstractAutowiredControllerJunit5TestBase {

	@Autowired
	private FileHandleDao fileMetadataDao;
	@Autowired
	StackConfiguration config;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private SynapseS3Client s3Client;

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	private Entity parent;
	private Long adminUserId;

	private List<S3FileHandle> handles = Lists.newArrayList();
	private List<String> entitiesToDelete = Lists.newArrayList();
	
	private String accessToken;
	
	@BeforeEach
	public void before() throws Exception {
	
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		accessToken = oidcTokenHelper.createTotalAccessToken(adminUserId);
		
		parent = new Project();
		parent.setName(UUID.randomUUID().toString());
		parent = servletTestHelper.createEntity(dispatchServlet, parent, accessToken);
		Assert.assertNotNull(parent);

		entitiesToDelete.add(parent.getId());
	}
	
	@AfterEach
	public void after(){
		for (String entity : Lists.reverse(entitiesToDelete)) {
			try {
				servletTestHelper.deleteEntity(dispatchServlet, null, entity, adminUserId,
						Collections.singletonMap("skipTrashCan", "false"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		for (S3FileHandle handle : handles) {
			fileMetadataDao.delete(handle.getId());
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
		table = servletTestHelper.createEntity(dispatchServlet, table, accessToken);
		entitiesToDelete.add(table.getId());

		table.setColumnIds(Lists.<String>newArrayList());
		table = servletTestHelper.updateEntity(dispatchServlet, table, accessToken);
		assertEquals(0, table.getColumnIds().size());
	}

	@Test
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
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			servletTestHelper.createEntity(dispatchServlet, table, accessToken);
		});
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
}
