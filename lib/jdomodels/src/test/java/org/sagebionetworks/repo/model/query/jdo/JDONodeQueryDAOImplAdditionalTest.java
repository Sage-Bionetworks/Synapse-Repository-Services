package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Additional tests for the node query.
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class JDONodeQueryDAOImplAdditionalTest {
	
	@Autowired
	NodeInheritanceDAO nodeInheritanceDao;
	
	@Autowired
	private NodeQueryDao nodeQueryDao;
	
	@Autowired
	private AsynchronousDAO asynchronousDAO;
	
	@Autowired
	private NodeDAO nodeDao;
	
	Long adminId;
	UserInfo userInfo;
	List<String> toDelete;
	Node project;
	
	@Before
	public void before() throws DatastoreException, InvalidModelException, NotFoundException{
		toDelete = new LinkedList<String>();
		adminId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		userInfo = new UserInfo(true, adminId);
		project = new Node();
		project.setName(UUID.randomUUID().toString());
		setMetadata(project, EntityType.project);
		String id = nodeDao.createNew(project);
		project.setId(id);
		toDelete.add(id);
	}
	
	@After
	public void afer(){
		if(toDelete != null){
			for(String id: toDelete){
				try {
					nodeDao.delete(id);
				} catch (Exception e) {}
			}
		}
	}
	
	/**
	 * Test that we can find nodes using the project ID
	 * @throws NotFoundException 
	 * @throws InvalidModelException 
	 * @throws DatastoreException 
	 */
	@Test
	public void testPLFM_3216() throws DatastoreException, InvalidModelException, NotFoundException{
		// parent
		Node parent = new Node();
		parent.setName("parent");
		parent.setParentId(project.getId());
		setMetadata(parent, EntityType.folder);
		parent.setId(nodeDao.createNew(parent));
		// child
		Node child = new Node();
		child.setName("child");
		child.setParentId(parent.getId());
		setMetadata(child, EntityType.folder);
		child.setId(nodeDao.createNew(child));
		
		// Query on projectId should return both
		BasicQuery query = new BasicQuery();
		query.setFrom("entity");
		// Filter by project Id.
		query.setFilters(new LinkedList<Expression>());
		query.getFilters().add(new Expression(new CompoundId(null, "projectId"), Comparator.EQUALS, project.getId()));
		NodeQueryResults results = nodeQueryDao.executeQuery(query, userInfo);
		assertNotNull(results);
		// The project plus its two children should be included.
		assertEquals(3, results.getTotalNumberOfResults());
		assertTrue(results.getResultIds().contains(project.getId()));
		assertTrue(results.getResultIds().contains(parent.getId()));
		assertTrue(results.getResultIds().contains(child.getId()));
	}
	
	/**
	 * Test that nodes in the trash can do not show up in node queries.
	 * @throws NotFoundException 
	 * @throws InvalidModelException 
	 * @throws DatastoreException 
	 */
	@Test
	public void testPLFM_3227() throws DatastoreException, InvalidModelException, NotFoundException{
		// parent
		Node toTrash = new Node();
		// set a random name to query for.
		toTrash.setName(UUID.randomUUID().toString());
		toTrash.setParentId(project.getId());
		setMetadata(toTrash, EntityType.folder);
		toTrash.setId(nodeDao.createNew(toTrash));
		toTrash = nodeDao.getNode(toTrash.getId());
		
		// Validate we can find the node before it is in the trash.
		BasicQuery query = new BasicQuery();
		query.setFrom("entity");
		// Filter by the name
		query.setFilters(new LinkedList<Expression>());
		query.getFilters().add(new Expression(new CompoundId(null, "name"), Comparator.EQUALS, toTrash.getName()));
		NodeQueryResults results = nodeQueryDao.executeQuery(query, userInfo);
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults());
		assertTrue(results.getResultIds().contains(toTrash.getId()));
		
		// Now move the node to the trash
		nodeInheritanceDao.addBeneficiary(toTrash.getId(), ""+JDONodeQueryDaoImpl.TRASH_FOLDER_ID);
		// We should not be able to find it
		results = nodeQueryDao.executeQuery(query, userInfo);
		assertNotNull(results);
		assertEquals(0, results.getTotalNumberOfResults());		
	}

	private void setMetadata(Node node, EntityType type){
		node.setCreatedByPrincipalId(adminId);
		node.setCreatedOn(new Date());
		node.setModifiedByPrincipalId(adminId);
		node.setModifiedOn(new Date());
		node.setNodeType(type.name());
	}
}
