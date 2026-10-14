package org.sagebionetworks.repo.manager.limits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.limits.ProjectStorageLimitsDao;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.limits.ProjectStorageData;
import org.sagebionetworks.repo.model.limits.ProjectStorageEvent;
import org.sagebionetworks.repo.model.limits.ProjectStorageLocationLimit;
import org.sagebionetworks.repo.model.limits.ProjectStorageLocationUsage;
import org.sagebionetworks.repo.model.limits.ProjectStorageUsage;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ProjectStorageLimitExceededException;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.Clock;

@ExtendWith(MockitoExtension.class)
public class ProjectStorageLimitsManagerTest {

	@Mock
	private ProjectStorageLimitsDao mockDao;
	
	@Mock
	private StackConfiguration mockConfig;
	
	@Mock
	private EntityAuthorizationManager mockAuthzManager;
	
	@Mock
	private TableIndexDAO mockReplicationDao;
		
	@Mock
	private NodeDAO mockNodeDao;
	
	@Mock
	private StorageLocationDAO mockStorageLocationDao;
	
	@Mock
	private TransactionalMessenger mockMessenger;
	
	@Mock
	private Clock mockClock;
	
	@Mock
	private FeatureManager mockFeatureManager;
	
	@InjectMocks
	@Spy
	private ProjectStorageLimitsManager manager;

	private UserInfo planManagerUser;
	
	@BeforeEach
	public void before() {
		planManagerUser = new UserInfo(false, 123L, AuthorizationConstants.DEFAULT_REALM_ID, new HashSet<>());
		planManagerUser.getGroups().add(BOOTSTRAP_PRINCIPAL.PLAN_MANAGERS.getPrincipalId());
	}
	
	@Test
	public void testRefreshProjectStorageData() {
		Long projectId = 123L;
		
		Date now = Date.from(Instant.now());
		
		ProjectStorageData data = new ProjectStorageData().setProjectId(projectId);
		
		when(mockClock.now()).thenReturn(now);
		when(mockDao.isStorageDataModifiedOnAfter(projectId, now.toInstant().minus(ProjectStorageLimitsManager.CACHE_UPDATE_FREQUENCY))).thenReturn(false);
		when(mockReplicationDao.computeProjectStorageData(projectId)).thenReturn(data);
		
		// Call under test
		manager.refreshProjectStorageData(projectId);
		
		verify(mockDao).setStorageData(List.of(data));
	}
	
	@Test
	public void testRefreshProjectStorageDataWithUptodate() {
		Long projectId = 123L;
		
		Date now = Date.from(Instant.now());
		
		when(mockClock.now()).thenReturn(now);
		when(mockDao.isStorageDataModifiedOnAfter(projectId, now.toInstant().minus(ProjectStorageLimitsManager.CACHE_UPDATE_FREQUENCY))).thenReturn(true);
		
		// Call under test
		manager.refreshProjectStorageData(projectId);
		
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testGetProjectStorageUsageWithNoStorageData() {
		String projectId = "syn123";
		Long projectIdLong = 123L;
		
		doReturn(KeyFactory.stringToKey(projectId)).when(manager).validateAndGetProjectId(projectId);
		
		when(mockDao.getStorageLocationLimits(projectIdLong)).thenReturn(List.of(
			new ProjectStorageLocationLimit().setStorageLocationId(1L).setMaxAllowedFileBytes(1024L),
			new ProjectStorageLocationLimit().setStorageLocationId(2L).setMaxAllowedFileBytes(2048L)
		));
		
		when(mockDao.getStorageData(projectIdLong)).thenReturn(Optional.empty());
		
		ProjectStorageUsage expected = new ProjectStorageUsage()
			.setProjectId(projectId)
			.setLocations(List.of(
				new ProjectStorageLocationUsage().setStorageLocationId(1L).setMaxAllowedFileBytes(1024L).setIsOverLimit(false).setSumFileBytes(0L),
				new ProjectStorageLocationUsage().setStorageLocationId(2L).setMaxAllowedFileBytes(2048L).setIsOverLimit(false).setSumFileBytes(0L)				
			));
		
		// Call under test
		assertEquals(expected, manager.getProjectStorageUsage(planManagerUser, projectId));
		
		// Emulates the call to the notification timer, this clears the internal cache
		manager.sendProjectStorageNotifications();
		
		verify(mockMessenger).publishMessageAfterCommit(new ProjectStorageEvent()
			.setObjectType(ObjectType.PROJECT_STORAGE_EVENT)
			.setObjectId(projectIdLong.toString())
			.setProjectId(projectIdLong)
		);
		
		verifyNoMoreInteractions(mockDao, mockNodeDao, mockReplicationDao, mockClock, mockMessenger);
	}
	
	@Test
	public void testGetProjectStorageUsageWithStorageData() {
		String projectId = "syn123";
		Long projectIdLong = 123L;
		
		doReturn(KeyFactory.stringToKey(projectId)).when(manager).validateAndGetProjectId(projectId);
		
		when(mockDao.getStorageLocationLimits(projectIdLong)).thenReturn(List.of(
			new ProjectStorageLocationLimit().setStorageLocationId(1L).setMaxAllowedFileBytes(1024L),
			new ProjectStorageLocationLimit().setStorageLocationId(2L).setMaxAllowedFileBytes(2048L)
		));
		
		when(mockDao.getStorageData(projectIdLong)).thenReturn(Optional.of(new ProjectStorageData()
			.setStorageLocationData(Map.of("1", 512L, "2", 4096L, "3", 2024L))
		));
		
		ProjectStorageUsage expected = new ProjectStorageUsage()
			.setProjectId(projectId)
			.setLocations(List.of(
				new ProjectStorageLocationUsage().setStorageLocationId(1L).setMaxAllowedFileBytes(1024L).setIsOverLimit(false).setSumFileBytes(512L),
				new ProjectStorageLocationUsage().setStorageLocationId(2L).setMaxAllowedFileBytes(2048L).setIsOverLimit(true).setSumFileBytes(4096L)
			));
		
		// Call under test
		assertEquals(expected, manager.getProjectStorageUsage(planManagerUser, projectId));
		
		// Emulates the call to the notification timer, this clears the internal cache
		manager.sendProjectStorageNotifications();
		
		verify(mockMessenger).publishMessageAfterCommit(new ProjectStorageEvent()
			.setObjectType(ObjectType.PROJECT_STORAGE_EVENT)
			.setObjectId(projectIdLong.toString())
			.setProjectId(projectIdLong)
		);
		
		verifyNoMoreInteractions(mockDao, mockNodeDao, mockReplicationDao, mockClock, mockMessenger);
	}
	
	@Test
	public void testGetProjectStorageUsageWithUnlimitedLimit() {
		String projectId = "syn123";
		Long projectIdLong = 123L;
		
		doReturn(KeyFactory.stringToKey(projectId)).when(manager).validateAndGetProjectId(projectId);
		
		when(mockDao.getStorageLocationLimits(projectIdLong)).thenReturn(List.of(
			new ProjectStorageLocationLimit().setStorageLocationId(1L).setMaxAllowedFileBytes(null)
		));
		
		when(mockDao.getStorageData(projectIdLong)).thenReturn(Optional.of(new ProjectStorageData()
			.setStorageLocationData(Map.of("1", 512L))
		));
		
		ProjectStorageUsage expected = new ProjectStorageUsage()
			.setProjectId(projectId)
			.setLocations(List.of(
				new ProjectStorageLocationUsage().setStorageLocationId(1L).setMaxAllowedFileBytes(null).setIsOverLimit(false).setSumFileBytes(512L)
			));
		
		// Call under test
		assertEquals(expected, manager.getProjectStorageUsage(planManagerUser, projectId));
		
		// Emulates the call to the notification timer, this clears the internal cache
		manager.sendProjectStorageNotifications();
		
		verify(mockMessenger).publishMessageAfterCommit(new ProjectStorageEvent()
			.setObjectType(ObjectType.PROJECT_STORAGE_EVENT)
			.setObjectId(projectIdLong.toString())
			.setProjectId(projectIdLong)
		);
		
		verifyNoMoreInteractions(mockDao, mockNodeDao, mockReplicationDao, mockClock, mockMessenger);
	}
	
	@Test
	public void testGetProjectStorageUsageWithNotPlanManagerAndAuthorized() {
		planManagerUser.getGroups().clear();
		
		String projectId = "syn123";
		Long projectIdLong = 123L;
		
		doReturn(KeyFactory.stringToKey(projectId)).when(manager).validateAndGetProjectId(projectId);
		
		when(mockAuthzManager.hasAccess(planManagerUser, projectId, ACCESS_TYPE.CREATE)).thenReturn(AuthorizationStatus.authorized());
		
		when(mockDao.getStorageLocationLimits(projectIdLong)).thenReturn(List.of(
			new ProjectStorageLocationLimit().setStorageLocationId(1L).setMaxAllowedFileBytes(1024L),
			new ProjectStorageLocationLimit().setStorageLocationId(2L).setMaxAllowedFileBytes(2048L)
		));
		
		when(mockDao.getStorageData(projectIdLong)).thenReturn(Optional.empty());
		
		ProjectStorageUsage expected = new ProjectStorageUsage()
			.setProjectId(projectId)
			.setLocations(List.of(
				new ProjectStorageLocationUsage().setStorageLocationId(1L).setMaxAllowedFileBytes(1024L).setIsOverLimit(false).setSumFileBytes(0L),
				new ProjectStorageLocationUsage().setStorageLocationId(2L).setMaxAllowedFileBytes(2048L).setIsOverLimit(false).setSumFileBytes(0L)				
			));
		
		// Call under test
		assertEquals(expected, manager.getProjectStorageUsage(planManagerUser, projectId));
		
		// Emulates the call to the notification timer, this clears the internal cache
		manager.sendProjectStorageNotifications();
		
		verify(mockMessenger).publishMessageAfterCommit(new ProjectStorageEvent()
			.setObjectType(ObjectType.PROJECT_STORAGE_EVENT)
			.setObjectId(projectIdLong.toString())
			.setProjectId(projectIdLong)
		);
		
		verifyNoMoreInteractions(mockDao, mockNodeDao, mockReplicationDao, mockClock, mockMessenger);
	}
	
	@Test
	public void testGetProjectStorageUsageWithNotPlanManagerAndNotAuthorized() {
		planManagerUser.getGroups().clear();
		
		String projectId = "syn123";
		
		doReturn(KeyFactory.stringToKey(projectId)).when(manager).validateAndGetProjectId(projectId);
		
		when(mockAuthzManager.hasAccess(planManagerUser, projectId, ACCESS_TYPE.CREATE)).thenReturn(AuthorizationStatus.accessDenied("Nope"));
		
		assertEquals("Nope", assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			manager.getProjectStorageUsage(planManagerUser, projectId);
		}).getMessage());
		
		verifyNoMoreInteractions(mockDao, mockNodeDao, mockReplicationDao, mockClock, mockMessenger);
	}
	
	@Test
	public void testGetProjectStorageLocationUsage() {
		String projectId = "123";
		Long storageLocationId = 2L;
		
		doReturn(KeyFactory.stringToKey(projectId)).when(manager).validateAndGetProjectId(projectId);
		
		when(mockDao.getStorageLocationLimit(123L, storageLocationId)).thenReturn(Optional.of(new ProjectStorageLocationLimit()
			.setProjectId(projectId)
			.setStorageLocationId(storageLocationId)
			.setMaxAllowedFileBytes(100L)
		));
		
		when(mockDao.getStorageData(123L)).thenReturn(Optional.of(new ProjectStorageData()
			.setStorageLocationData(Map.of(storageLocationId.toString(), 50L))
		));
		
		assertEquals(Optional.of(new ProjectStorageLocationUsage()
				.setMaxAllowedFileBytes(100L)
				.setStorageLocationId(storageLocationId)
				.setSumFileBytes(50L)
				.setIsOverLimit(false)
			),
			// Call under test
			manager.getProjectStorageLocationUsage(projectId, storageLocationId)
		);
		
		manager.sendProjectStorageNotifications();
		
		verify(mockMessenger).publishMessageAfterCommit(new ProjectStorageEvent()
			.setObjectType(ObjectType.PROJECT_STORAGE_EVENT)
			.setObjectId(projectId)
			.setProjectId(123L)
		);
		
		verifyNoMoreInteractions(mockDao, mockNodeDao, mockReplicationDao, mockClock, mockMessenger);
	}
	
	@Test
	public void testGetProjectStorageLocationUsageWithNoLimit() {
		String projectId = "123";
		Long storageLocationId = 2L;
		
		doReturn(KeyFactory.stringToKey(projectId)).when(manager).validateAndGetProjectId(projectId);
		
		when(mockDao.getStorageLocationLimit(123L, storageLocationId)).thenReturn(Optional.empty());
				
		assertEquals(Optional.empty(),
			// Call under test
			manager.getProjectStorageLocationUsage(projectId, storageLocationId)
		);
		
		manager.sendProjectStorageNotifications();
		
		verify(mockMessenger).publishMessageAfterCommit(new ProjectStorageEvent()
			.setObjectType(ObjectType.PROJECT_STORAGE_EVENT)
			.setObjectId(projectId)
			.setProjectId(123L)
		);
		
		verifyNoMoreInteractions(mockDao, mockNodeDao, mockReplicationDao, mockClock, mockMessenger);
	}
	
	@Test
	public void testGetProjectStorageLocationUsageWithNoData() {
		String projectId = "123";
		Long storageLocationId = 2L;
		
		doReturn(KeyFactory.stringToKey(projectId)).when(manager).validateAndGetProjectId(projectId);
		
		when(mockDao.getStorageLocationLimit(123L, storageLocationId)).thenReturn(Optional.of(new ProjectStorageLocationLimit()
			.setProjectId(projectId)
			.setStorageLocationId(storageLocationId)
			.setMaxAllowedFileBytes(100L)
		));
		
		when(mockDao.getStorageData(123L)).thenReturn(Optional.empty());
		
		assertEquals(Optional.of(new ProjectStorageLocationUsage()
				.setMaxAllowedFileBytes(100L)
				.setStorageLocationId(storageLocationId)
				.setSumFileBytes(0L)
				.setIsOverLimit(false)
			),
			// Call under test
			manager.getProjectStorageLocationUsage(projectId, storageLocationId)
		);
		
		manager.sendProjectStorageNotifications();
		
		verify(mockMessenger).publishMessageAfterCommit(new ProjectStorageEvent()
			.setObjectType(ObjectType.PROJECT_STORAGE_EVENT)
			.setObjectId(projectId)
			.setProjectId(123L)
		);
		
		verifyNoMoreInteractions(mockDao, mockNodeDao, mockReplicationDao, mockClock, mockMessenger);
	}
	
	
	@Test
	public void testGetProjectStorageLocationUsageWithNoStorageLocationId() {
		String projectId = "123";
		Long storageLocationId = null;
		
		assertEquals("The storageLocationId is required.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.getProjectStorageLocationUsage(projectId, storageLocationId);
		}).getMessage());
				
		verifyNoMoreInteractions(mockDao, mockNodeDao, mockReplicationDao, mockClock, mockMessenger);
	}
		
	@Test
	public void testSendProjectStorageNotification() {
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.project);
		when(mockDao.getStorageLocationLimits(any())).thenReturn(Collections.emptyList());
		when(mockDao.getStorageData(any())).thenReturn(Optional.empty());
		
		// Call under test
		manager.sendProjectStorageNotifications();
		
		verifyNoMoreInteractions(mockMessenger);
		
		manager.getProjectStorageUsage(planManagerUser, "123");
		manager.getProjectStorageUsage(planManagerUser, "123");
		manager.getProjectStorageUsage(planManagerUser, "456");
		
		// Call under test
		manager.sendProjectStorageNotifications();
		
		verify(mockMessenger).publishMessageAfterCommit(new ProjectStorageEvent()
			.setObjectType(ObjectType.PROJECT_STORAGE_EVENT)
			.setObjectId("123")
			.setProjectId(123L)
		);
		
		verify(mockMessenger).publishMessageAfterCommit(new ProjectStorageEvent()
			.setObjectType(ObjectType.PROJECT_STORAGE_EVENT)
			.setObjectId("456")
			.setProjectId(456L)
		);
		
		verifyNoMoreInteractions(mockMessenger);
	}
	
	@Test
	public void testSetDefaultProjectStorageLimit() {
		String projectId = "123";
		Long storageLocationId = 2L;
		
		doNothing().when(manager).validateStorageLocationId(storageLocationId);
		doReturn(KeyFactory.stringToKey(projectId)).when(manager).validateAndGetProjectId(projectId);
		
		when(mockDao.getStorageLocationLimit(123L, 2L)).thenReturn(Optional.empty());
		
		// Call under test
		manager.setDefaultProjectStorageLimit(projectId, storageLocationId);
		
		verify(mockDao).setStorageLocationLimit(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), new ProjectStorageLocationLimit()
			.setProjectId(projectId)
			.setStorageLocationId(storageLocationId)
			.setMaxAllowedFileBytes(null)
		);
		
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testSetDefaultProjectStorageLimitWithAlreadyExists() {
		String projectId = "123";
		Long storageLocationId = 2L;
		
		doNothing().when(manager).validateStorageLocationId(storageLocationId);
		doReturn(KeyFactory.stringToKey(projectId)).when(manager).validateAndGetProjectId(projectId);
		
		when(mockDao.getStorageLocationLimit(123L, 2L)).thenReturn(Optional.of(new ProjectStorageLocationLimit()));
		
		// Call under test
		manager.setDefaultProjectStorageLimit(projectId, storageLocationId);

		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testSetDefaultProjectStorageLimitWithDefaultStorageLocation() {
		when(mockConfig.getDefaultProjectStorageLimit()).thenReturn(100L);
		// Mimics spring Autowired call
		manager.setDefaultStorageLocationMaxBytes(mockConfig);
		
		String projectId = "123";
		Long storageLocationId = StorageLocationDAO.DEFAULT_STORAGE_LOCATION_ID;
		
		doNothing().when(manager).validateStorageLocationId(storageLocationId);
		doReturn(KeyFactory.stringToKey(projectId)).when(manager).validateAndGetProjectId(projectId);
		
		when(mockDao.getStorageLocationLimit(123L, 1L)).thenReturn(Optional.empty());
		
		// Call under test
		manager.setDefaultProjectStorageLimit(projectId, storageLocationId);
		
		verify(mockDao).setStorageLocationLimit(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), new ProjectStorageLocationLimit()
			.setProjectId(projectId)
			.setStorageLocationId(storageLocationId)
			.setMaxAllowedFileBytes(100L)
		);
		
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testSetProjectStorageLimit() {
		
		String projectId = "123";
		Long storageLocationId = 2L;
		
		doNothing().when(manager).validateStorageLocationId(storageLocationId);
		doReturn(KeyFactory.stringToKey(projectId)).when(manager).validateAndGetProjectId(projectId);
		
		ProjectStorageLocationLimit limit = new ProjectStorageLocationLimit()
			.setProjectId(projectId)
			.setStorageLocationId(storageLocationId)
			.setMaxAllowedFileBytes(1024L);
		
		when(mockDao.setStorageLocationLimit(planManagerUser.getId(), limit)).thenReturn(limit);
		
		// Call under test
		assertEquals(limit, manager.setProjectStorageLimit(planManagerUser, limit));
		
		// Emulate the timer call
		manager.sendProjectStorageNotifications();
		
		verify(mockMessenger).publishMessageAfterCommit(new ProjectStorageEvent()
			.setObjectType(ObjectType.PROJECT_STORAGE_EVENT)
			.setObjectId(projectId)
			.setProjectId(KeyFactory.stringToKey(projectId))
		);
		
		verifyNoMoreInteractions(mockDao, mockMessenger);
	}
	
	@Test
	public void testSetProjectStorageLimitWithNullLimit() {
		
		String projectId = "123";
		Long storageLocationId = 2L;
		
		doNothing().when(manager).validateStorageLocationId(storageLocationId);
		doReturn(KeyFactory.stringToKey(projectId)).when(manager).validateAndGetProjectId(projectId);
		
		ProjectStorageLocationLimit limit = new ProjectStorageLocationLimit()
			.setProjectId(projectId)
			.setStorageLocationId(storageLocationId)
			.setMaxAllowedFileBytes(null);
		
		when(mockDao.setStorageLocationLimit(planManagerUser.getId(), limit)).thenReturn(limit);
		
		// Call under test
		assertEquals(limit, manager.setProjectStorageLimit(planManagerUser, limit));
		
		// Emulate the timer call
		manager.sendProjectStorageNotifications();
		
		verify(mockMessenger).publishMessageAfterCommit(new ProjectStorageEvent()
			.setObjectType(ObjectType.PROJECT_STORAGE_EVENT)
			.setObjectId(projectId)
			.setProjectId(KeyFactory.stringToKey(projectId))
		);
		
		verifyNoMoreInteractions(mockDao, mockMessenger);
	}
	
	@Test
	public void testSetProjectStorageLimitWithUnauthorized() {
		planManagerUser.getGroups().clear();
		
		Long projectId = 123L;
		Long storageLocationId = 2L;		
		
		ProjectStorageLocationLimit limit = new ProjectStorageLocationLimit()
			.setProjectId(projectId.toString())
			.setStorageLocationId(storageLocationId)
			.setMaxAllowedFileBytes(1024L);
		
		assertEquals("You are not authorized to perform this operation.", assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			manager.setProjectStorageLimit(planManagerUser, limit);
		}).getMessage());
		
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testSetProjectStorageLimitWithNoUser() {
		
		Long projectId = 123L;
		Long storageLocationId = 2L;
		
		ProjectStorageLocationLimit limit = new ProjectStorageLocationLimit()
			.setProjectId(projectId.toString())
			.setStorageLocationId(storageLocationId)
			.setMaxAllowedFileBytes(1024L);
		
		assertEquals("The user is required.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.setProjectStorageLimit(null, limit);
		}).getMessage());
		
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testSetProjectStorageLimitWithNoLimit() {
				
		assertEquals("The limit is required.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.setProjectStorageLimit(planManagerUser, null);
		}).getMessage());
		
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testSetProjectStorageLimitWithNegativeLimit() {
		
		Long projectId = 123L;
		Long storageLocationId = 2L;
		
		ProjectStorageLocationLimit limit = new ProjectStorageLocationLimit()
			.setProjectId(projectId.toString())
			.setStorageLocationId(storageLocationId)
			.setMaxAllowedFileBytes(-1L);
		
		assertEquals("The maxAllowedFileBytes cannot be a negative number.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.setProjectStorageLimit(planManagerUser, limit);
		}).getMessage());
		
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testVerifyProjectStorageLocationUsageUnderLimitWithUnderLimit() {
		
		when(mockFeatureManager.isFeatureEnabled(Feature.ENFORCE_PROJECT_STORAGE_LIMITS)).thenReturn(true);
		
		String projectId = "123";
		Long storageLocationId = 2L;
		
		doReturn(Optional.of(new ProjectStorageLocationUsage()
			.setSumFileBytes(1024L)
			.setMaxAllowedFileBytes(null)
			.setIsOverLimit(false)
		)).when(manager).getProjectStorageLocationUsage(projectId, storageLocationId);
		
		// Call under test
		manager.verifyProjectStorageLocationUsageUnderLimit(projectId, storageLocationId);
	}
	
	@Test
	public void testVerifyProjectStorageLocationUsageUnderLimitWithOverLimit() {
		when(mockFeatureManager.isFeatureEnabled(Feature.ENFORCE_PROJECT_STORAGE_LIMITS)).thenReturn(true);
		
		String projectId = "123";
		Long storageLocationId = 2L;
		
		doReturn(Optional.of(new ProjectStorageLocationUsage()
			.setSumFileBytes(4096L)
			.setMaxAllowedFileBytes(2048L)
			.setIsOverLimit(true)
		)).when(manager).getProjectStorageLocationUsage(projectId, storageLocationId);
		
		assertEquals("The project storage usage exceeds the limit for the storage location (Project: 123, Storage Location: 2, Usage: 4 KiB, Limit: 2 KiB).", assertThrows(ProjectStorageLimitExceededException.class, () -> {			
			// Call under test
			manager.verifyProjectStorageLocationUsageUnderLimit(projectId, storageLocationId);
		}).getMessage());
	}
	
	@Test
	public void testVerifyProjectStorageLocationUsageUnderLimitWithNoLimitDefined() {
		when(mockFeatureManager.isFeatureEnabled(Feature.ENFORCE_PROJECT_STORAGE_LIMITS)).thenReturn(true);
		
		String projectId = "123";
		Long storageLocationId = 2L;
		
		doReturn(Optional.empty()).when(manager).getProjectStorageLocationUsage(projectId, storageLocationId);
		
		// Call under test
		manager.verifyProjectStorageLocationUsageUnderLimit(projectId, storageLocationId);
		
		// This case used to throw an exception, but it started breaking existing pipelines so we removed it
//		assertEquals("The storage location 2 is not assigned to the project 123.", assertThrows(IllegalArgumentException.class, () -> {			
//			// Call under test
//			manager.verifyProjectStorageLocationUsageUnderLimit(projectId, storageLocationId);
//		}).getMessage());
	}
	
	@Test
	public void testVerifyProjectStorageLocationUsageUnderLimitWithFeatureDisabled() {
		
		when(mockFeatureManager.isFeatureEnabled(Feature.ENFORCE_PROJECT_STORAGE_LIMITS)).thenReturn(false);
		
		String projectId = "123";
		Long storageLocationId = 2L;
		
		// Call under test
		manager.verifyProjectStorageLocationUsageUnderLimit(projectId, storageLocationId);
		
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testValidateAndGetProjectId() {
		
		String projectId = "syn123";
		
		when(mockNodeDao.getNodeTypeById(projectId)).thenReturn(EntityType.project);
		
		// Call under test
		assertEquals(123, manager.validateAndGetProjectId(projectId));
	}
	
	@Test
	public void testValidateAndGetProjectIdEmptyId() {
		
		assertEquals("The projectId is required and must not be the empty string.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validateAndGetProjectId(null);
		}).getMessage());
		
		assertEquals("The projectId is required and must not be a blank string.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validateAndGetProjectId(" ");
		}).getMessage());
	}
	
	@ParameterizedTest
	@EnumSource(value = EntityType.class, mode = Mode.EXCLUDE, names = "project")
	public void testValidateAndGetProjectIdWithWrongNodeType(EntityType entityType) {
		
		String projectId = "syn123";
		
		when(mockNodeDao.getNodeTypeById(projectId)).thenReturn(entityType);
		
		assertEquals("The entity with the given id is not a project.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validateAndGetProjectId(projectId);
		}).getMessage());
	}
	
	@Test
	public void testValidateStorageLocationId() {
		Long storageLocationId = 2L;
		
		when(mockStorageLocationDao.exists(storageLocationId)).thenReturn(true);
		
		// Call under test
		manager.validateStorageLocationId(storageLocationId);
	}
	
	@Test
	public void testValidateStorageLocationIdWithNotExists() {
		Long storageLocationId = 2L;
		
		when(mockStorageLocationDao.exists(storageLocationId)).thenReturn(false);
		
		assertEquals("A storage location with id 2 does not exist.", assertThrows(NotFoundException.class, () -> {			
			// Call under test
			manager.validateStorageLocationId(storageLocationId);
		}).getMessage());
	}
	
	@Test
	public void testValidateStorageLocationIdWithNull() {
		Long storageLocationId = null;
		
		assertEquals("The storageLocationId is required.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.validateStorageLocationId(storageLocationId);
		}).getMessage());
		
		verifyNoMoreInteractions(mockStorageLocationDao);
	}
	
}
