package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.provenance.UsedEntity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.IllegalTransactionStateException;

/**
 * This test of the DBOFavoriteDAOImpl is only for DB function and DB enforced 
 * business logic. Put tests for DAO business logic in DBOFavoriteDAOImplTest
 * @author dburdick
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOFavoriteDAOImplAutowiredTest {

	@Autowired
	FavoriteDAO favoriteDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private NodeDAO nodeDao;
	
	// must be deleted at the end of each test.
	List<Favorite> favoritesToDelete;
	List<String> nodesToDelete;
	
	private UserInfo userInfo = null;
	private Long creatorUserGroupId = null;	
	private Long altUserGroupId = null;
	
	long initialCount; 

	@Before
	public void before() throws Exception {
		favoritesToDelete = new ArrayList<Favorite>();
		nodesToDelete = new ArrayList<String>();
		
		creatorUserGroupId = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		assertNotNull(creatorUserGroupId);
		
		altUserGroupId = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.DEFAULT_GROUPS.AUTHENTICATED_USERS.name(), false).getId());
		assertNotNull(altUserGroupId);
		
		assertNotNull(favoriteDao);
		
		initialCount = favoriteDao.getCount();
	}
	
	@After
	public void after() throws Exception {
		// delete activites
		if(favoritesToDelete != null && favoriteDao != null){
			for(Favorite fav : favoritesToDelete){
				// Delete each
				try {
					favoriteDao.remove(fav.getPrincipalId(), fav.getEntityId());
				} catch (Exception e) {
					// keep deleting
				}
			}
		}
		
		// delete nodes
		if(nodesToDelete != null && nodeDao != null){
			for(String id:  nodesToDelete){
				// Delete each
				try{
					nodeDao.delete(id);
				}catch (NotFoundException e) {
					// happens if the object no longer exists.
				}
			}
		}

	}
		
	@Test 
	public void testAdd() throws Exception{
		// make two nodes  
		Node node1 = NodeTestUtils.createNew("n1", creatorUserGroupId, creatorUserGroupId);
		String node1Id = nodeDao.createNew(node1);		
		nodesToDelete.add(node1Id);
		Node node2 = NodeTestUtils.createNew("n2", creatorUserGroupId, creatorUserGroupId);
		String node2Id = nodeDao.createNew(node2);
		nodesToDelete.add(node2Id);
		
		Favorite fav1 = createFavorite(node1Id);
		Favorite fav2 = createFavorite(node2Id);
		
		Favorite fav1created = favoriteDao.add(fav1);
		favoritesToDelete.add(fav1created);
		assertEquals(fav1, fav1created);
		assertEquals(initialCount + 1, favoriteDao.getCount());

		Favorite fav2created = favoriteDao.add(fav2);
		favoritesToDelete.add(fav2created);
		assertEquals(fav2, fav2created);
		assertEquals(initialCount + 2, favoriteDao.getCount());		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testAddWithNullPrincipal() throws Exception{
		Node node1 = NodeTestUtils.createNew("n1", creatorUserGroupId, creatorUserGroupId);
		String node1Id = nodeDao.createNew(node1);		
		nodesToDelete.add(node1Id);
		
		Favorite fav1 = new Favorite();
		fav1.setPrincipalId(null);
		fav1.setEntityId(node1Id);
		
		favoriteDao.add(fav1);
		fail();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testAddWithNullEntity() throws Exception{		
		Favorite fav1 = new Favorite();
		fav1.setPrincipalId(String.valueOf(creatorUserGroupId));
		fav1.setEntityId(null);
		
		favoriteDao.add(fav1);
		fail();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testAddNodeFKConstraint() throws Exception {
		Favorite fav = new Favorite();
		fav.setPrincipalId(String.valueOf(creatorUserGroupId));
		fav.setEntityId("syn"+String.valueOf(0));
		favoriteDao.add(fav);
		fail();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testAddPrincipalFKConstraint() throws Exception {
		Node node1 = NodeTestUtils.createNew("n1", creatorUserGroupId, creatorUserGroupId);
		String node1Id = nodeDao.createNew(node1);		
		nodesToDelete.add(node1Id);

		Favorite fav = new Favorite();
		fav.setPrincipalId(String.valueOf(-1));
		fav.setEntityId(node1Id);
		favoriteDao.add(fav);
		fail();
	}
	
	@Test
	public void testRemove() throws Exception {
		Node node1 = NodeTestUtils.createNew("n1", creatorUserGroupId, creatorUserGroupId);
		String node1Id = nodeDao.createNew(node1);		
		nodesToDelete.add(node1Id);
		Favorite fav1 = createFavorite(node1Id);
		
		Favorite fav1created = favoriteDao.add(fav1);
		favoritesToDelete.add(fav1created);
		assertEquals(initialCount+1, favoriteDao.getCount());

		// remove favorite
		favoriteDao.remove(fav1created.getPrincipalId(), fav1created.getEntityId());
		assertEquals(initialCount, favoriteDao.getCount());
	}
	
	@Test
	public void testRemoveNoOp() throws Exception {
		Favorite fav1 = new Favorite();		
		fav1.setPrincipalId(String.valueOf(creatorUserGroupId));
		fav1.setEntityId("syn"+Long.MAX_VALUE);
		assertEquals(initialCount, favoriteDao.getCount());
		// remove favorite
		favoriteDao.remove(fav1.getPrincipalId(), fav1.getEntityId());
		assertEquals(initialCount, favoriteDao.getCount());
	}

	@Test
	public void testGetFavorites() throws Exception {
		// make two nodes & two favorites  
		Node node1 = NodeTestUtils.createNew("n1", creatorUserGroupId, creatorUserGroupId);
		String node1Id = nodeDao.createNew(node1);		
		nodesToDelete.add(node1Id);
		Node node2 = NodeTestUtils.createNew("n2", creatorUserGroupId, creatorUserGroupId);
		String node2Id = nodeDao.createNew(node2);
		nodesToDelete.add(node2Id);		
		Favorite fav1 = createFavorite(node1Id);
		Favorite fav2 = createFavorite(node2Id);		
		Favorite fav1created = favoriteDao.add(fav1);
		favoritesToDelete.add(fav1created);
		Favorite fav2created = favoriteDao.add(fav2);
		favoritesToDelete.add(fav2created);
		
		PaginatedResults<Favorite> favs = favoriteDao.getFavorites(creatorUserGroupId.toString(), Integer.MAX_VALUE, 0);

		assertEquals(2, favs.getTotalNumberOfResults());		
		assertEquals(2, favs.getResults().size());
		assertTrue(favs.getResults().contains(fav1created));
		assertTrue(favs.getResults().contains(fav2created));
	}

	@Test
	public void testGetIndividualFavorite() throws Exception {
		Node node1 = NodeTestUtils.createNew("n1", creatorUserGroupId, creatorUserGroupId);
		String node1Id = nodeDao.createNew(node1);		
		nodesToDelete.add(node1Id);
		Favorite fav1 = createFavorite(node1Id);
		Favorite fav1created = favoriteDao.add(fav1);
		favoritesToDelete.add(fav1created);
		
		Favorite fav1get = favoriteDao.getIndividualFavorite(fav1.getPrincipalId(), fav1.getEntityId());
		
		assertEquals(fav1created, fav1get);
	}

	@Test(expected=NotFoundException.class)
	public void testGetIndividualFavoriteNotFound() throws Exception {
		Favorite fav1 = createFavorite("syn"+Long.MAX_VALUE);		
		Favorite fav1get = favoriteDao.getIndividualFavorite(fav1.getPrincipalId(), fav1.getEntityId());		
		fail();
	}

	
	/*
	 * Private Methods
	 */
	private Favorite createFavorite(String node1Id) {
		Favorite fav1 = new Favorite();
		fav1.setPrincipalId(String.valueOf(creatorUserGroupId));
		fav1.setEntityId(node1Id);
		return fav1;
	}
}

