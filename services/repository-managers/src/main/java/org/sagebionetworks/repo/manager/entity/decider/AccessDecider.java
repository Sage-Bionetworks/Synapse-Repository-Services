package org.sagebionetworks.repo.manager.entity.decider;

import java.util.Arrays;
import java.util.Optional;

import static org.sagebionetworks.repo.model.AuthorizationConstants.*;

import org.sagebionetworks.repo.model.auth.AuthorizationStatus;

/**
 * Abstraction for a function that attempts to make an access decision based
 * only on the provided context.
 *
 */
@FunctionalInterface
public interface AccessDecider {

	/**
	 * Attempt to make an access decision based only on the provided context. If a
	 * decision cannot be made then return an empty optional.
	 * 
	 * @return
	 */
	Optional<UsersEntityAccessInfo> determineAccess(AccessContext c);

	/**
	 * Make an access decision for the given context using the provided deciders. A
	 * decision is made by asking each decider, in order, to attempt to make a
	 * decision. The first non-empty decision that is found will be returned.
	 * 
	 * @param c        The context provides all of the information about the
	 *                 decision to be made.
	 * @param deciders The ordered AccessDeciders to ask.
	 * @return The first non-empty decision that is found will be returned. If none
	 *         of the deciders provide a non-empty decision, a generic access
	 *         denied will be returned.
	 */
	public static UsersEntityAccessInfo makeAccessDecision(AccessContext c, AccessDecider... deciders) {
		return Arrays.stream(deciders).map(d -> d.determineAccess(c)).filter(r -> r.isPresent()).map(r -> r.get())
				.findFirst().orElse(new UsersEntityAccessInfo(c, AuthorizationStatus.accessDenied(ERR_MSG_ACCESS_DENIED)));
	}
}
