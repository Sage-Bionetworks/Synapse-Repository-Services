package org.sagebionetworks.repo.manager.stack;

import java.util.Optional;

/**
 * Service to detect if the current running stack is the production stack.
 * 
 * @author Marco Marasca
 *
 */
public interface ProdDetector {

	/**
	 * Tries to detected if the current running stack is the production stack. The
	 * production stack version information is fetched directly from the /version
	 * endpoint of the prod stack (configured by the stack builder).
	 * <p>
	 * In a development stack (DEV) this check will return an optional containing a
	 * true value since by default we configure this endpoint on itself for testing.
	 * 
	 * @return An optional containing the result of the detection, if the detection
	 *         fails (e.g. the stack is not running or cannot be reached) will
	 *         return an empty optional
	 */
	Optional<Boolean> isProductionStack();

}
