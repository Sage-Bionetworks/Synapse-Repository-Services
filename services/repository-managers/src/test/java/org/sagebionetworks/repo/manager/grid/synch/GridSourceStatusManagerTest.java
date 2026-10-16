package org.sagebionetworks.repo.manager.grid.synch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridSourceStatus;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class GridSourceStatusManagerTest {

	private static final String SESSION_ID = "321";
	private static final String SOURCE_ID = "syn123";

	@Mock
	private GridManager mockGridManager;
	@Mock
	private EntityManager mockEntityManager;
	@Mock
	private GridRecordSetSourceStatusManager mockRecordSetSourceStatusManager;

	@InjectMocks
	private GridSourceStatusManager manager;

	private UserInfo user;
	private GridSession session;

	@BeforeEach
	public void before() {
		user = new UserInfo(false, 555L, AuthorizationConstants.DEFAULT_REALM_ID);
		session = new GridSession().setSessionId(SESSION_ID).setSourceEntityId(SOURCE_ID);
	}

	@Test
	public void testGetGridSourceStatusWithRecordSetSource() {
		RecordSet recordSet = new RecordSet().setId(SOURCE_ID);
		setupSource(recordSet);
		GridSourceStatus expected = new GridSourceStatus().setGridSessionId(SESSION_ID).setSourceEntityId(SOURCE_ID)
				.setSessionSourceVersionNumber(1L).setCurrentSourceVersionNumber(1L).setHasChanges(false)
				.setChanges(List.of());
		when(mockRecordSetSourceStatusManager.getGridSourceStatus(session, recordSet)).thenReturn(expected);

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, SESSION_ID);

		assertSame(expected, result);
	}

	@Test
	public void testGetGridSourceStatusWithEntityViewSource() {
		setupSource(new EntityView().setId(SOURCE_ID));

		// call under test
		String message = assertThrows(IllegalArgumentException.class,
				() -> manager.getGridSourceStatus(user, SESSION_ID)).getMessage();

		assertEquals("Grid source status is not available for a grid session with a source of type: EntityView",
				message);
		verifyNoInteractions(mockRecordSetSourceStatusManager);
	}

	@Test
	public void testGetGridSourceStatusWithTableSource() {
		setupSource(new TableEntity().setId(SOURCE_ID));

		// call under test
		String message = assertThrows(IllegalArgumentException.class,
				() -> manager.getGridSourceStatus(user, SESSION_ID)).getMessage();

		assertEquals("Grid source status is not available for a grid session with a source of type: TableEntity",
				message);
		verifyNoInteractions(mockRecordSetSourceStatusManager);
	}

	@Test
	public void testGetGridSourceStatusWithNoSource() {
		session.setSourceEntityId(null);
		when(mockGridManager.getGridSession(user, SESSION_ID)).thenReturn(session);

		// call under test
		String message = assertThrows(NotFoundException.class, () -> manager.getGridSourceStatus(user, SESSION_ID))
				.getMessage();

		assertEquals("Grid session: 321 does not have a source", message);
		verifyNoInteractions(mockEntityManager, mockRecordSetSourceStatusManager);
	}

	/**
	 * The source is read without its own authorization check, so a caller denied
	 * access to the session must never reach that read.
	 */
	@Test
	public void testGetGridSourceStatusWithUnauthorizedSession() {
		UnauthorizedException denied = new UnauthorizedException("denied");
		when(mockGridManager.getGridSession(user, SESSION_ID)).thenThrow(denied);

		// call under test
		UnauthorizedException result = assertThrows(UnauthorizedException.class,
				() -> manager.getGridSourceStatus(user, SESSION_ID));

		assertSame(denied, result);
		verifyNoInteractions(mockEntityManager, mockRecordSetSourceStatusManager);
	}

	@Test
	public void testGetGridSourceStatusWithNullUser() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.getGridSourceStatus(null, SESSION_ID));

		verifyNoInteractions(mockGridManager, mockEntityManager, mockRecordSetSourceStatusManager);
	}

	@Test
	public void testGetGridSourceStatusWithNullSessionId() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.getGridSourceStatus(user, null));

		verifyNoInteractions(mockGridManager, mockEntityManager, mockRecordSetSourceStatusManager);
	}

	/**
	 * The session is loaded through the GridManager, which authorizes the caller,
	 * before the source is read and its type resolved.
	 */
	private void setupSource(Entity source) {
		when(mockGridManager.getGridSession(user, SESSION_ID)).thenReturn(session);
		when(mockEntityManager.getEntityWithoutAuthorization(SOURCE_ID, null)).thenReturn(source);
	}
}
