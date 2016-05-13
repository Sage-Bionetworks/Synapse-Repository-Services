package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class FileViewDaoImplTest {
	
	@Autowired
	NodeDAO nodeDao;
	@Autowired
	FileViewDao fileViewDao;
	
	private Long creatorUserGroupId;
	List<String> toDelete;
	
	@Before
	public void before(){
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		toDelete = new ArrayList<String>();
	}
	
	@After
	public void after(){
		if(toDelete != null && nodeDao != null){
			for(String id:  toDelete){
				// Delete each
				try{
					nodeDao.delete(id);
				}catch (Exception e) {}
			}
		}
	}
	
	@Test
	public void testCalculateCRCForAllFilesWithinContainers() throws UnsupportedEncodingException{
		// create a project
		Node project = NodeTestUtils.createNew("Project", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		project = nodeDao.createNewNode(project);
		toDelete.add(project.getId());
		assertNotNull(project);
		// add a file
		Node file = NodeTestUtils.createNew("file", creatorUserGroupId);
		file.setNodeType(EntityType.file);
		file.setParentId(project.getId());
		file = nodeDao.createNewNode(file);
		
		Long projectId = KeyFactory.stringToKey(project.getId());
		Set<Long> containers = Sets.newHashSet(projectId);
		//  call under test
		long crcResults = fileViewDao.calculateCRCForAllFilesWithinContainers(containers);
		assertTrue(crcResults > 0L);
	}
	
	@Test
	public void testCalculateCRCForAllFilesWithinContainersNoChildren() throws UnsupportedEncodingException{
		// create a project
		Node project = NodeTestUtils.createNew("Project", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		project = nodeDao.createNewNode(project);
		toDelete.add(project.getId());
		assertNotNull(project);
		
		Long projectId = KeyFactory.stringToKey(project.getId());
		Set<Long> containers = Sets.newHashSet(projectId);
		//  call under test
		long crcResults = fileViewDao.calculateCRCForAllFilesWithinContainers(containers);
		assertEquals(0, crcResults);
	}
	
	@Test
	public void testCalculateCRCForAllFilesWithinContainersEmpty() throws UnsupportedEncodingException{
		// empty containers
		Set<Long> containers = new HashSet<Long>(0);
		//  call under test
		long crcResults = fileViewDao.calculateCRCForAllFilesWithinContainers(containers);
		assertEquals(0, crcResults);
	}
	
	@Test
	public void testStreamOverFileEntities() throws UnsupportedEncodingException{
		// create a project
		Node project = NodeTestUtils.createNew("Project", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		project = nodeDao.createNewNode(project);
		toDelete.add(project.getId());
		assertNotNull(project);
		
		Long projectId = KeyFactory.stringToKey(project.getId());
		Set<Long> containers = Sets.newHashSet(projectId);
		// There should no files yet
		long count = fileViewDao.countAllFilesInView(containers);
		assertEquals(0, count);
		
		// add a file
		Node file = NodeTestUtils.createNew("file", creatorUserGroupId);
		file.setNodeType(EntityType.file);
		file.setParentId(project.getId());
		file = nodeDao.createNewNode(file);
		// add some annotations
		NamedAnnotations annos = new NamedAnnotations();
		annos.getAdditionalAnnotations().addAnnotation("longKey", new Long(555));
		annos.setId(file.getId());
		annos.setEtag(file.getETag());
		annos.setCreatedBy(file.getCreatedByPrincipalId());
		annos.setCreationDate(new Date());
		nodeDao.updateAnnotations(file.getId(), annos);
		
		// Setup the schema to fetch
		List<ColumnModel> schema = new LinkedList<ColumnModel>();
		schema.add(FileEntityFields.id.getColumnModel());
		schema.add(FileEntityFields.name.getColumnModel());
		schema.add(FileEntityFields.etag.getColumnModel());
	
		ColumnModel annoColumn = new ColumnModel();
		annoColumn.setName("longKey");
		annoColumn.setColumnType(ColumnType.INTEGER);
		schema.add(annoColumn);
		
		//  call under test
		final List<Row> rows = new LinkedList<Row>();
		// call under test
		fileViewDao.streamOverFileEntities(containers, schema, new RowHandler() {
			
			@Override
			public void nextRow(Row row) {
				rows.add(row);
				
			}
		});
		Long fileId = KeyFactory.stringToKey(file.getId());
		assertEquals(1, rows.size());
		Row row = rows.get(0);
		assertEquals(fileId, row.getRowId());
		assertEquals(file.getVersionNumber(), row.getVersionNumber());
		assertNotNull(row.getValues());
		List<String> values = row.getValues();
		List<String> expectedValeus = Lists.newArrayList(""+fileId,file.getName(),file.getETag(),"555");
		assertEquals(expectedValeus, values);
		
		// there should now be one file.
		count = fileViewDao.countAllFilesInView(containers);
		assertEquals(1, count);
	}

}
