package org.sagebionetworks.drs.util;

import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.rest.doc.DrsException;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.springframework.http.HttpStatus;

import java.util.concurrent.Callable;

public class HandleExceptionUtil {

    public static <V extends JSONEntity> V handleException(Callable<V> function) {
        try {
            return function.call();
        } catch (NotFoundException ex) {
            throw new DrsException(ex.getMessage(), HttpStatus.NOT_FOUND.value());
        } catch (IllegalArgumentException ex) {
            throw new DrsException(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        } catch (UnauthorizedException ex) {
            throw new DrsException(ex.getMessage(), HttpStatus.UNAUTHORIZED.value());
        } catch (UnauthenticatedException ex) {
            throw new DrsException(ex.getMessage(), HttpStatus.FORBIDDEN.value());
        } catch (Exception ex) {
            throw new DrsException(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
