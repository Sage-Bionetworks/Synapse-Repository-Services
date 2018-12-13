package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.StackStatusManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.message.MessageSyndication;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.migration.IdGeneratorExport;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test for AdministrationServiceImpl
 * 
 * @author John
 *
 */
public class AdministrationServiceImplTest {

	@Mock
	ObjectTypeSerializer mockObjectTypeSerializer;
	@Mock
	UserManager mockUserManager;
	@Mock
	StackStatusManager mockStackStatusManager;
	@Mock
	MessageSyndication mockMessageSyndication;
	@Mock
	DBOChangeDAO mockChangeDAO;
	@Mock
	IdGenerator mockIdGenerator;
	
	AdministrationServiceImpl adminService;
	
	Long nonAdminUserId = 98345L;
	UserInfo nonAdmin;
	Long adminUserId = 842059834L;
	UserInfo admin;
	String exportScript;
	
	@Before
	public void before() throws DatastoreException, NotFoundException{
		MockitoAnnotations.initMocks(this);
		adminService = new AdministrationServiceImpl();
		ReflectionTestUtils.setField(adminService, "objectTypeSerializer", mockObjectTypeSerializer);
		ReflectionTestUtils.setField(adminService, "userManager", mockUserManager);
		ReflectionTestUtils.setField(adminService, "stackStatusManager", mockStackStatusManager);
		ReflectionTestUtils.setField(adminService, "messageSyndication", mockMessageSyndication);
		ReflectionTestUtils.setField(adminService, "changeDAO", mockChangeDAO);
		ReflectionTestUtils.setField(adminService, "idGenerator", mockIdGenerator);
		// Setup the users
		nonAdmin = new UserInfo(false);
		admin = new UserInfo(true);
		when(mockUserManager.getUserInfo(nonAdminUserId)).thenReturn(nonAdmin);
		when(mockUserManager.getUserInfo(adminUserId)).thenReturn(admin);
		exportScript = "the export script";
		when(mockIdGenerator.createRestoreScript()).thenReturn(exportScript);
	}	
	
	@Test (expected=UnauthorizedException.class)
	public void testListChangeMessagesUnauthorized() throws DatastoreException, NotFoundException{
		adminService.listChangeMessages(nonAdminUserId, 0l, ObjectType.ACTIVITY, Long.MAX_VALUE);
	}

	@Test 
	public void testListChangeMessagesAuthorized() throws DatastoreException, NotFoundException{
		adminService.listChangeMessages(adminUserId, 0l, ObjectType.ACTIVITY, Long.MAX_VALUE);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testRebroadcastChangeMessagesToQueueUnauthorized() throws DatastoreException, NotFoundException{
		adminService.rebroadcastChangeMessagesToQueue(nonAdminUserId, "queuename", 0l, ObjectType.ACTIVITY, Long.MAX_VALUE);
	}

	@Test 
	public void testRebroadcastChangeMessagesToQueueAuthorized() throws DatastoreException, NotFoundException{
		adminService.rebroadcastChangeMessagesToQueue(adminUserId, "queuename", 0l, ObjectType.ACTIVITY, Long.MAX_VALUE);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testReFireChangeMessagesToQueueUnauthorized() throws DatastoreException, NotFoundException{
		adminService.reFireChangeMessages(nonAdminUserId, 0L, Long.MAX_VALUE);
	}

	@Test 
	public void testReFireChangeMessagesToQueueAuthorized() throws DatastoreException, NotFoundException{
		adminService.reFireChangeMessages(adminUserId, 0L, Long.MAX_VALUE);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetCurrentChangeNumberUnauthorized() throws DatastoreException, NotFoundException{
		adminService.getCurrentChangeNumber(nonAdminUserId);
	}

	@Test 
	public void testGetCurrentChangeNumberAuthorized() throws DatastoreException, NotFoundException{
		adminService.getCurrentChangeNumber(adminUserId);
	}

	@Test (expected=UnauthorizedException.class)
	public void testCreateOrUpdateChangeMessagesUnauthorized() throws UnauthorizedException, NotFoundException {
		adminService.createOrUpdateChangeMessages(nonAdminUserId, new ChangeMessages());
	}

	@Test
	public void testCreateOrUpdateChangeMessagesAuthorized() throws UnauthorizedException, NotFoundException {
		ChangeMessages batch =  new ChangeMessages();
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.UPDATE);
		message.setObjectEtag("etag");
		message.setObjectId("12345");
		message.setObjectType(ObjectType.ENTITY);
		batch.setList(Arrays.asList(message));
		when(mockChangeDAO.replaceChange(batch.getList())).thenReturn(batch.getList());
		adminService.createOrUpdateChangeMessages(adminUserId, batch);
	}
	
	@Test
	public void testCreateIdGeneratorExport() {
		// call under test
		IdGeneratorExport export = this.adminService.createIdGeneratorExport(adminUserId);
		assertNotNull(export);
		assertEquals(exportScript, export.getExportScript());
	}
	
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateIdGeneratorExportNonAdmin() {
		// call under test
		this.adminService.createIdGeneratorExport(nonAdminUserId);
	}
}
