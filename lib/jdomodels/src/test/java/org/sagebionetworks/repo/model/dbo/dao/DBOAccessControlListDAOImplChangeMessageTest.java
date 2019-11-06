package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAccessControlListDAOImplChangeMessageTest {

	@Autowired
	DBOChangeDAO changeDAO;
	@Autowired
	private NodeDAO nodeDAO;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private AccessControlListDAO aclDAO;

	private Long createdById;
	private Long modifiedById;

	private Node node;
	private String nodeId;
	private UserGroup group;
	private UserGroup group2;

	private Collection<Node> nodeList = new ArrayList<Node>();
	private Collection<UserGroup> groupList = new ArrayList<UserGroup>();
	private Collection<AccessControlList> aclList = new ArrayList<AccessControlList>();


	@Before
	public void setUp() throws Exception {
		createdById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

		// strictly speaking it's nonsensical for a group to be a 'modifier'.  we're just using it for testing purposes
		modifiedById = BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId();

		// create a resource on which to apply permissions
		node = new Node();
		node.setName("foo");
		node.setCreatedOn(new Date());
		node.setCreatedByPrincipalId(createdById);
		node.setModifiedOn(new Date());
		node.setModifiedByPrincipalId(modifiedById);
		node.setNodeType(EntityType.project);
		nodeId = nodeDAO.createNew(node);
		assertNotNull(nodeId);
		node = nodeDAO.getNode(nodeId);
		nodeList.add(node);

		// create a group to give the permissions to
		group = new UserGroup();
		group.setIsIndividual(false);
		group.setId(userGroupDAO.create(group).toString());
		assertNotNull(group.getId());
		groupList.add(group);

		// Create a second user
		group2 = new UserGroup();
		group2.setIsIndividual(false);
		group2.setId(userGroupDAO.create(group2).toString());
		assertNotNull(group2.getId());
		groupList.add(group2);
	}

	@Test
	public void testCreate() throws Exception {
		changeDAO.deleteAllChanges();

		// Create an ACL for this node
		AccessControlList acl = new AccessControlList();
		acl.setId(nodeId);
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		String aclId = aclDAO.create(acl, ObjectType.ENTITY);
		assertEquals(nodeId, aclId);
		aclList.add(acl);

		// Did a message get sent?
		List<ChangeMessage> changes = changeDAO.listChanges(changeDAO.getCurrentChangeNumber(), ObjectType.ACCESS_CONTROL_LIST, Long.MAX_VALUE);
		assertNotNull(changes);
		ChangeMessage message = changes.get(0);
		assertNotNull(message);
		assertEquals(ChangeType.CREATE, message.getChangeType());
		assertEquals(ObjectType.ACCESS_CONTROL_LIST, message.getObjectType());

		// TEST GET ACL USING Long Id
		assertEquals(acl, aclDAO.get(Long.parseLong(message.getObjectId())));

		// TEST getOwnerType USING Long Id
		assertEquals(ObjectType.ENTITY, aclDAO.getOwnerType(Long.parseLong(message.getObjectId())));
	}

	@Test
	public void testUpdate() throws Exception {
		changeDAO.deleteAllChanges();

		// Create an ACL for this node
		AccessControlList acl = new AccessControlList();
		acl.setId(nodeId);
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		String aclId = aclDAO.create(acl, ObjectType.ENTITY);
		assertEquals(nodeId, aclId);

		acl = aclDAO.get(node.getId(), ObjectType.ENTITY);
		assertNotNull(acl);
		assertNotNull(acl.getEtag());
		final String etagBeforeUpdate = acl.getEtag();
		assertEquals(nodeId, acl.getId());
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(Long.parseLong(group.getId()));
		ra.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.READ})));
		ras.add(ra);
		acl.setResourceAccess(ras);

		aclDAO.update(acl, ObjectType.ENTITY);
		acl = aclDAO.get(node.getId(), ObjectType.ENTITY);
		assertNotNull(acl);
		assertFalse(etagBeforeUpdate.equals(acl.getEtag()));
		ras = acl.getResourceAccess();
		assertEquals(1, ras.size());
		ResourceAccess raClone = ras.iterator().next();
		assertEquals(ra.getPrincipalId(), raClone.getPrincipalId());
		assertEquals(ra.getAccessType(), raClone.getAccessType());
		aclList.add(acl);

		// Did a message get sent?
		List<ChangeMessage> changes = changeDAO.listChanges(changeDAO.getCurrentChangeNumber(), ObjectType.ACCESS_CONTROL_LIST, Long.MAX_VALUE);
		assertNotNull(changes);
		ChangeMessage message = changes.get(0);
		assertNotNull(message);
		assertEquals(ChangeType.UPDATE, message.getChangeType());
		assertEquals(ObjectType.ACCESS_CONTROL_LIST, message.getObjectType());
		assertEquals(acl, aclDAO.get(Long.parseLong(message.getObjectId())));
	}

	@Test
	public void testDelete() throws Exception {
		changeDAO.deleteAllChanges();

		// Create an ACL for this node
		AccessControlList acl = new AccessControlList();
		acl.setId(nodeId);
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		String aclId = aclDAO.create(acl, ObjectType.ENTITY);
		assertEquals(nodeId, aclId);

		aclDAO.delete(nodeId, ObjectType.ENTITY);

		List<ChangeMessage> changes = changeDAO.listChanges(changeDAO.getCurrentChangeNumber(), ObjectType.ACCESS_CONTROL_LIST, Long.MAX_VALUE);
		assertNotNull(changes);
		ChangeMessage message = changes.get(0);
		assertNotNull(message);
		assertEquals(ChangeType.DELETE, message.getChangeType());
		assertEquals(ObjectType.ACCESS_CONTROL_LIST, message.getObjectType());
	}
	
	@Test
	public void testDeleteList() throws Exception{
		changeDAO.deleteAllChanges();
		
		// Create an ACL for this node
		AccessControlList acl = new AccessControlList();
		acl.setId(nodeId);
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		String aclId = aclDAO.create(acl, ObjectType.ENTITY);
		assertEquals(nodeId, aclId);
		
		List<Long> aclToBeDeletedList = new ArrayList<Long>();
		aclToBeDeletedList.add(KeyFactory.stringToKey(nodeId));
		
		aclDAO.delete(aclToBeDeletedList, ObjectType.ENTITY);
		
		
		List<ChangeMessage> changes = changeDAO.listChanges(changeDAO.getCurrentChangeNumber(), ObjectType.ACCESS_CONTROL_LIST, Long.MAX_VALUE);
		assertNotNull(changes);
		ChangeMessage message = changes.get(0);
		assertNotNull(message);
		assertEquals(ChangeType.DELETE, message.getChangeType());
		assertEquals(ObjectType.ACCESS_CONTROL_LIST, message.getObjectType());
	}

	@After
	public void cleanUp() throws Exception {
		for (AccessControlList acl : aclList) {
			aclDAO.delete(acl.getId(), ObjectType.ENTITY);
		}
		nodeList.clear();
		aclList.clear();
		for (UserGroup g : groupList) {
			userGroupDAO.delete(g.getId());
		}
		groupList.clear();
	}

}
