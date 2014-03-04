package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.principal.AliasCheckRequest;
import org.sagebionetworks.repo.model.principal.AliasCheckResponse;

/**
 * Abstraction for Principal lookup.
 * 
 * @author John
 *
 */
public interface PrincipalService {

	/**
	 * Check if an alias is available.
	 * @param check
	 * @return
	 */
	AliasCheckResponse checkAlias(AliasCheckRequest check);

}
