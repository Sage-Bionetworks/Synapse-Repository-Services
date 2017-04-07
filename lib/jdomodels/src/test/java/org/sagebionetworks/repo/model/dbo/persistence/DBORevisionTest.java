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
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBORevisionTest {
	
	@Autowired
	private DBOBasicDao dboBasicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private List<Long> toDelete = null;
	
	private DBONode node;
	
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
	public void before() throws DatastoreException, UnsupportedEncodingException{
		toDelete = new LinkedList<Long>();
		// Create a node to create revisions of.
		node = new DBONode();
		node.setId(idGenerator.generateNewId(IdType.ENTITY_ID));
		toDelete.add(node.getId());
		node.setBenefactorId(node.getId());
		Long createdById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		node.setCreatedBy(createdById);
		node.setCreatedOn(System.currentTimeMillis());
		node.setCurrentRevNumber(null);
		node.setDescription("A basic description".getBytes("UTF-8"));
		node.seteTag("0");
		node.setName("DBORevisionTest.baseNode");
		node.setParentId(null);
		node.setType(EntityType.project.name());
		dboBasicDao.createNew(node);
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		DBORevision rev = new DBORevision();
		rev.setOwner(node.getId());
		rev.setRevisionNumber(new Long(1));
		rev.setAnnotations(null);
		rev.setReference(null);
		rev.setComment(null);
		rev.setLabel(""+rev.getRevisionNumber());
		Long createdById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		rev.setModifiedBy(createdById);
		rev.setModifiedOn(System.currentTimeMillis());
		// Now create it
		rev = dboBasicDao.createNew(rev);
		// Make sure we can get it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("owner", rev.getOwner());
		params.addValue("revisionNumber", rev.getRevisionNumber());
		DBORevision clone = dboBasicDao.getObjectByPrimaryKey(DBORevision.class, params);
		assertEquals(rev, clone);
		// Update with some values
		clone.setAnnotations("Fake annotations".getBytes("UTF-8"));
		Reference ref = new Reference();
		byte[] blob = JDOSecondaryPropertyUtils.compressReference(ref);
		clone.setReference(blob);
		clone.setComment("No comment!");
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		// Fetch the updated
		DBORevision updatedClone = dboBasicDao.getObjectByPrimaryKey(DBORevision.class, params);
		assertEquals(clone, updatedClone);
	}

}
