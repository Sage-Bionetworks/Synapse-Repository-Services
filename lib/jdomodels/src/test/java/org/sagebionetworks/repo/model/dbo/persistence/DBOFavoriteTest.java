package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOFavoriteTest {
	
	@Autowired
	private DBOBasicDao dboBasicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private List<DBOFavorite> favoritesToDelete = null;
	private List<Long> nodeIdsToDelete = null;
	
	@AfterEach
	public void after() throws DatastoreException {
		if(dboBasicDao != null && favoritesToDelete != null){
			for(DBOFavorite fav : favoritesToDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue(DBOFavorite.FIELD_COLUMN_ID_PRINCIPAL_ID, fav.getPrincipalId());
				params.addValue(DBOFavorite.FIELD_COLUMN_ID_NODE_ID, fav.getNodeId());
				try {
				dboBasicDao.deleteObjectByPrimaryKey(DBOFavorite.class, params);
				} catch (DatastoreException e) {
					// next
				}
			}
		}
		if(dboBasicDao != null && nodeIdsToDelete != null){
			for(Long id: nodeIdsToDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				try {
				dboBasicDao.deleteObjectByPrimaryKey(DBONode.class, params);
				} catch (DatastoreException e) {
					// next
				}
			}
		}
	}
	
	@BeforeEach
	public void before(){
		favoritesToDelete = new LinkedList<DBOFavorite>();
		nodeIdsToDelete = new LinkedList<Long>();
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException, NotFoundException, UnsupportedEncodingException{
		DBONode node = createNode();
		
		DBOFavorite favorite = new DBOFavorite();
		favorite.setNodeId(node.getId());
		Long createdById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		favorite.setPrincipalId(createdById);
		favorite.setCreatedOn(System.currentTimeMillis());
		favorite.setId(idGenerator.generateNewId(IdType.FAVORITE_ID));
		// Make sure we can create it
		DBOFavorite clone = dboBasicDao.createNew(favorite);
		assertNotNull(clone);
		favoritesToDelete.add(favorite);
		assertEquals(favorite, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(DBOFavorite.FIELD_COLUMN_ID_PRINCIPAL_ID, favorite.getPrincipalId());
		params.addValue(DBOFavorite.FIELD_COLUMN_ID_NODE_ID, favorite.getNodeId());
		clone = dboBasicDao.getObjectByPrimaryKey(DBOFavorite.class, params).get();
		assertNotNull(clone);
		assertEquals(favorite, clone);
		
		// Make sure we can update it.
		favorite.setCreatedOn(System.currentTimeMillis() + 100000);
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		
		// Get the clone back again
		params = new MapSqlParameterSource();
		params.addValue(DBOFavorite.FIELD_COLUMN_ID_PRINCIPAL_ID, clone.getPrincipalId());
		params.addValue(DBOFavorite.FIELD_COLUMN_ID_NODE_ID, clone.getNodeId());
		DBOFavorite clone2 = dboBasicDao.getObjectByPrimaryKey(DBOFavorite.class, params).get();
		assertEquals(clone, clone2);
	}

	private DBONode createNode() {
		DBONode node = new DBONode();
		node.setId(idGenerator.generateNewId(IdType.ENTITY_ID));
		node.setName("SomeName");
		Long createdById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		node.setCreatedBy(createdById);
		node.setCreatedOn(System.currentTimeMillis());
		node.setCurrentRevNumber(NodeConstants.DEFAULT_VERSION_NUMBER);
		node.setMaxRevNumber(NodeConstants.DEFAULT_VERSION_NUMBER);
		node.seteTag("1");
		node.setType(EntityType.project.name());
		// Make sure we can create it
		DBONode clone = dboBasicDao.createNew(node);
		assertNotNull(clone);
		nodeIdsToDelete.add(node.getId());
		return node;
	}

}
