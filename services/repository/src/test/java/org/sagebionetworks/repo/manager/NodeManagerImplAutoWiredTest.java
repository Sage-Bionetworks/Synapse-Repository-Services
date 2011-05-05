package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.Bootstrapper;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.aw.JDOUserGroupDAO;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * This is an integration test for the NodeManagerImpl.  Most of the testing should occur
 * in NodeManagerImpleUnitTest.
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:manager-test-context.xml" })
public class NodeManagerImplAutoWiredTest {
	
	@Autowired
	public NodeManager nodeManager;
	
	@Autowired
	Bootstrapper modelBootstrapper;
	
	@Autowired
	JDOUserGroupDAO userGroupDAO;

	
	List<String> nodesToDelete;
	
	@Before
	public void before() throws Exception{
		modelBootstrapper.bootstrap();
		assertNotNull(nodeManager);
		nodesToDelete = new ArrayList<String>();
	}
	
	@After
	public void after() throws Exception {
		if(nodeManager != null && nodesToDelete != null){
			for(String id: nodesToDelete){
				try {
					nodeManager.delete(AuthUtilConstants.ANONYMOUS_USER_ID, id);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 				
			}
		}
	}
	
//	private void createUser(String userName) {
//		JDOUserDAOImpl userDAO = new JDOUserDAOImpl(null/*AuthUtilConstants.ADMIN_USER_ID*/);
//		JDOExecutor<JDOUser> exec = new JDOExecutor<JDOUser>(jdoTemplate, JDOUser.class);
//		Collection<JDOUser> c = exec.execute("userId==pUserId", String.class.getName()+" pUserId", null, userName);
//		if (c.size()>1) throw new IllegalStateException(""+c.size()+" users with name "+userName);
//		JDOUser user = c.iterator().next();
//		if (user==null) {
//			user = new JDOUser();
//			user.setCreationDate(new Date());
//			user.setUserId(userName);
//			jdoTemplate.makePersistent(user);
//		}
//		// ensure individual group is created, and that <userName> is a member
//		JDOUserGroup ag = JDOUserGroupDAOImpl.getAdminGroup(pm);
//		if (ag==null) {
//			groupDAO.createAdminGroup(pm);
//			ag = JDOUserGroupDAOImpl.getAdminGroup(pm);
//		}
//		groupDAO.addUser(ag, user, pm);
//	}
	
	@Test
	public void testCreateAndUpdate() throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException{
		// Create a node
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testCreateNode");
		
		// need to enable Public to have 'create' access to 'someType'
		UserGroup publicGroup = userGroupDAO.getPublicGroup();
		assertNotNull(publicGroup);
		userGroupDAO.setCreatableTypes(publicGroup, Arrays.asList(new String[]{"someType"}));
		assertTrue(userGroupDAO.getCreatableTypes(publicGroup).toString(), userGroupDAO.getCreatableTypes(publicGroup).contains("someType"));
		newNode.setType("someType");
		String id = nodeManager.createNewNode(newNode, AuthUtilConstants.ANONYMOUS_USER_ID);
		assertNotNull(id);
		nodesToDelete.add(id);
		//Make sure we can get the node
		Node fetched = nodeManager.get(AuthUtilConstants.ANONYMOUS_USER_ID, id);
		assertNotNull(fetched);
		assertEquals(AuthUtilConstants.ANONYMOUS_USER_ID, fetched.getCreatedBy());
		assertEquals(AuthUtilConstants.ANONYMOUS_USER_ID, fetched.getModifiedBy());
		assertNotNull(fetched.getCreatedOn());
		assertNotNull(fetched.getModifiedOn());
		assertEquals(id, fetched.getId());
		assertEquals(newNode.getName(), fetched.getName());
		assertEquals(newNode.getType(), fetched.getType());
		assertNotNull(fetched.geteTag());
		
		// Now try to update the node
		String startingETag = fetched.geteTag();
		fetched.setName("mySecondName");
		Node updated = nodeManager.update(AuthUtilConstants.ANONYMOUS_USER_ID, fetched);
		assertNotNull(updated);
		// Make sure the result has a new eTag
		assertFalse(startingETag.equals(updated.geteTag()));
		// Now get it again
		Node fetchedAgain = nodeManager.get(AuthUtilConstants.ANONYMOUS_USER_ID, id);
		assertNotNull(fetchedAgain);
		assertEquals("mySecondName", fetchedAgain.getName());
		assertEquals(updated.geteTag(), fetchedAgain.geteTag());

	}
	
	@Test
	public void testUpdateAnnotations() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		// Create a node
		Node newNode = new Node();
		newNode.setName("NodeManagerImplAutoWiredTest.testUpdateAnnotations");
		newNode.setType("someType");
		String id = nodeManager.createNewNode(newNode, AuthUtilConstants.ANONYMOUS_USER_ID);
		assertNotNull(id);
		nodesToDelete.add(id);
		// First get the annotations for this node
		Annotations annos = nodeManager.getAnnotations(null, id);
		assertNotNull(annos);
		assertNotNull(annos.getEtag());
		String eTagBeforeUpdate = annos.getEtag();
		long before = Long.parseLong(eTagBeforeUpdate);
		String expectedEtagAfterUpdate = new Long(++before).toString();
		// Add some values
		annos.addAnnotation("longKey", new Long(1));
		// Now update the node
		Annotations updated = nodeManager.updateAnnotations(null, id, annos);
		assertNotNull(updated);
		Annotations copy = nodeManager.getAnnotations(null, id);
		assertEquals(updated,copy);
		// Make sure the eTag has changed
		assertEquals(expectedEtagAfterUpdate, copy.getEtag());
		Node nodeCopy = nodeManager.get(null, id);
		assertNotNull(nodeCopy);
		assertNotNull(nodeCopy.geteTag());
		assertEquals(expectedEtagAfterUpdate, nodeCopy.geteTag().toString());
	}
	

}
