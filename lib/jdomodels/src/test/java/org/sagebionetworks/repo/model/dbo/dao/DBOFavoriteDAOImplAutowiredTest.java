package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
	private FavoriteDAO favoriteDao;
	
	@Autowired
	private NodeDAO nodeDao;
	
	// must be deleted at the end of each test.
	private List<Favorite> favoritesToDelete;
	private List<String> nodesToDelete;
	
	private Long creatorUserGroupId = null;	
	
	long initialCount; 

	@Before
	public void before() throws Exception {
		favoritesToDelete = new ArrayList<Favorite>();
		nodesToDelete = new ArrayList<String>();
		
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
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
		assertEquals(fav1.getPrincipalId(), fav1created.getPrincipalId());
		assertEquals(fav1.getEntityId(), fav1created.getEntityId());
		assertEquals(initialCount + 1, favoriteDao.getCount());
		assertTrue(fav1created.getCreatedOn().getTime() > 0);

		Favorite fav2created = favoriteDao.add(fav2);
		favoritesToDelete.add(fav2created);
		assertEquals(fav2.getPrincipalId(), fav2created.getPrincipalId());
		assertEquals(fav2.getEntityId(), fav2created.getEntityId());
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
		Node parent = NodeTestUtils.createNew("parent", creatorUserGroupId, creatorUserGroupId);
		String parentId = nodeDao.createNew(parent);
		nodesToDelete.add(parentId);
		// make two nodes & two favorites  
		Node node1 = NodeTestUtils.createNew("n1", creatorUserGroupId, creatorUserGroupId);
		node1.setParentId(parentId);
		String node1Id = nodeDao.createNew(node1);		
		nodesToDelete.add(node1Id);
		Node node2 = NodeTestUtils.createNew("n2", creatorUserGroupId, creatorUserGroupId);
		node2.setParentId(parentId);
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
	public void testGetFavoritesEntityHeader() throws Exception {
		
		Node parent = NodeTestUtils.createNew("parent", creatorUserGroupId, creatorUserGroupId);
		String parentId = nodeDao.createNew(parent);
		nodesToDelete.add(parentId);

		// make two nodes & two favorites
		EntityType node1Type = EntityType.project;
		EntityType node2Type = EntityType.file;
		String node1Name = "node1";
		String node2Name = "node2";
		
		Node node1 = new Node();
		node1.setName(node1Name);
		node1.setCreatedByPrincipalId(creatorUserGroupId);
		node1.setModifiedByPrincipalId(creatorUserGroupId);
		node1.setCreatedOn(new Date(System.currentTimeMillis()));
		node1.setModifiedOn(node1.getCreatedOn());
		node1.setNodeType(node1Type);
		node1.setParentId(parentId);
		String node1Id = nodeDao.createNew(node1);		
		nodesToDelete.add(node1Id);

		Node node2 = new Node();
		node2.setName(node2Name);
		node2.setCreatedByPrincipalId(creatorUserGroupId);
		node2.setModifiedByPrincipalId(creatorUserGroupId);
		node2.setCreatedOn(new Date(System.currentTimeMillis()));
		node2.setModifiedOn(node2.getCreatedOn());
		node2.setNodeType(node2Type);
		node2.setParentId(parentId);
		String node2Id = nodeDao.createNew(node2);		
		nodesToDelete.add(node2Id);

		Favorite fav1 = createFavorite(node1Id);
		Favorite fav2 = createFavorite(node2Id);		
		Favorite fav1created = favoriteDao.add(fav1);
		favoritesToDelete.add(fav1created);
		Favorite fav2created = favoriteDao.add(fav2);
		favoritesToDelete.add(fav2created);

		PaginatedResults<EntityHeader> favs = favoriteDao.getFavoritesEntityHeader(creatorUserGroupId.toString(), Integer.MAX_VALUE, 0);

		assertEquals(2, favs.getTotalNumberOfResults());		
		assertEquals(2, favs.getResults().size());
		
		EntityHeader eh1 = favs.getResults().get(0);
		EntityHeader eh2 = favs.getResults().get(1);

		assertNotNull(eh1);
		assertNotNull(eh2);
		assertEquals(node1Id, eh1.getId());
		assertEquals(Project.class.getName(), eh1.getType());
		assertEquals(new Long(1), eh1.getVersionNumber());
		assertEquals("1", eh1.getVersionLabel());
		assertTrue(eh1.getIsLatestVersion());
		assertTrue(eh2.getIsLatestVersion());

		// Test limit and offset (PLFM-6616)
		int limit = 1;
		int offset = 0;
		favs = favoriteDao.getFavoritesEntityHeader(creatorUserGroupId.toString(), limit, offset);
		assertEquals(1, favs.getResults().size());
		assertEquals(node1Id, favs.getResults().get(0).getId());

		offset = 1;
		favs = favoriteDao.getFavoritesEntityHeader(creatorUserGroupId.toString(), limit, offset);
		assertEquals(1, favs.getResults().size());
		assertEquals(node2Id, favs.getResults().get(0).getId());
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

