/**
 *
 */
package org.sagebionetworks.repo.web;

import javax.servlet.ServletException;

/**
 * Application exception indicating that the desired resource was not found.
 *
 * @author deflaux
 *
 */
public class NotFoundException extends ServletException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public NotFoundException(String message) {
        super(message);
    }
}
