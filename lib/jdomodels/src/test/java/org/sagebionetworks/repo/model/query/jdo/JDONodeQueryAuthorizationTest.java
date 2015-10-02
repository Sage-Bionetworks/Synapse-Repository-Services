package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class JDONodeQueryAuthorizationTest implements InitializingBean{
	
	private static Logger log = LogManager.getLogger(JDONodeQueryAuthorizationTest.class);

	@Autowired
	private NodeQueryDao nodeQueryDao;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private AccessControlListDAO accessControlListDAO;
	
	@Autowired
	private AsynchronousDAO asynchronousDAO;
	
	private volatile static JDONodeQueryAuthorizationTest instance = null;

	// Keep track of the groups that must be
	// deleted at the end.
	static List<String> groupsToDelete;
	static List<String> nodesToDelete;
	static private UserInfo adminUser;
	static private UserGroup groupA;
	static private UserGroup groupB;
	static private Node projectA;
	static private Node projectB;
	static private String attributeName = "JDONodeQueryAuthorizationTest.LongAttName";
	
	static private Map<Long, UserInfo> usersInGroupA;
	static private Map<Long, UserInfo> usersInGroupB;
	static private Map<Long, UserInfo> usersInBothGroups;
	
	static private Map<String, Node> nodesInProjectA;
	static private Map<String, Node> nodesInProjectB;
	

	/**
	 * Setup for this tests is expensive so it only occurs once.
	 * @see #afterPropertiesSet()
	 * @throws Exception
	 */
	@Before
	public void before() throws Exception {
		assertNotNull(nodeQueryDao);
		assertNotNull(nodeDao);
		assertNotNull(userGroupDAO);
	}
	
	@Test
	public void testAdminProjectQuery() throws DatastoreException{
		assertNotNull(adminUser);
		// An administrator can see everything.
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.project.name());
		query.setOffset(0);
		query.setLimit(1000);
		query.setSort("name");
		query.setAscending(true);
		long start = System.currentTimeMillis();
		NodeQueryResults results = nodeQueryDao.executeQuery(query, adminUser);
		long end = System.currentTimeMillis();
		log.info("testAdminProjectQuery: "+(end-start)+"ms");
		assertNotNull(results);
		// There are two projects
		assertTrue(results.getTotalNumberOfResults() >= 2);
		List<String> idList = results.getResultIds();
		assertNotNull(idList);
		assertTrue(idList.size() >= 2);
	}
	
	@Test
	public void testAdminDatasetQuery() throws DatastoreException{
		assertNotNull(adminUser);
		// An administrator can see everything.
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.link.name());
		query.setOffset(0);
		query.setLimit(1000);
		query.setSort("name");
		query.setAscending(true);
		long start = System.currentTimeMillis();
		NodeQueryResults results = nodeQueryDao.executeQuery(query, adminUser);
		long end = System.currentTimeMillis();
		log.info("testAdminProjectQuery: "+(end-start)+"ms");
		assertNotNull(results);
		// There are two projects
		int totalCount = nodesInProjectA.size() + nodesInProjectB.size();
		assertEquals(totalCount, results.getTotalNumberOfResults());
	}
	
	@Test
	public void testUsersGroupAProjectQuery() throws DatastoreException{
		assertNotNull(adminUser);
		// An administrator can see everything.
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.project.name());
		query.setOffset(0);
		query.setLimit(1000);
		query.setSort("name");
		query.setAscending(true);
		// Try each of the Group A users
		Iterator<Long> it = usersInGroupA.keySet().iterator();
		while(it.hasNext()){
			Long userId = it.next();
			UserInfo userInfo = usersInGroupA.get(userId);
			long start = System.currentTimeMillis();
			NodeQueryResults results = nodeQueryDao.executeQuery(query, userInfo);
			long end = System.currentTimeMillis();
			log.info("testUsersGroupAProjectQuery userId: "+userId+" : "+(end-start)+"ms");
			assertNotNull(results);
			assertEquals("User: "+userId+" should have only been able to see one project.", 1, results.getTotalNumberOfResults());
			List<String> idList = results.getResultIds();
			assertNotNull(idList);
			assertEquals(1, idList.size());
			String id = idList.get(0);
			// This should be projectA
			assertEquals(id, projectA.getId());
		}
	}
	
	@Test
	public void testUsersGroupBProjectQuery() throws DatastoreException{
		assertNotNull(adminUser);
		// An administrator can see everything.
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.project.name());
		query.setOffset(0);
		query.setLimit(1000);
		query.setSort("name");
		query.setAscending(true);
		// Try each of the Group A users
		Iterator<Long> it = usersInGroupB.keySet().iterator();
		while(it.hasNext()){
			Long userId = it.next();
			UserInfo userInfo = usersInGroupB.get(userId);
			long start = System.currentTimeMillis();
			NodeQueryResults results = nodeQueryDao.executeQuery(query, userInfo);
			long end = System.currentTimeMillis();
			log.info("testUsersGroupBProjectQuery userId: "+userId+" : "+(end-start)+"ms");
			assertNotNull(results);
			assertEquals("User: "+userId+" should have only been able to see one project.", 1, results.getTotalNumberOfResults());
			List<String> idList = results.getResultIds();
			assertNotNull(idList);
			assertEquals(1, idList.size());
			String id = idList.get(0);
			// This should be projectA
			assertEquals(id, projectB.getId());
		}
	}
	
	@Test
	public void testUsersGroupADatasetQuery() throws DatastoreException{
		assertNotNull(adminUser);
		// An administrator can see everything.
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.link.name());
		query.setOffset(0);
		query.setLimit(1000);
		query.setSort("name");
		query.setAscending(true);
		// Try each of the Group A users
		Iterator<Long> it = usersInGroupA.keySet().iterator();
		while(it.hasNext()){
			Long userId = it.next();
			UserInfo userInfo = usersInGroupA.get(userId);
			long start = System.currentTimeMillis();
			NodeQueryResults results = nodeQueryDao.executeQuery(query, userInfo);
			long end = System.currentTimeMillis();
			log.info("testUsersGroupADatasetQuery userId: "+userId+" : "+(end-start)+"ms");
			assertNotNull(results);
			assertEquals("User: "+userId+" should have been able to see "+nodesInProjectA.size()+" datasets", nodesInProjectA.size(), results.getTotalNumberOfResults());
			List<String> idList = results.getResultIds();
			assertNotNull(idList);
			assertEquals(nodesInProjectA.size(), idList.size());
			// Validate each node
			for(String nodeId: idList){
				assertNotNull(nodesInProjectA.get(nodeId));
			}
		}
	}
	
	@Test
	public void testUsersInBothGroupsDatasetQuery() throws DatastoreException{
		assertNotNull(adminUser);
		// An administrator can see everything.
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.link.name());
		query.setOffset(0);
		query.setLimit(1000);
		query.setSort("name");
		query.setAscending(true);
		// Try each of the Group A users
		Iterator<Long> it = usersInBothGroups.keySet().iterator();
		while(it.hasNext()){
			Long userId = it.next();
			UserInfo userInfo = usersInBothGroups.get(userId);
			long start = System.currentTimeMillis();
			NodeQueryResults results = nodeQueryDao.executeQuery(query, userInfo);
			long end = System.currentTimeMillis();
			log.info("testUsersInBothGroupsDatasetQuery userId: "+userId+" : "+(end-start)+"ms");
			assertNotNull(results);
			int expectedTotalCount = nodesInProjectA.size() + nodesInProjectB.size();
			assertEquals("User: "+userId+" should have been able to see "+expectedTotalCount+" datasets", expectedTotalCount, results.getTotalNumberOfResults());
			List<String> idList = results.getResultIds();
			assertNotNull(idList);
			assertEquals(expectedTotalCount, idList.size());
			// Validate each node
			for(String nodeId: idList){
				// This node could be in project A or B.
				Node node = nodesInProjectA.get(nodeId);
				if(node == null){
					node = nodesInProjectB.get(nodeId);
				}
				assertNotNull(node);
			}
		}
	}
	
	@Test
	public void testQueryWithAnnotations() throws DatastoreException{
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.link.name());
		query.setOffset(0);
		query.setLimit(1000);
		query.setSort(attributeName);
		query.setAscending(true);
		query.addExpression(new Expression(new CompoundId(null, attributeName), Comparator.GREATER_THAN, 0));
		Iterator<Long> it = usersInGroupB.keySet().iterator();
		while(it.hasNext()){
			Long userId = it.next();
			UserInfo userInfo = usersInGroupB.get(userId);
			long start = System.currentTimeMillis();
			NodeQueryResults results = nodeQueryDao.executeQuery(query, userInfo);
			long end = System.currentTimeMillis();
			log.info("testQueryWithAnnotations userId: "+userId+" : "+(end-start)+"ms");
			assertNotNull(results);
			// The first node should be filtered out since it will have a value == 0
			int expectedCount = nodesInProjectB.size()-1;
			assertEquals("User: "+userId+" should have been able to see "+expectedCount+" datasets", expectedCount, results.getTotalNumberOfResults());
			List<String> idList = results.getResultIds();
			assertNotNull(idList);
			assertEquals(expectedCount, idList.size());
			// Validate each node
			for(String nodeId: idList){
				assertNotNull(nodesInProjectB.get(nodeId));
			}
		}
	}
	
	
	/**
	 * Helper for creating a new user.
	 * @param name
	 * @param isAdmin
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	private UserInfo createUser(boolean isAdmin) throws DatastoreException, InvalidModelException, NotFoundException{
		// Create a group for this user
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		Long id = userGroupDAO.create(user);
		groupsToDelete.add(id.toString());
		user = userGroupDAO.get(id);
		UserInfo info = new UserInfo(isAdmin);
		info.setId(id);
		info.setGroups(new HashSet<Long>());
		// add the user to their own groups
		info.getGroups().add(id);

		return info;
	}
	
	/**
	 * Helper to create a group.
	 * @param name
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	private UserGroup createGroup() throws DatastoreException, InvalidModelException, NotFoundException{
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		Long id = userGroupDAO.create(group);
		groupsToDelete.add(id.toString());
		return userGroupDAO.get(id);
	}

	@AfterClass
	public static void afterClass() {
		// Cleanup groups
		if (instance.userGroupDAO != null) {
			if(groupsToDelete != null){
				for(String id: groupsToDelete){
					try{
						instance.userGroupDAO.delete(id);
					}catch(Throwable e){
						
					}
				}
			}
		}
		if(instance.nodeDao != null && nodesToDelete != null){
			for(String id: nodesToDelete){
				try{
					instance.nodeDao.delete(id);
				}catch (Throwable e){
				}
			}
		}
	}


	/**
	 * The setup for this test is expensive so we only do it once after all of the
	 * beans are ready.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Long creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

		if(instance != null){
			return;
		}
		instance = this;
		// These are all of the nodes we need to update.
		List<String> toUpdate = new LinkedList<String>();
		// Keeps track of the users to delete
		nodesToDelete = new ArrayList<String>();
		groupsToDelete = new ArrayList<String>();
		// Create some users
		adminUser= createUser(true);
		// Create two groups
		groupA = createGroup();
		groupB = createGroup();
		// Create users in each group
		usersInGroupA = new HashMap<Long, UserInfo>();
		for(int i=0; i<10; i++){
			// Create all of the users
			UserInfo info = createUser(false);
			info.getGroups().add(Long.parseLong(groupA.getId()));
			usersInGroupA.put(info.getId(), info);
		}
		// Create group b users
		usersInGroupB = new HashMap<Long, UserInfo>();
		for(int i=0; i<7; i++){
			// Create all of the users
			UserInfo info = createUser(false);
			info.getGroups().add(Long.parseLong(groupB.getId()));
			usersInGroupB.put(info.getId(), info);
		}
		
		// Create users in both groups
		usersInBothGroups = new HashMap<Long, UserInfo>();
		for(int i=0; i<7; i++){
			// Create all of the users
			UserInfo info = createUser(false);
			info.getGroups().add(Long.parseLong(groupB.getId()));
			info.getGroups().add(Long.parseLong(groupA.getId()));
			usersInBothGroups.put(info.getId(), info);
		}
		
		// Now create the two projects
		//Project A
		projectA = NodeTestUtils.createNew("projectA", creatorUserGroupId);
		projectA.setNodeType(EntityType.project);
		String id = nodeDao.createNew(projectA);
		toUpdate.add(id);
		nodesToDelete.add(id);
		projectA = nodeDao.getNode(id);
		// Create the ACL for this node.
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(id, adminUser);
		// Make sure group A can read from this node
		ResourceAccess access = new ResourceAccess();
		access.setPrincipalId(Long.parseLong(groupA.getId()));
		access.setAccessType(new HashSet<ACCESS_TYPE>());
		access.getAccessType().add(ACCESS_TYPE.READ);
		acl.getResourceAccess().add(access);
		accessControlListDAO.create(acl, ObjectType.ENTITY);
		// Project B
		projectB = NodeTestUtils.createNew("projectB", creatorUserGroupId);
		projectB.setNodeType(EntityType.project);
		id = nodeDao.createNew(projectB);
		nodesToDelete.add(id);
		toUpdate.add(id);
		projectB = nodeDao.getNode(id);
		// Create the ACL for this node.
		acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(id, adminUser);
		// Make sure group B can read from this node
		access = new ResourceAccess();
		access.setPrincipalId(Long.parseLong(groupB.getId()));
		access.setAccessType(new HashSet<ACCESS_TYPE>());
		access.getAccessType().add(ACCESS_TYPE.READ);
		acl.getResourceAccess().add(access);
		accessControlListDAO.create(acl, ObjectType.ENTITY);
		
		// Now add some nodes to each project.
		nodesInProjectA = new HashMap<String, Node>();
		for(int i=0; i<25; i++){
			Node node = NodeTestUtils.createNew("nodeInProjectA"+i, creatorUserGroupId);
			node.setNodeType(EntityType.link);
			node.setParentId(projectA.getId());
			id = nodeDao.createNew(node);
			toUpdate.add(id);
//			nodesToDelete.add(id);
			node = nodeDao.getNode(id);
			nodesInProjectA.put(node.getId(), node);
		}
		
		// Now add some nodes to each project.
		nodesInProjectB = new HashMap<String, Node>();
		for(int i=0; i<25; i++){
			Node node = NodeTestUtils.createNew("nodeInProjectB"+i, creatorUserGroupId);
			node.setNodeType(EntityType.link);
			node.setParentId(projectB.getId());
			id = nodeDao.createNew(node);
			toUpdate.add(id);
//			nodesToDelete.add(id);
			node = nodeDao.getNode(id);
			nodesInProjectB.put(node.getId(), node);
			// Add an attribute to nodes in group B
			NamedAnnotations annos = nodeDao.getAnnotations(id);
			assertNotNull(annos);
//			fieldTypeDao.addNewType(attributeName, FieldType.LONG_ATTRIBUTE);
			annos.getAdditionalAnnotations().addAnnotation(attributeName, new Long(i));
			nodeDao.updateAnnotations(id, annos);
		}
		// since we have moved the annotation updates to an asynchronous process we need to manually
		// update the annotations of all nodes for this test. See PLFM-1548
		for(String entityId: toUpdate){
			asynchronousDAO.createEntity(entityId);
		}
	}

}
