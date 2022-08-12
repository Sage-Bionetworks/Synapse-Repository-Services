package org.sagebionetworks.drs.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.drs.DrsManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.rest.doc.DrsException;
import org.springframework.http.HttpStatus;

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
    private static final String PROJECT_ID = "syn123456";

    @Test
    public void testGETDrsObjectWithInvalidIDThrowsDrsException() {
        when(drsManager.getDrsObject(any(),any())).thenThrow(new IllegalArgumentException(ILLEGAL_ARGUMENT_ERROR_MESSAGE));
        final DrsException exception = assertThrows(DrsException.class, () -> {
            drsService.getDrsObject(USER_ID, PROJECT_ID);
        });
        assertEquals(ILLEGAL_ARGUMENT_ERROR_MESSAGE, exception.getMessage());
        assertEquals(exception.getStatusCode(), HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void testGETDrsObjectThrowsNullPointerException() {
        final String errorMessage = "Null pointer exception";
        when(drsManager.getDrsObject(any(),any())).thenThrow(new NullPointerException(errorMessage));
        final DrsException exception=assertThrows(DrsException.class, () -> drsService.getDrsObject(USER_ID, PROJECT_ID));
        assertEquals(exception.getMsg(),errorMessage);
        assertEquals(exception.getStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    public void testGETDrsObjectThrowsNotFoundException() {
        final  NotFoundException notFoundException = new NotFoundException(String.format("Drs object id %s does not exists", PROJECT_ID));
        when(drsManager.getDrsObject(any(),any())).thenThrow(notFoundException);
        final DrsException exception = assertThrows(DrsException.class, () -> {
            drsService.getDrsObject(USER_ID, PROJECT_ID);
        });

        assertEquals(exception.getMsg(),notFoundException.getMessage());
        assertEquals(exception.getStatusCode(), HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void testGETDrsObjectThrowsUnauthorizedException() {
        when(drsManager.getDrsObject(any(),any())).thenThrow(new UnauthorizedException());
        final DrsException exception=assertThrows(DrsException.class, () -> drsService.getDrsObject(USER_ID, PROJECT_ID));
        assertEquals(exception.getStatusCode(), HttpStatus.UNAUTHORIZED.value());
    }

}
