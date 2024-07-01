package org.sagebionetworks.table.worker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.semaphore.LockContext;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingCallable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TableStatusDeleteWorkerTest {

    @InjectMocks
    TableStatusDeleteWorker worker;
    ChangeMessage change;
    @Mock
    private TableManagerSupport tableManagerSupport;
    @Mock
    private NodeDAO nodeDao;
    @Mock
    private ProgressCallback mockProgressCallback;

    @BeforeEach
    public void beforeEach() {
        String tableId = "syn123";
        change = new ChangeMessage();
        change.setChangeNumber(99L);
        change.setChangeType(ChangeType.DELETE);
        change.setObjectId(tableId);
        change.setObjectType(ObjectType.ENTITY);
    }

    @Test
    public void testRunDeleteTableWithVersion() throws Exception {
        String id = "Syn123";
        long version = 1L;
        IdAndVersion idAndVersion = KeyFactory.idAndVersion(id, version);
        change.setObjectId(id);
        change.setObjectVersion(version);
        when(nodeDao.getNodeTypeById(any())).thenReturn(EntityType.table);
        LockContext expectedLockContext = new LockContext(LockContext.ContextType.TableStatusDelete, idAndVersion);

        // call under test
        worker.run(mockProgressCallback, change);

        verify(nodeDao).getNodeTypeById(idAndVersion.getId().toString());
        verify(tableManagerSupport).tryRunWithTableExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext),
                eq(idAndVersion), any(ProgressingCallable.class));
    }

    @Test
    public void testRunDeleteTableWithoutVersion() throws Exception {
        String id = "Syn123";
        Long version = null;
        IdAndVersion idAndVersion = KeyFactory.idAndVersion(id, version);
        change.setObjectId(id);
        change.setObjectVersion(version);
        when(nodeDao.getNodeTypeById(any())).thenReturn(EntityType.table);
        LockContext expectedLockContext = new LockContext(LockContext.ContextType.TableStatusDelete, idAndVersion);

        // call under test
        worker.run(mockProgressCallback, change);

        verify(nodeDao).getNodeTypeById(idAndVersion.getId().toString());
        verify(tableManagerSupport).tryRunWithTableExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext),
                eq(idAndVersion), any(ProgressingCallable.class));
    }

    @Test
    public void testRunDeleteForNonTableEntity() throws Exception {
        String id = "Syn123";
        Long version = null;
        IdAndVersion idAndVersion = KeyFactory.idAndVersion(id, version);
        change.setObjectId(id);
        change.setObjectVersion(version);
        when(nodeDao.getNodeTypeById(any())).thenReturn(EntityType.project);

        // call under test
        worker.run(mockProgressCallback, change);

        verify(nodeDao).getNodeTypeById(idAndVersion.getId().toString());
        verifyZeroInteractions(tableManagerSupport);
    }

    @Test
    public void testRunDeleteForVirtualTable() throws Exception {
        String id = "Syn123";
        Long version = null;
        IdAndVersion idAndVersion = KeyFactory.idAndVersion(id, version);
        change.setObjectId(id);
        change.setObjectVersion(version);
        when(nodeDao.getNodeTypeById(any())).thenReturn(EntityType.virtualtable);

        // call under test
        worker.run(mockProgressCallback, change);

        verify(nodeDao).getNodeTypeById(idAndVersion.getId().toString());
        verifyZeroInteractions(tableManagerSupport);
    }
}
