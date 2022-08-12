package org.sagebionetworks.drs.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.drs.DrsManager;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.web.rest.doc.DrsException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.drs.DrsManagerImpl.ILLEGAL_ARGUMENT_ERROR_MESSAGE;

@ExtendWith(MockitoExtension.class)
public class DrsServiceImplTest {
    @InjectMocks
    DrsServiceImpl drsService;
    @Mock
    DrsManager drsManager;

    private static final Long USER_ID = 1L;

    @Test
    public void testGETDrsObjectWithInvalidIDThrowsDrsException() {
        final Project project = getProject("syn1.1","project");
        when(drsManager.getDrsObject(any(),any())).thenThrow(new IllegalArgumentException(ILLEGAL_ARGUMENT_ERROR_MESSAGE));
        final DrsException exception = assertThrows(DrsException.class, () -> {
            drsService.getDrsObject(USER_ID, project.getId());
        });
        assertEquals(ILLEGAL_ARGUMENT_ERROR_MESSAGE, exception.getMessage());
    }

    private Project getProject(final String id, final String name){
        final Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setDescription("test");
        project.setCreatedOn(Date.from(LocalDate.of(2022, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant()));
        project.setModifiedOn(Date.from(LocalDate.of(2022, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant()));
        return project;
    }
}
