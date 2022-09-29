package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableStatus;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatusChangeEvent;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class TableStatusDAOImplUnitTest {
	
	@Mock
	private DBOBasicDao mockBasicDao;

	@Mock
	private JdbcTemplate mockJdbcTemplate;
		
	@Mock
	private TransactionalMessenger mockMessenger;
	
	@InjectMocks
	private TableStatusDAOImpl dao;
	
	@Mock
	private DBOTableStatus mockStatus;
	
	private TableStatusDAOImpl daoSpy;
	
	private IdAndVersion idAndVersion;

	@BeforeEach
	public void before() {
		daoSpy = Mockito.spy(dao);
		idAndVersion = IdAndVersion.parse("syn123");
	}
	
	private static ArgumentMatcher<TableStatusChangeEvent> matchStatus(TableStatusChangeEvent expected) {
		return event -> {
			// The timestamp is created at runtime so we sync it before equals check
			return expected.setTimestamp(event.getTimestamp()).equals(event);
		};
	}
	
	@Test
	public void testPublishMessageWhenResetTableStatusToPending() {
		TableStatusChangeEvent expectedEvent = new TableStatusChangeEvent()
			.setObjectType(ObjectType.TABLE_STATUS_EVENT)
			.setObjectId(idAndVersion.getId().toString())
			.setObjectVersion(idAndVersion.getVersion().orElse(null))
			.setState(TableState.PROCESSING);
		
		// Call under test
		dao.resetTableStatusToProcessing(idAndVersion);
		
		verify(mockMessenger).publishMessageAfterCommit(argThat(matchStatus(expectedEvent)));
	}
	
	@Test
	public void testPublishMessageWhenAttemptToSetTableStatusToFailed() {

		doReturn(Optional.of(mockStatus)).when(daoSpy).selectResetTokenForUpdate(any());
		
		TableStatusChangeEvent expectedEvent = new TableStatusChangeEvent()
			.setObjectType(ObjectType.TABLE_STATUS_EVENT)
			.setObjectId(idAndVersion.getId().toString())
			.setObjectVersion(idAndVersion.getVersion().orElse(null))
			.setState(TableState.PROCESSING_FAILED);
		
		// Call under test
		daoSpy.attemptToSetTableStatusToFailed(idAndVersion, "message", "error");
		
		verify(mockMessenger).publishMessageAfterCommit(argThat(matchStatus(expectedEvent)));
	}
	
	@Test
	public void testPublishMessageWhenAttemptToSetTableStatusToAvailable() {

		when(mockStatus.getResetToken()).thenReturn("resetToken");
		doReturn(Optional.of(mockStatus)).when(daoSpy).selectResetTokenForUpdate(any());
		
		TableStatusChangeEvent expectedEvent = new TableStatusChangeEvent()
			.setObjectType(ObjectType.TABLE_STATUS_EVENT)
			.setObjectId(idAndVersion.getId().toString())
			.setObjectVersion(idAndVersion.getVersion().orElse(null))
			.setState(TableState.AVAILABLE);
		
		// Call under test
		daoSpy.attemptToSetTableStatusToAvailable(idAndVersion, "resetToken", "etag");
		
		verify(mockMessenger).publishMessageAfterCommit(argThat(matchStatus(expectedEvent)));
	}
	
	@Test
	public void testPublishMessageWhenUpdateChangedOnIfAvailable() {
		
		when(mockJdbcTemplate.update(anyString(), anyLong(), any(), anyLong())).thenReturn(1);
		
		TableStatusChangeEvent expectedEvent = new TableStatusChangeEvent()
			.setObjectType(ObjectType.TABLE_STATUS_EVENT)
			.setObjectId(idAndVersion.getId().toString())
			.setObjectVersion(idAndVersion.getVersion().orElse(null))
			.setState(TableState.AVAILABLE);
		
		// Call under test
		dao.updateChangedOnIfAvailable(idAndVersion);
		
		verify(mockMessenger).publishMessageAfterCommit(argThat(matchStatus(expectedEvent)));
	}
	
	@Test
	public void testPublishMessageWhenUpdateChangedOnIfAvailableWithNoUpdate() {
		
		when(mockJdbcTemplate.update(anyString(), anyLong(), any(), anyLong())).thenReturn(0);
		
		// Call under test
		dao.updateChangedOnIfAvailable(idAndVersion);
		
		verifyZeroInteractions(mockMessenger);
	}

}
