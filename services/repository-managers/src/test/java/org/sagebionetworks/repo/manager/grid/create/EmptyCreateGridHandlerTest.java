package org.sagebionetworks.repo.manager.grid.create;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.grid.PatchStore;
import org.sagebionetworks.repo.manager.grid.SnapshotStore;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.manager.table.query.MainQuery;
import org.sagebionetworks.repo.manager.table.query.QueryTranslations;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.grid.CreateGridSession;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.table.cluster.QueryTranslator;

@ExtendWith(MockitoExtension.class)
public class EmptyCreateGridHandlerTest {

	@Mock
	private GridDao mockGridDao;

	@Mock
	private UserInfo mockUser;

	@Mock
	private TableQueryManager mockQueryManager;

	@Mock
	private AsyncJobProgressCallback mockCallback;

	@Mock
	private QueryTranslations mockQueryTranslattion;
	@Mock
	private MainQuery mockMainQuery;
	@Mock
	private QueryTranslator mockTranslator;
	@Mock
	private EntityManager mockEntityManager;

	@Mock
	SnapshotStore mockSnapshotStore;

	@InjectMocks
	EmptyCreateGridHandler handler;

	@Test
	public void testCanCreate() {
		assertTrue(handler.canCreate(new CreateGridRequest()));
		assertFalse(handler.canCreate(new CreateGridRequest().setRecordSetId("syn123")));
		assertFalse(handler.canCreate(new CreateGridRequest().setInitialQuery(new Query())));
	}

	@Test
	public void testCreateGrid() {
		GridSession session = new GridSession().setSessionId("s123");
		when(mockUser.getId()).thenReturn(11L);
		when(mockGridDao.createGridSession(new CreateGridSession().setUserId(11L))).thenReturn(session);

		// call under test
		CreateGridHandlerResult result = handler.createGrid(mockCallback, mockUser, new CreateGridRequest(),
				mockSnapshotStore);
		assertEquals(new CreateGridHandlerResult().setGridSession(session), result);
	}

}
