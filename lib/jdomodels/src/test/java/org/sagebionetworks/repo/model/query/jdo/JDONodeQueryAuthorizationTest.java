package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FieldTypeDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Compartor;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class JDONodeQueryAuthorizationTest implements InitializingBean{
	
	private static Log log = LogFactory.getLog(JDONodeQueryAuthorizationTest.class);

	@Autowired
	private NodeQueryDao nodeQueryDao;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private FieldTypeDAO fieldTypeDao;

	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private AccessControlListDAO accessControlListDAO;
	
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
	
	static private Map<String, UserInfo> usersInGroupA;
	static private Map<String, UserInfo> usersInGroupB;
	static private Map<String, UserInfo> usersInBothGroups;
	
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
		assertNotNull(fieldTypeDao);
		assertNotNull(userGroupDAO);
	}
	
	@Test
	public void testAdminProjectQuery() throws DatastoreException{
		assertNotNull(adminUser);
		// An administrator can see everything.
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.project);
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
		query.setFrom(EntityType.dataset);
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
		query.setFrom(EntityType.project);
		query.setOffset(0);
		query.setLimit(1000);
		query.setSort("name");
		query.setAscending(true);
		// Try each of the Group A users
		Iterator<String> it = usersInGroupA.keySet().iterator();
		while(it.hasNext()){
			String name = it.next();
			UserInfo userInfo = usersInGroupA.get(name);
			long start = System.currentTimeMillis();
			NodeQueryResults results = nodeQueryDao.executeQuery(query, userInfo);
			long end = System.currentTimeMillis();
			log.info("testUsersGroupAProjectQuery userId: "+name+" : "+(end-start)+"ms");
			assertNotNull(results);
			assertEquals("User: "+name+" should have only been able to see one project.", 1, results.getTotalNumberOfResults());
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
		query.setFrom(EntityType.project);
		query.setOffset(0);
		query.setLimit(1000);
		query.setSort("name");
		query.setAscending(true);
		// Try each of the Group A users
		Iterator<String> it = usersInGroupB.keySet().iterator();
		while(it.hasNext()){
			String name = it.next();
			UserInfo userInfo = usersInGroupB.get(name);
			long start = System.currentTimeMillis();
			NodeQueryResults results = nodeQueryDao.executeQuery(query, userInfo);
			long end = System.currentTimeMillis();
			log.info("testUsersGroupBProjectQuery userId: "+name+" : "+(end-start)+"ms");
			assertNotNull(results);
			assertEquals("User: "+name+" should have only been able to see one project.", 1, results.getTotalNumberOfResults());
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
		query.setFrom(EntityType.dataset);
		query.setOffset(0);
		query.setLimit(1000);
		query.setSort("name");
		query.setAscending(true);
		// Try each of the Group A users
		Iterator<String> it = usersInGroupA.keySet().iterator();
		while(it.hasNext()){
			String name = it.next();
			UserInfo userInfo = usersInGroupA.get(name);
			long start = System.currentTimeMillis();
			NodeQueryResults results = nodeQueryDao.executeQuery(query, userInfo);
			long end = System.currentTimeMillis();
			log.info("testUsersGroupADatasetQuery userId: "+name+" : "+(end-start)+"ms");
			assertNotNull(results);
			assertEquals("User: "+name+" should have been able to see "+nodesInProjectA.size()+" datasets", nodesInProjectA.size(), results.getTotalNumberOfResults());
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
		query.setFrom(EntityType.dataset);
		query.setOffset(0);
		query.setLimit(1000);
		query.setSort("name");
		query.setAscending(true);
		// Try each of the Group A users
		Iterator<String> it = usersInBothGroups.keySet().iterator();
		while(it.hasNext()){
			String name = it.next();
			UserInfo userInfo = usersInBothGroups.get(name);
			long start = System.currentTimeMillis();
			NodeQueryResults results = nodeQueryDao.executeQuery(query, userInfo);
			long end = System.currentTimeMillis();
			log.info("testUsersInBothGroupsDatasetQuery userId: "+name+" : "+(end-start)+"ms");
			assertNotNull(results);
			int expectedTotalCount = nodesInProjectA.size() + nodesInProjectB.size();
			assertEquals("User: "+name+" should have been able to see "+expectedTotalCount+" datasets", expectedTotalCount, results.getTotalNumberOfResults());
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
		query.setFrom(EntityType.dataset);
		query.setOffset(0);
		query.setLimit(1000);
		query.setSort(attributeName);
		query.setAscending(true);
		query.addExpression(new Expression(new CompoundId(null, attributeName), Compartor.GREATER_THAN, 0));
		Iterator<String> it = usersInGroupB.keySet().iterator();
		while(it.hasNext()){
			String name = it.next();
			UserInfo userInfo = usersInGroupB.get(name);
			long start = System.currentTimeMillis();
			NodeQueryResults results = nodeQueryDao.executeQuery(query, userInfo);
			long end = System.currentTimeMillis();
			log.info("testQueryWithAnnotations userId: "+name+" : "+(end-start)+"ms");
			assertNotNull(results);
			// The first node should be filtered out since it will have a value == 0
			int expectedCount = nodesInProjectB.size()-1;
			assertEquals("User: "+name+" should have been able to see "+expectedCount+" datasets", expectedCount, results.getTotalNumberOfResults());
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
	private UserInfo createUser(String name, boolean isAdmin) throws DatastoreException, InvalidModelException, NotFoundException{
		User user = new User();
		user.setUserId(name);
		// Create a group for this user
		UserGroup group = new UserGroup();
		group.setName(name+"group");
		group.setIndividual(true);
		String id = userGroupDAO.create(group);
		groupsToDelete.add(id);
		group = userGroupDAO.get(id);
		UserInfo info = new UserInfo(isAdmin);
		info.setUser(user);
		info.setIndividualGroup(group);
		info.setGroups(new ArrayList<UserGroup>());
		info.getGroups().add(group);

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
	private UserGroup createGroup(String name) throws DatastoreException, InvalidModelException, NotFoundException{
		UserGroup group = new UserGroup();
		group.setName(name);
		group.setIndividual(false);
		String id = userGroupDAO.create(group);
		groupsToDelete.add(id);
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
		if(instance != null){
			return;
		}
		instance = this;
		// Keeps track of the users to delete
		nodesToDelete = new ArrayList<String>();
		groupsToDelete = new ArrayList<String>();
		// Create some users
		adminUser= createUser("admin@JDONodeQueryAuthorizationTest.org", true);
		// Create two groups
		groupA = createGroup("groupA@JDONodeQueryAuthorizationTest.org");
		groupB = createGroup("groupB@JDONodeQueryAuthorizationTest.org");
		// Create users in each group
		usersInGroupA = new HashMap<String, UserInfo>();
		for(int i=0; i<10; i++){
			// Create all of the users
			String userId = "userInA"+i+"@JDONodeQueryAuthorizationTest.org";
			UserInfo info = createUser(userId, false);
			info.getGroups().add(groupA);
			usersInGroupA.put(userId, info);
		}
		// Create group b users
		usersInGroupB = new HashMap<String, UserInfo>();
		for(int i=0; i<7; i++){
			// Create all of the users
			String userId = "userInB"+i+"@JDONodeQueryAuthorizationTest.org";
			UserInfo info = createUser(userId, false);
			info.getGroups().add(groupB);
			usersInGroupB.put(userId, info);
		}
		
		// Create users in both groups
		usersInBothGroups = new HashMap<String, UserInfo>();
		for(int i=0; i<7; i++){
			// Create all of the users
			String userId = "userInBothA&B"+i+"@JDONodeQueryAuthorizationTest.org";
			UserInfo info = createUser(userId, false);
			info.getGroups().add(groupB);
			info.getGroups().add(groupA);
			usersInBothGroups.put(userId, info);
		}
		
		// Now create the two projects
		//Project A
		projectA = NodeTestUtils.createNew("projectA");
		projectA.setNodeType(EntityType.project.name());
		String id = nodeDao.createNew(projectA);
		nodesToDelete.add(id);
		projectA = nodeDao.getNode(id);
		// Create the ACL for this node.
		AccessControlList acl = AccessControlList.createACLToGrantAll(id, adminUser);
		// Make sure group A can read from this node
		ResourceAccess access = new ResourceAccess();
		access.setGroupName(groupA.getName());
		access.setAccessType(new HashSet<ACCESS_TYPE>());
		access.getAccessType().add(ACCESS_TYPE.READ);
		acl.getResourceAccess().add(access);
		accessControlListDAO.create(acl);
		// Project B
		projectB = NodeTestUtils.createNew("projectB");
		projectB.setNodeType(EntityType.project.name());
		id = nodeDao.createNew(projectB);
		nodesToDelete.add(id);
		projectB = nodeDao.getNode(id);
		// Create the ACL for this node.
		acl = AccessControlList.createACLToGrantAll(id, adminUser);
		// Make sure group B can read from this node
		access = new ResourceAccess();
		access.setGroupName(groupB.getName());
		access.setAccessType(new HashSet<ACCESS_TYPE>());
		access.getAccessType().add(ACCESS_TYPE.READ);
		acl.getResourceAccess().add(access);
		accessControlListDAO.create(acl);
		
		// Now add some nodes to each project.
		nodesInProjectA = new HashMap<String, Node>();
		for(int i=0; i<25; i++){
			Node node = NodeTestUtils.createNew("nodeInProjectA"+i);
			node.setNodeType(EntityType.dataset.name());
			node.setParentId(projectA.getId());
			id = nodeDao.createNew(node);
//			nodesToDelete.add(id);
			node = nodeDao.getNode(id);
			nodesInProjectA.put(node.getId(), node);
		}
		
		// Now add some nodes to each project.
		nodesInProjectB = new HashMap<String, Node>();
		for(int i=0; i<25; i++){
			Node node = NodeTestUtils.createNew("nodeInProjectB"+i);
			node.setNodeType(EntityType.dataset.name());
			node.setParentId(projectB.getId());
			id = nodeDao.createNew(node);
//			nodesToDelete.add(id);
			node = nodeDao.getNode(id);
			nodesInProjectB.put(node.getId(), node);
			// Add an attribute to nodes in group B
			NamedAnnotations annos = nodeDao.getAnnotations(id);
			assertNotNull(annos);
			fieldTypeDao.addNewType(attributeName, FieldType.LONG_ATTRIBUTE);
			annos.getAdditionalAnnotations().addAnnotation(attributeName, new Long(i));
			nodeDao.updateAnnotations(id, annos);
		}
	}

}
