package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.IdVersionTableType;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.LocalStackChangeMesssage;

@ExtendWith(MockitoExtension.class)
public class MissingTableStatusWorkerTest {

	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private TableStatusDAO mockTableStatusDao;
	@Mock
	private RepositoryMessagePublisher mockRepositoryMessagePublisher;
	@Captor
	private ArgumentCaptor<LocalStackChangeMesssage> messageCapture;

	@InjectMocks
	private MissingTableStatusWorker worker;

	@Test
	public void testRunWithTableWithMultiple() throws Exception {

		when(mockTableStatusDao.getAllTablesAndViewsWithMissingStatus(anyLong()))
				.thenReturn(List.of(new IdVersionTableType(IdAndVersion.parse("syn123.2"), TableType.table),
						new IdVersionTableType(IdAndVersion.parse("syn123"), TableType.table),
						new IdVersionTableType(IdAndVersion.parse("syn456.2"), TableType.materializedview),
						new IdVersionTableType(IdAndVersion.parse("syn789.3"), TableType.entityview)));

		// call under test
		worker.run(mockCallback);
		
		verify(mockRepositoryMessagePublisher, times(4)).fireLocalStackMessage(messageCapture.capture());
		List<LocalStackChangeMesssage> message = messageCapture.getAllValues();
		// zero
		LocalStackChangeMesssage m = message.get(0);
		assertEquals("123", m.getObjectId());
		assertEquals(2L, m.getObjectVersion());
		assertEquals(TableType.table.getObjectType(), m.getObjectType());
		assertEquals(ChangeType.UPDATE, m.getChangeType());
		assertNotNull(m.getTimestamp());
		assertEquals(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), m.getUserId());
		
		// one
		m = message.get(1);
		assertEquals("123", m.getObjectId());
		assertEquals(null, m.getObjectVersion());
		assertEquals(TableType.table.getObjectType(), m.getObjectType());
		assertEquals(ChangeType.UPDATE, m.getChangeType());
		assertNotNull(m.getTimestamp());
		assertEquals(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), m.getUserId());
		
		// two
		m = message.get(2);
		assertEquals("456", m.getObjectId());
		assertEquals(2L, m.getObjectVersion());
		assertEquals(TableType.materializedview.getObjectType(), m.getObjectType());
		assertEquals(ChangeType.UPDATE, m.getChangeType());
		assertNotNull(m.getTimestamp());
		assertEquals(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), m.getUserId());
		
		// three
		m = message.get(3);
		assertEquals("789", m.getObjectId());
		assertEquals(3L, m.getObjectVersion());
		assertEquals(TableType.entityview.getObjectType(), m.getObjectType());
		assertEquals(ChangeType.UPDATE, m.getChangeType());
		assertNotNull(m.getTimestamp());
		assertEquals(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), m.getUserId());
		
	}
	

}
