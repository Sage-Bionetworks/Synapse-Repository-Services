package org.sagebionetworks.repo.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ReferenceUtilTest {
	
	@Autowired
	private ReferenceUtil referenceUtil;
	
	@Autowired
	private NodeDAO nodeDao;
	
	private List<String> toDelete;
	
	private Node one = null;
	private Node two = null;
	
	// For FKs only
	private Long userGroupId;
	
	@Before
	public void before() throws NotFoundException, DatastoreException, InvalidModelException {
		userGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		toDelete = new ArrayList<String>();
		
		// Create two nodes to reference
		one = createNew("one");
		String id = this.nodeDao.createNew(one);
		toDelete.add(id);
		one = nodeDao.getNode(id);
		// Create a child
		two = createNew("two");
		two.setParentId(id);
		id = this.nodeDao.createNew(two);
		toDelete.add(id);
		two = nodeDao.getNode(id);
		two.setVersionComment("v2 dude!");
		two.setVersionLabel("1.0");
		nodeDao.createNewVersion(two);
		// Get ti back
		two = nodeDao.getNode(id);
	}
	
	@After
	public void after(){
		if(nodeDao != null && toDelete != null){
			for(String id: toDelete){
				try {
					nodeDao.delete(id);
				} catch (Exception e) {} 
			}
		}
	}
	
	/**
	 * Create a new node using basic data.
	 * @param name
	 * @return
	 */
	public  Node createNew(String name){
		Node node = new Node();
		node.setName(name);
		assertNotNull(userGroupId);
		node.setCreatedByPrincipalId(userGroupId);
		node.setModifiedByPrincipalId(userGroupId);
		node.setCreatedOn(new Date(System.currentTimeMillis()));
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(EntityType.project);
		return node;
	}
	
	@Test
	public void replaceNullVersionNumbersWithCurrent() throws DatastoreException{
		Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
		Set<Reference> refSet = new HashSet<Reference>();
		references.put("groupOne", refSet);
		// one
		Reference ref = new Reference();
		ref.setTargetId(one.getId());
		ref.setTargetVersionNumber(null);
		refSet.add(ref);
		// two
		ref = new Reference();
		ref.setTargetId(two.getId());
		ref.setTargetVersionNumber(null);
		refSet.add(ref);
		// Now Have the current version set
		referenceUtil.replaceNullVersionNumbersWithCurrent(references);
		assertNotNull(references);
		assertEquals(1, references.size());
		Set<Reference> refSetMod = references.get("groupOne");
		assertNotNull(refSetMod);
		Iterator<Reference> refIt = refSetMod.iterator();
		Reference refToOne = null;
		Reference refToTwo = null;
		while(refIt.hasNext()){
			Reference fromIt = refIt.next();
			if(fromIt.getTargetId().equals(one.getId())){
				refToOne = fromIt;
			}else if(fromIt.getTargetId().equals(two.getId())){
				refToTwo = fromIt;
			}
		}
		assertNotNull(refToOne);
		assertNotNull(refToTwo);
		assertNotNull(one.getVersionNumber());
		assertNotNull(two.getVersionNumber());
		assertEquals(one.getVersionNumber(),  refToOne.getTargetVersionNumber());
		assertEquals(two.getVersionNumber(),  refToTwo.getTargetVersionNumber());
	}

	
}
