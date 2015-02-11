package org.sagebionetworks.repo.manager;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;

import static org.sagebionetworks.repo.manager.EntityTypeConvertionError.*;

/**
 * Unit test for as much as possible.
 * @author John
 *
 */
public class EntityTypeConverterImplUnitTest {

	NodeDAO mockNodeDao;	
	AuthorizationManager mockAuthorizationManager;
	EntityManager mockEntityManager;
	EntityTypeConverterImpl typeConverter;
	Entity entity;
	UserInfo nonAdmin;
	UserInfo admin;
	
	@Before
	public void before() throws DatastoreException, NotFoundException{
		mockNodeDao = Mockito.mock(NodeDAO.class);
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		mockEntityManager = Mockito.mock(EntityManager.class);
		typeConverter = new EntityTypeConverterImpl(mockNodeDao, mockAuthorizationManager, mockEntityManager);
		entity = new Study();
		entity.setId("syn123");
		nonAdmin = new UserInfo(false);
		admin = new UserInfo(true);
		when(mockAuthorizationManager.canAccess(nonAdmin, entity.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(new AuthorizationStatus(true, null));
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUnauthorized() throws DatastoreException, NotFoundException{
		when(mockAuthorizationManager.canAccess(nonAdmin, entity.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(new AuthorizationStatus(false, "because I said so"));
		// should fail
		typeConverter.convertOldTypeToNew(nonAdmin, entity);
		verify(mockNodeDao, never()).lockNodeAndIncrementEtag(anyString(), anyString(), any(ChangeType.class));
	}
	
	@Test
	public void testAuthorized() throws DatastoreException, NotFoundException{
		when(mockAuthorizationManager.canAccess(nonAdmin, entity.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(new AuthorizationStatus(true, null));
		// should fail
		typeConverter.convertOldTypeToNew(nonAdmin, entity);
		// entity must be locked, etag checked and a change message setup.
		verify(mockNodeDao).lockNodeAndIncrementEtag(anyString(), anyString(), any(ChangeType.class));
	}
	
	/**
	 * Only locationable entities can be converted.
	 */
	@Test
	public void testNotLocationable() throws UnauthorizedException, DatastoreException, NotFoundException{
		Entity[] notLocationable = new Entity[]{
				new Project(),
				new TableEntity(),
				new Folder(),
				new FileEntity(),
		};
		for(Entity entity: notLocationable){
			try {
				typeConverter.convertOldTypeToNew(nonAdmin, entity);
			} catch (IllegalArgumentException e) {
				assertEquals(NOT_LOCATIONABLE.name(), e.getMessage());
			}
		}
	}
	
	/**
	 * Studies can have children because they will be converted to folders.
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Test
	public void testStudyHasChildren() throws DatastoreException, NotFoundException{
		// A study can have children.
		when(mockNodeDao.getChildrenIds(entity.getId())).thenReturn(new HashSet<String>(Arrays.asList("456","789")));
		when(mockEntityManager.getEntity(nonAdmin, entity.getId(), Folder.class)).thenReturn(new Folder());
		Entity result = typeConverter.convertOldTypeToNew(nonAdmin, entity);
		assertNotNull(result);
		assertTrue("Study should have been converted to a folder",result instanceof Folder);
	}
	
	@Test
	public void testDataHasChildren() throws DatastoreException, NotFoundException{
		entity = new Data();
		entity.setId("syn123");
		// Only studies are allowed to have children.
		when(mockNodeDao.getChildrenIds(entity.getId())).thenReturn(new HashSet<String>(Arrays.asList("456","789")));
		try {
			typeConverter.convertOldTypeToNew(nonAdmin, entity);
		} catch (IllegalArgumentException e) {
			assertEquals(FILES_CANNOT_HAVE_CHILDREN.name(), e.getMessage());
		}
	}
	
	@Test
	public void testDataNoChildren() throws DatastoreException, NotFoundException{
		entity = new Data();
		entity.setId("syn123");
		// no children this time.
		when(mockNodeDao.getChildrenIds(entity.getId())).thenReturn(new HashSet<String>(0));
		when(mockEntityManager.getEntity(nonAdmin, entity.getId(), FileEntity.class)).thenReturn(new FileEntity());
		Entity file = typeConverter.convertOldTypeToNew(nonAdmin, entity);
		assertNotNull(file);
		assertTrue("Data should have been converted to a file.",file instanceof FileEntity);
	}

}
