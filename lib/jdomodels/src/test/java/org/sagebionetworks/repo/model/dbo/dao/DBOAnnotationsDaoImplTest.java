package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAnnotationsDaoImplTest {

	@Autowired
	DBOAnnotationsDao dboAnnotationsDao;
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Autowired
	UserGroupDAO userGroupDAO;
	
	@Autowired
	private IdGenerator idGenerator;
	
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
		String createdBy = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		toDelete = new LinkedList<Long>();
		// Create a node to create revisions of.
		node = new DBONode();
		node.setId(idGenerator.generateNewId());
		toDelete.add(node.getId());
		node.setBenefactorId(node.getId());
		node.setCreatedBy(Long.parseLong(createdBy));
		node.setCreatedOn(System.currentTimeMillis());
		node.setCurrentRevNumber(null);
		node.setDescription("A basic description".getBytes("UTF-8"));
		node.seteTag(new Long(0));
		node.setName("DBOAnnotationsDaoImplTest.baseNode");
		node.setParentId(null);
		node.setNodeType(EntityType.project.getId());
		dboBasicDao.createNew(node);
	}
	
	@Test
	public void testStringAnnotations() throws DatastoreException{
		Annotations annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyOne", "valueOne");
		annos.addAnnotation("keyTwo", "valueTwo");
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		// Now get them back
		Annotations clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertEquals("valueOne", clone.getSingleValue("keyOne"));
		assertEquals("valueTwo", clone.getSingleValue("keyTwo"));
		
		// Make sure we can replace them
		annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyTwo", "valueTwo");
		annos.addAnnotation("keyThree", "valueThree");
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		// this was removed.
		assertEquals(null, clone.getSingleValue("keyOne"));
		assertEquals("valueTwo", clone.getSingleValue("keyTwo"));
		assertEquals("valueThree", clone.getSingleValue("keyThree"));
	}
	
	@Test
	public void testLongAnnotations() throws DatastoreException{
		Annotations annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyOne", new Long(123));
		annos.addAnnotation("keyTwo", new Long(345));
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		// Now get them back
		Annotations clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertEquals(new Long(123), clone.getSingleValue("keyOne"));
		assertEquals(new Long(345), clone.getSingleValue("keyTwo"));
		
		// Make sure we can replace them
		annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyTwo", new Long(345));
		annos.addAnnotation("keyThree", new Long(789));
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		// this was removed.
		assertEquals(null, clone.getSingleValue("keyOne"));
		assertEquals(new Long(345), clone.getSingleValue("keyTwo"));
		assertEquals(new Long(789), clone.getSingleValue("keyThree"));
	}
	
	@Test
	public void testDoubleAnnotations() throws DatastoreException{
		Annotations annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyOne", new Double(123.1));
		annos.addAnnotation("keyTwo", new Double(345.2));
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		// Now get them back
		Annotations clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertEquals(new Double(123.1), clone.getSingleValue("keyOne"));
		assertEquals(new Double(345.2), clone.getSingleValue("keyTwo"));
		
		// Make sure we can replace them
		annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyTwo", new Double(345.2));
		annos.addAnnotation("keyThree", new Double(789.3));
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		// this was removed.
		assertEquals(null, clone.getSingleValue("keyOne"));
		assertEquals(new Double(345.2), clone.getSingleValue("keyTwo"));
		assertEquals(new Double(789.3), clone.getSingleValue("keyThree"));
	}
	
	@Test
	public void testDateAnnotations() throws DatastoreException{
		Annotations annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyOne", new Date(1000));
		annos.addAnnotation("keyTwo", new Date(2000));
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		// Now get them back
		Annotations clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertEquals(new Date(1000), clone.getSingleValue("keyOne"));
		assertEquals(new Date(2000), clone.getSingleValue("keyTwo"));
		
		// Make sure we can replace them
		annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyTwo", new Date(2000));
		annos.addAnnotation("keyThree", new Date(3000));
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		// this was removed.
		assertEquals(null, clone.getSingleValue("keyOne"));
		assertEquals(new Date(2000), clone.getSingleValue("keyTwo"));
		assertEquals(new Date(3000), clone.getSingleValue("keyThree"));
	}
	
	@Test
	public void testOneOfEachAnnotations() throws DatastoreException{
		Annotations annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("stringKey", "String");
		annos.addAnnotation("longKey", new Long(123));
		annos.addAnnotation("doubleKey", new Double(1.11));
		annos.addAnnotation("dateKey", new Date(2000));
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		// Now get them back
		Annotations clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertEquals("String", clone.getSingleValue("stringKey"));
		assertEquals(new Long(123), clone.getSingleValue("longKey"));
		assertEquals(new Double(1.11), clone.getSingleValue("doubleKey"));
		assertEquals(new Date(2000), clone.getSingleValue("dateKey"));
	}
	
}
