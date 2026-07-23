package org.sagebionetworks.repo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.curation.CurationTaskManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.TaskBundle;
import org.sagebionetworks.repo.model.curation.TaskState;
import org.sagebionetworks.repo.model.curation.TaskStatus;

@ExtendWith(MockitoExtension.class)
public class CurationTaskServiceImplUnitTest {

    @Mock
    private CurationTaskManager mockManager;

    @Mock
    private UserManager mockUserManager;

    @InjectMocks
    private CurationTaskServiceImpl service;

    private final Long userId = 101L;
    private final Long taskId = 987L;
    private UserInfo userInfo;

    @BeforeEach
    public void setup() {
        userInfo = new UserInfo(false, userId);
    }

    @Test
    public void testUpdateTaskBundleSetsTaskIdFromPath() {
        // The body carries no taskId; the URL path taskId is authoritative and should be applied.
        TaskBundle bundle = new TaskBundle()
                .setTask(new CurationTask())
                .setStatus(new TaskStatus().setState(TaskState.IN_PROGRESS));
        TaskBundle updated = new TaskBundle().setTask(new CurationTask().setTaskId(taskId));

        when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
        when(mockManager.updateTaskBundle(eq(userInfo), eq(bundle))).thenReturn(updated);

        // call under test
        TaskBundle result = service.updateTaskBundle(userId, taskId, bundle);

        assertSame(updated, result);
        assertEquals(taskId, bundle.getTask().getTaskId());
    }

    @Test
    public void testUpdateTaskBundleRejectsTaskIdMismatch() {
        // The body's taskId disagrees with the URL path taskId — reject rather than trust either.
        TaskBundle bundle = new TaskBundle()
                .setTask(new CurationTask().setTaskId(555L))
                .setStatus(new TaskStatus().setState(TaskState.IN_PROGRESS));

        // call under test
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateTaskBundle(userId, taskId, bundle));
        assertTrue(ex.getMessage().contains("does not match"));

        verify(mockManager, never()).updateTaskBundle(any(), any());
    }
}
