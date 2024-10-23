package org.sagebionetworks.repo.manager.limits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.limits.ProjectStorageLimitsDao;
import org.sagebionetworks.repo.model.limits.ProjectStorageData;
import org.sagebionetworks.repo.model.limits.ProjectStorageEvent;
import org.sagebionetworks.repo.model.limits.ProjectStorageLocationLimit;
import org.sagebionetworks.repo.model.limits.ProjectStorageLocationUsage;
import org.sagebionetworks.repo.model.limits.ProjectStorageUsage;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.Clock;

@ExtendWith(MockitoExtension.class)
public class ProjectStorageLimitsManagerTest {

	@Mock
	private ProjectStorageLimitsDao mockDao;
	
	@Mock
	private TableIndexDAO mockReplicationDao;
	
	@Mock
	private NodeDAO mockNodeDao;
	
	@Mock
	private TransactionalMessenger mockMessenger;
	
	@Mock
	private Clock mockClock;
	
	@InjectMocks
	private ProjectStorageLimitManager manager;

	@Test
	public void testRefreshProjectStorageData() {
		Long projectId = 123L;
		
		Date now = Date.from(Instant.now());
		
		ProjectStorageData data = new ProjectStorageData().setProjectId(projectId);
		
		when(mockClock.now()).thenReturn(now);
		when(mockDao.isStorageDataModifiedOnAfter(projectId, now.toInstant().minus(Duration.ofMinutes(2)))).thenReturn(false);
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
		when(mockDao.isStorageDataModifiedOnAfter(projectId, now.toInstant().minus(Duration.ofMinutes(2)))).thenReturn(true);
		
		// Call under test
		manager.refreshProjectStorageData(projectId);
		
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void getProjectStorageUsageWithNoStorageData() {
		String projectId = "syn123";
		Long projectIdLong = 123L;
		
		when(mockNodeDao.getNodeTypeById(projectId)).thenReturn(EntityType.project);
		when(mockDao.getStorageLocationLimits(projectIdLong)).thenReturn(List.of(
			new ProjectStorageLocationLimit().setStorageLocationId("2").setMaxAllowedFileBytes(2048L),
			new ProjectStorageLocationLimit().setStorageLocationId("1").setMaxAllowedFileBytes(1024L)
		));
		
		when(mockDao.getStorageData(projectIdLong)).thenReturn(Optional.empty());
		
		ProjectStorageUsage expected = new ProjectStorageUsage()
			.setProjectId(projectId)
			.setLocations(List.of(
				new ProjectStorageLocationUsage().setStorageLocationId("1").setMaxAllowedFileBytes(1024L).setIsOverLimit(false).setSumFileBytes(0L),
				new ProjectStorageLocationUsage().setStorageLocationId("2").setMaxAllowedFileBytes(2048L).setIsOverLimit(false).setSumFileBytes(0L)				
			));
		
		// Call under test
		assertEquals(expected, manager.gerProjectStorageUsage(projectId));
		
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
	public void getProjectStorageUsageWithStorageData() {
		String projectId = "syn123";
		Long projectIdLong = 123L;
		
		when(mockNodeDao.getNodeTypeById(projectId)).thenReturn(EntityType.project);
		when(mockDao.getStorageLocationLimits(projectIdLong)).thenReturn(List.of(
			new ProjectStorageLocationLimit().setStorageLocationId("2").setMaxAllowedFileBytes(2048L),
			new ProjectStorageLocationLimit().setStorageLocationId("1").setMaxAllowedFileBytes(1024L)
		));
		
		when(mockDao.getStorageData(projectIdLong)).thenReturn(Optional.of(new ProjectStorageData()
			.setStorageLocationData(Map.of("1", 512L, "2", 4096L, "3", 2024L))
		));
		
		ProjectStorageUsage expected = new ProjectStorageUsage()
			.setProjectId(projectId)
			.setLocations(List.of(
				new ProjectStorageLocationUsage().setStorageLocationId("1").setMaxAllowedFileBytes(1024L).setIsOverLimit(false).setSumFileBytes(512L),
				new ProjectStorageLocationUsage().setStorageLocationId("2").setMaxAllowedFileBytes(2048L).setIsOverLimit(true).setSumFileBytes(4096L),
				new ProjectStorageLocationUsage().setStorageLocationId("3").setMaxAllowedFileBytes(null).setIsOverLimit(false).setSumFileBytes(2024L)
			));
		
		// Call under test
		assertEquals(expected, manager.gerProjectStorageUsage(projectId));
		
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
	public void getProjectStorageUsageWithNoProjectId() {		
		assertEquals("The projectId is required and must not be the empty string.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.gerProjectStorageUsage(null);
		}).getMessage());
		
		assertEquals("The projectId is required and must not be a blank string.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.gerProjectStorageUsage(" ");
		}).getMessage());
		
		verifyZeroInteractions(mockDao, mockNodeDao, mockReplicationDao, mockClock, mockMessenger);
	}
	
	@ParameterizedTest
	@EnumSource(value = EntityType.class, mode = Mode.EXCLUDE, names = "project")
	public void getProjectStorageUsageWithWrongType(EntityType type) {
		String projectId = "123";
		
		when(mockNodeDao.getNodeTypeById(projectId)).thenReturn(type);
		
		assertEquals("The entity with the given id is not a project.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.gerProjectStorageUsage(projectId);
		}).getMessage());
		
		verifyZeroInteractions(mockDao, mockNodeDao, mockReplicationDao, mockClock, mockMessenger);
	}
	
	@Test
	public void testSendProjectStorageNotification() {
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.project);
		when(mockDao.getStorageLocationLimits(any())).thenReturn(Collections.emptyList());
		when(mockDao.getStorageData(any())).thenReturn(Optional.empty());
		
		// Call under test
		manager.sendProjectStorageNotifications();
		
		verifyZeroInteractions(mockMessenger);
		
		manager.gerProjectStorageUsage("123");
		manager.gerProjectStorageUsage("123");
		manager.gerProjectStorageUsage("456");
		
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
	
}
