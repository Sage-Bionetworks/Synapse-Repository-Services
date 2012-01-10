package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOReferenceDaoImplTest {

	@Autowired
	DBOReferenceDao dboReferenceDao;
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	List<Long> toDelete = null;
	
	// This is the node we will add refrence too.
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
	public void before() throws DatastoreException{
		toDelete = new LinkedList<Long>();
		// Create a node to create revisions of.
		node = new DBONode();
		node.setId(idGenerator.generateNewId());
		toDelete.add(node.getId());
		node.setBenefactorId(node.getId());
		node.setCreatedBy("DBOAnnotationsDaoImplTest");
		node.setCreatedOn(System.currentTimeMillis());
		node.setCurrentRevNumber(null);
		node.setDescription("A basic description");
		node.seteTag(new Long(0));
		node.setName("DBOAnnotationsDaoImplTest.baseNode");
		node.setParentId(null);
		node.setNodeType(EntityType.project.getId());
		dboBasicDao.createNew(node);
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException{
		// Build up two groups
		Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
		Set<Reference> one = new HashSet<Reference>();
		Set<Reference> two = new HashSet<Reference>();
		references.put("groupOne", one);
		references.put("groupTwo", two);
		// Add one to one
		Reference ref = new Reference();
		ref.setTargetId("123");
		ref.setTargetVersionNumber(new Long(1));
		one.add(ref);
		// Add one to two
		 ref = new Reference();
		ref.setTargetId("456");
		ref.setTargetVersionNumber(new Long(0));
		two.add(ref);
		// Now save to the DB
		dboReferenceDao.replaceReferences(node.getId(), references);
		// Now fetch them back
		Map<String, Set<Reference>> clone = dboReferenceDao.getReferences(node.getId());
		assertEquals(references, clone);
		// Now change the values and make sure we can replace them.
		clone.remove("groupOne");
		Set<Reference> three = new HashSet<Reference>();
		clone.put("groupThree", three);
		// Add one to two
		ref = new Reference();
		ref.setTargetId("789");
		ref.setTargetVersionNumber(new Long(10));
		three.add(ref);
		// Replace them 
		dboReferenceDao.replaceReferences(node.getId(), clone);
		Map<String, Set<Reference>> clone2 = dboReferenceDao.getReferences(node.getId());
		assertEquals(clone, clone2);
	}
	
	@Test
	public void testNullTargetRevNumber() throws DatastoreException{
		// Build up two groups
		Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
		Set<Reference> one = new HashSet<Reference>();
		Set<Reference> two = new HashSet<Reference>();
		references.put("groupOne", one);
		// Add one to one
		Reference ref = new Reference();
		ref.setTargetId("123");
		ref.setTargetVersionNumber(null);
		one.add(ref);
		// Add one to two
		 ref = new Reference();
		ref.setTargetId("456");
		ref.setTargetVersionNumber(new Long(12));
		two.add(ref);
		// Now save to the DB
		dboReferenceDao.replaceReferences(node.getId(), references);
		// Now fetch them back
		Map<String, Set<Reference>> clone = dboReferenceDao.getReferences(node.getId());
		assertEquals(references, clone);
	}
	
	@Test
	public void testUnique() throws DatastoreException{
		// Build up two groups
		Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
		Set<Reference> one = new HashSet<Reference>();
		references.put("groupOne", one);
		// Add one to one
		Reference ref = new Reference();
		ref.setTargetId("123");
		ref.setTargetVersionNumber(new Long(12));
		one.add(ref);
		// Add one to two
		ref = new Reference();
		ref.setTargetId("123");
		ref.setTargetVersionNumber(new Long(12));
		one.add(ref);
		// The set is actually enforcing this for us.
		assertEquals(1, one.size());
		// Now save to the DB
		dboReferenceDao.replaceReferences(node.getId(), references);
		// Now fetch them back
		Map<String, Set<Reference>> clone = dboReferenceDao.getReferences(node.getId());
		assertEquals(references, clone);
	}
	
	@Test
	public void testUniqueNull() throws DatastoreException{
		// Build up two groups
		Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
		Set<Reference> one = new HashSet<Reference>();
		references.put("groupOne", one);
		// Add one to one
		Reference ref = new Reference();
		ref.setTargetId("123");
		ref.setTargetVersionNumber(null);
		one.add(ref);
		// Add one to two
		ref = new Reference();
		ref.setTargetId("123");
		ref.setTargetVersionNumber(null);
		one.add(ref);
		// The set is actually enforcing this for us.
		assertEquals(1, one.size());
		// Now save to the DB
		dboReferenceDao.replaceReferences(node.getId(), references);
		// Now fetch them back
		Map<String, Set<Reference>> clone = dboReferenceDao.getReferences(node.getId());
		assertEquals(references, clone);
	}
	
	@Test
	public void testUniqueMixed() throws DatastoreException{
		// Build up two groups
		Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
		Set<Reference> one = new HashSet<Reference>();
		references.put("groupOne", one);
		// Add one to one
		Reference ref = new Reference();
		ref.setTargetId("123");
		ref.setTargetVersionNumber(new Long(1));
		one.add(ref);
		// Add one to two
		ref = new Reference();
		ref.setTargetId("123");
		ref.setTargetVersionNumber(null);
		one.add(ref);
		// The set is actually enforcing this for us.
		assertEquals(2, one.size());
		// Now save to the DB
		dboReferenceDao.replaceReferences(node.getId(), references);
		// Now fetch them back
		Map<String, Set<Reference>> clone = dboReferenceDao.getReferences(node.getId());
		assertEquals(references, clone);
	}
}
