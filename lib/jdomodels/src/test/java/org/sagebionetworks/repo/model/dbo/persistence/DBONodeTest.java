package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBONodeTest {
	
	@Autowired
	private DBOBasicDao dboBasicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private List<Long> toDelete = null;
	
	@After
	public void after() throws DatastoreException {
		if(dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBONode.class, params);
			}
		}
	}
	
	@Before
	public void before(){
		toDelete = new LinkedList<Long>();
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException, NotFoundException, UnsupportedEncodingException{
		DBONode node = new DBONode();
		node.setId(idGenerator.generateNewId());
		node.setName("SomeName");
		node.setBenefactorId(node.getId());
		Long createdById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		node.setCreatedBy(createdById);
		node.setCreatedOn(System.currentTimeMillis());
		node.setCurrentRevNumber(null);
		node.seteTag("1");
		node.setType(EntityType.project.name());
		// Make sure we can create it
		DBONode clone = dboBasicDao.createNew(node);
		assertNotNull(clone);
		toDelete.add(node.getId());
		assertEquals(node, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", node.getId());
		clone = dboBasicDao.getObjectByPrimaryKey(DBONode.class, params);
		assertNotNull(clone);
		assertEquals(node, clone);
		
		// Make sure we can create a child
		DBONode child = new DBONode();
		child.setId(idGenerator.generateNewId());
		child.setName("SomeChild");
		child.setBenefactorId(node.getBenefactorId());
		child.setCreatedBy(createdById);
		child.setCreatedOn(System.currentTimeMillis());
		child.setCurrentRevNumber(new Long(0));
		child.seteTag("1");
		child.setType(EntityType.folder.name());
		child.setParentId(node.getId());
		child.setDescription("I have a description".getBytes("UTF-8"));
		// Get it back
		clone = dboBasicDao.createNew(child);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		assertEquals(child, clone);
		
		
		// Make sure we can update it.
		clone.setDescription("This is a new description".getBytes("UTF-8"));
		clone.seteTag("2");
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		
		// Get the clone back again
		params = new MapSqlParameterSource();
		params.addValue("id", clone.getId());
		DBONode clone2 = dboBasicDao.getObjectByPrimaryKey(DBONode.class, params);
		assertEquals(clone, clone2);
	}

}
