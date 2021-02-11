package org.sagebionetworks.repo.manager.entity.decider;

import java.util.Optional;

/**
 * Abstraction for a function that attempts to make an access decision based only
 * on the provided context.
 *
 */
@FunctionalInterface
public interface AccessDecider {

	/**
	 * Attempt to make an access decision based only on the provided context. If a
	 * decision cannot be made then return an empty optional.
	 * 
	 * @param T
	 * @return
	 */
	Optional<UsersEntityAccessInfo> deteremineAccess(AccessContext c);
}
