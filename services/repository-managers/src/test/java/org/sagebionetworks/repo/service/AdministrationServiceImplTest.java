package org.sagebionetworks.repo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.message.MessageSyndication;
import org.sagebionetworks.repo.manager.password.InvalidPasswordException;
import org.sagebionetworks.repo.manager.password.PasswordValidator;
import org.sagebionetworks.repo.manager.stack.StackStatusManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.admin.ExpireQuarantinedEmailRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.ses.EmailQuarantineDao;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.migration.IdGeneratorExport;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Unit test for AdministrationServiceImpl
 * 
 * @author John
 *
 */
@ExtendWith(MockitoExtension.class)
public class AdministrationServiceImplTest {

	@Mock
	private AuthenticationManager mockAuthManager;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private StackStatusManager mockStackStatusManager;
	@Mock
	private MessageSyndication mockMessageSyndication;
	@Mock
	private DBOChangeDAO mockChangeDAO;
	@Mock
	private IdGenerator mockIdGenerator;
	@Mock
	private PasswordValidator mockPasswordValidator;
	@Mock
	private EmailQuarantineDao mockEmailQuarantineDao;

	@InjectMocks
	private AdministrationServiceImpl adminService;
	
	private Long nonAdminUserId = 98345L;
	private UserInfo nonAdmin;
	private Long adminUserId = 842059834L;
	private UserInfo admin;
	private String exportScript;
	
	@BeforeEach
	public void before() throws DatastoreException, NotFoundException{
		// Setup the users
		nonAdmin = new UserInfo(false);
		admin = new UserInfo(true);
	}
	
	@Test
	public void testListChangeMessagesUnauthorized() throws DatastoreException, NotFoundException{
		when(mockUserManager.getUserInfo(nonAdminUserId)).thenReturn(nonAdmin);
		assertThrows(UnauthorizedException.class, () -> {			
			adminService.listChangeMessages(nonAdminUserId, 0l, ObjectType.ACTIVITY, Long.MAX_VALUE);
		});
	}

	@Test 
	public void testListChangeMessagesAuthorized() throws DatastoreException, NotFoundException{
		when(mockUserManager.getUserInfo(adminUserId)).thenReturn(admin);
		adminService.listChangeMessages(adminUserId, 0l, ObjectType.ACTIVITY, Long.MAX_VALUE);
	}
	
	@Test
	public void testRebroadcastChangeMessagesToQueueUnauthorized() throws DatastoreException, NotFoundException{
		when(mockUserManager.getUserInfo(nonAdminUserId)).thenReturn(nonAdmin);
		assertThrows(UnauthorizedException.class, () -> {
			adminService.rebroadcastChangeMessagesToQueue(nonAdminUserId, "queuename", 0l, ObjectType.ACTIVITY, Long.MAX_VALUE);
		});
	}

	@Test 
	public void testRebroadcastChangeMessagesToQueueAuthorized() throws DatastoreException, NotFoundException{
		when(mockUserManager.getUserInfo(adminUserId)).thenReturn(admin);
		adminService.rebroadcastChangeMessagesToQueue(adminUserId, "queuename", 0l, ObjectType.ACTIVITY, Long.MAX_VALUE);
	}
	
	@Test
	public void testReFireChangeMessagesToQueueUnauthorized() throws DatastoreException, NotFoundException{
		when(mockUserManager.getUserInfo(nonAdminUserId)).thenReturn(nonAdmin);
		assertThrows(UnauthorizedException.class, () -> {
			adminService.reFireChangeMessages(nonAdminUserId, 0L, Long.MAX_VALUE);
		});
	}

	@Test 
	public void testReFireChangeMessagesToQueueAuthorized() throws DatastoreException, NotFoundException{
		when(mockUserManager.getUserInfo(adminUserId)).thenReturn(admin);
		adminService.reFireChangeMessages(adminUserId, 0L, Long.MAX_VALUE);
	}
	
	@Test
	public void testGetCurrentChangeNumberUnauthorized() throws DatastoreException, NotFoundException{
		when(mockUserManager.getUserInfo(nonAdminUserId)).thenReturn(nonAdmin);
		assertThrows(UnauthorizedException.class, () -> {
			adminService.getCurrentChangeNumber(nonAdminUserId);
		});
	}

	@Test 
	public void testGetCurrentChangeNumberAuthorized() throws DatastoreException, NotFoundException{
		when(mockUserManager.getUserInfo(adminUserId)).thenReturn(admin);
		adminService.getCurrentChangeNumber(adminUserId);
	}

	@Test
	public void testCreateOrUpdateChangeMessagesUnauthorized() throws UnauthorizedException, NotFoundException {
		when(mockUserManager.getUserInfo(nonAdminUserId)).thenReturn(nonAdmin);
		assertThrows(UnauthorizedException.class, () -> {
			adminService.createOrUpdateChangeMessages(nonAdminUserId, new ChangeMessages());
		});
	}

	@Test
	public void testCreateOrUpdateChangeMessagesAuthorized() throws UnauthorizedException, NotFoundException {
		when(mockUserManager.getUserInfo(adminUserId)).thenReturn(admin);
		ChangeMessages batch =  new ChangeMessages();
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.UPDATE);
		message.setObjectId("12345");
		message.setObjectType(ObjectType.ENTITY);
		batch.setList(Arrays.asList(message));
		when(mockChangeDAO.replaceChange(batch.getList())).thenReturn(batch.getList());
		adminService.createOrUpdateChangeMessages(adminUserId, batch);
	}
	
	@Test
	public void testCreateIdGeneratorExport() {
		exportScript = "exportScript";
		when(mockUserManager.getUserInfo(adminUserId)).thenReturn(admin);
		when(mockIdGenerator.createRestoreScript()).thenReturn(exportScript);
		// call under test
		IdGeneratorExport export = this.adminService.createIdGeneratorExport(adminUserId);
		assertNotNull(export);
		assertEquals(exportScript, export.getExportScript());
	}
	
	
	@Test
	public void testCreateIdGeneratorExportNonAdmin() {
		when(mockUserManager.getUserInfo(nonAdminUserId)).thenReturn(nonAdmin);
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			this.adminService.createIdGeneratorExport(nonAdminUserId);
		});
	}

	@Test
	public void testCreateOrGetTestUser_bannedPassword(){
		when(mockUserManager.getUserInfo(adminUserId)).thenReturn(admin);
		String bannedPassword = "hunter2";
		doThrow(InvalidPasswordException.class).when(mockPasswordValidator).validatePassword(bannedPassword);
		NewIntegrationTestUser testUser = new NewIntegrationTestUser();
		testUser.setPassword(bannedPassword);
		
		assertThrows(InvalidPasswordException.class, () -> {
			adminService.createOrGetTestUser(adminUserId, testUser);
		});
	}
	
	@Test
	public void testGetUserAccessToken() {
		LoginResponse expected = new LoginResponse().setAccessToken("token");
		
		when(mockUserManager.getUserInfo(any())).thenReturn(admin);
		when(mockAuthManager.loginWithNoPasswordCheck(anyLong(), any())).thenReturn(expected);
		
		// Call under test
		LoginResponse result = adminService.getUserAccessToken(adminUserId, nonAdminUserId);
		
		assertEquals(expected, result);
		
		verify(mockUserManager).getUserInfo(adminUserId);
		verify(mockAuthManager).loginWithNoPasswordCheck(nonAdminUserId, null);
	}
	
	@Test
	public void testGetUserAccessTokenUnauthorized() {
		
		when(mockUserManager.getUserInfo(any())).thenReturn(nonAdmin);
		
		assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			adminService.getUserAccessToken(adminUserId, nonAdminUserId);
		});
		
		verify(mockUserManager).getUserInfo(adminUserId);
	}
	
	@Test
	public void testExpireQuarantinedEmail() {
		
		when(mockUserManager.getUserInfo(any())).thenReturn(admin);
		
		// Call under test
		adminService.expireQuarantinedEmail(adminUserId, new ExpireQuarantinedEmailRequest().setEmail("email@sagebase.org"));
		
		verify(mockEmailQuarantineDao).expireQuarantinedEmail("email@sagebase.org");
	}
	
	@Test
	public void testExpireQuarantinedEmailWithUnatuhorized() {
		
		when(mockUserManager.getUserInfo(any())).thenReturn(nonAdmin);
		
		assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			adminService.expireQuarantinedEmail(nonAdminUserId, new ExpireQuarantinedEmailRequest().setEmail("email@sagebase.org"));
		});
		
		verifyZeroInteractions(mockEmailQuarantineDao);
	}
	
	@Test
	public void testExpireQuarantinedEmailWithNoRequest() {
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			adminService.expireQuarantinedEmail(nonAdminUserId, null);
		}).getMessage();
		
		assertEquals("The request is required.", result);
		
		verifyZeroInteractions(mockEmailQuarantineDao);
	}
	
	@Test
	public void testExpireQuarantinedEmailWithNoEmail() {
				
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			adminService.expireQuarantinedEmail(nonAdminUserId, new ExpireQuarantinedEmailRequest());
		}).getMessage();
		
		assertEquals("The request.email is required and must not be the empty string.", result);
		
		verifyZeroInteractions(mockEmailQuarantineDao);
	}
}
