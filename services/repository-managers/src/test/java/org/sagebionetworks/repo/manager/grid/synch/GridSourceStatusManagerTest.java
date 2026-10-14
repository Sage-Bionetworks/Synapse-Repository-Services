package org.sagebionetworks.repo.manager.grid.synch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridSourceStatus;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class GridSourceStatusManagerTest {

	private static final String SESSION_ID = "321";
	private static final Long RECORD_SET_ID = 123L;

	@Mock
	private GridManager mockGridManager;
	@Mock
	private GridRecordSetSourceStatusManager mockRecordSetSourceStatusManager;

	@InjectMocks
	private GridSourceStatusManager manager;

	private UserInfo user;
	private GridSession session;

	@BeforeEach
	public void before() {
		user = new UserInfo(false, 555L, AuthorizationConstants.DEFAULT_REALM_ID);
		session = new GridSession().setSessionId(SESSION_ID).setSourceEntityId("syn123");
	}

	@Test
	public void testGetGridSourceStatusWithRecordSetSource() {
		setupSource(EntityType.recordset);
		GridSourceStatus expected = new GridSourceStatus().setGridSessionId(SESSION_ID).setSourceEntityId("syn123")
				.setHasChanges(false).setChanges(List.of());
		when(mockRecordSetSourceStatusManager.getGridSourceStatus(user, session)).thenReturn(expected);

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, SESSION_ID);

		assertSame(expected, result);
	}

	@Test
	public void testGetGridSourceStatusWithEntityViewSource() {
		setupSource(EntityType.entityview);

		// call under test
		String message = assertThrows(IllegalArgumentException.class,
				() -> manager.getGridSourceStatus(user, SESSION_ID)).getMessage();

		assertEquals("Grid source status is not available for a grid session with a source of type: entityview",
				message);
		verifyNoInteractions(mockRecordSetSourceStatusManager);
	}

	@Test
	public void testGetGridSourceStatusWithTableSource() {
		setupSource(EntityType.table);

		// call under test
		String message = assertThrows(IllegalArgumentException.class,
				() -> manager.getGridSourceStatus(user, SESSION_ID)).getMessage();

		assertEquals("Grid source status is not available for a grid session with a source of type: table", message);
		verifyNoInteractions(mockRecordSetSourceStatusManager);
	}

	@Test
	public void testGetGridSourceStatusWithNoSource() {
		when(mockGridManager.getGridSession(user, SESSION_ID)).thenReturn(session);
		when(mockGridManager.getSessionSource(SESSION_ID)).thenReturn(Optional.empty());

		// call under test
		String message = assertThrows(NotFoundException.class, () -> manager.getGridSourceStatus(user, SESSION_ID))
				.getMessage();

		assertEquals("Grid session: 321 does not have a source", message);
		verifyNoInteractions(mockRecordSetSourceStatusManager);
	}

	@Test
	public void testGetGridSourceStatusWithNullUser() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.getGridSourceStatus(null, SESSION_ID));

		verifyNoInteractions(mockGridManager, mockRecordSetSourceStatusManager);
	}

	@Test
	public void testGetGridSourceStatusWithNullSessionId() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.getGridSourceStatus(user, null));

		verifyNoInteractions(mockGridManager, mockRecordSetSourceStatusManager);
	}

	/**
	 * The session is loaded through the GridManager, which authorizes the caller,
	 * before the source type is resolved.
	 */
	private void setupSource(EntityType type) {
		when(mockGridManager.getGridSession(user, SESSION_ID)).thenReturn(session);
		when(mockGridManager.getSessionSource(SESSION_ID))
				.thenReturn(Optional.of(new GridSource(RECORD_SET_ID, type)));
	}
}
