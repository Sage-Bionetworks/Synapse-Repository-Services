package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBORevisionTest {
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	List<Long> toDelete = null;
	
	DBONode node;
	
	@After
	public void after() throws DatastoreException {
		if(dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectById(DBONode.class, params);
			}
		}
	}
	
	@Before
	public void before() throws DatastoreException, UnsupportedEncodingException{
		toDelete = new LinkedList<Long>();
		// Create a node to create revisions of.
		node = new DBONode();
		node.setId(idGenerator.generateNewId());
		toDelete.add(node.getId());
		node.setBenefactorId(node.getId());
		Long createdById = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		node.setCreatedBy(createdById);
		node.setCreatedOn(System.currentTimeMillis());
		node.setCurrentRevNumber(null);
		node.setDescription("A basic description".getBytes("UTF-8"));
		node.seteTag("0");
		node.setName("DBORevisionTest.baseNode");
		node.setParentId(null);
		node.setNodeType(EntityType.project.getId());
		dboBasicDao.createNew(node);
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		DBORevision rev = new DBORevision();
		rev.setOwner(node.getId());
		rev.setRevisionNumber(new Long(1));
		rev.setAnnotations(null);
		rev.setReferences(null);
		rev.setComment(null);
		rev.setLabel(""+rev.getRevisionNumber());
		Long createdById = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		rev.setModifiedBy(createdById);
		rev.setModifiedOn(System.currentTimeMillis());
		// Now create it
		rev = dboBasicDao.createNew(rev);
		// Make sure we can get it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("owner", rev.getOwner());
		params.addValue("revisionNumber", rev.getRevisionNumber());
		DBORevision clone = dboBasicDao.getObjectById(DBORevision.class, params);
		assertEquals(rev, clone);
		// Update with some values
		clone.setAnnotations("Fake annotations".getBytes("UTF-8"));
		clone.setReferences("Fake References".getBytes("UTF-8"));
		clone.setComment("No comment!");
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		// Fetch the updated
		DBORevision updatedClone = dboBasicDao.getObjectById(DBORevision.class, params);
		assertEquals(clone, updatedClone);
	}

}
