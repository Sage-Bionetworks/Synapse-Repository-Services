package org.sagebionetworks.repo.manager.statistics;

import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.web.NotFoundException;

import java.util.Optional;

/**
 * Utility class used to resolve a project id from a file handle association
 * 
 * @author Marco
 *
 */
public interface ProjectResolver {

	Optional<Long> resolveProject(FileHandleAssociateType associationType, String associationId) throws UnsupportedOperationException, NotFoundException, IllegalStateException;

}
