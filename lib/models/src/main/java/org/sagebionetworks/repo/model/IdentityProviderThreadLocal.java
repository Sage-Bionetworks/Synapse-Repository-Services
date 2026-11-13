package org.sagebionetworks.repo.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.sagebionetworks.repo.model.auth.IdentityProviderName;

/**
 * Provides both binding and retrieval of the identity provider that authenticated the current
 * thread's request.
 *
 * The identity provider is read from the access token at the start of a web service request and
 * bound to the thread local under the id of the principal the token authenticated, from where
 * {@link UserInfo} picks it up as it is constructed. A request may build a UserInfo for principals
 * other than its caller, and since nothing authenticated those principals in this request, they
 * have no identity provider. It is likewise absent for a request that nothing authenticated, and
 * for work that does not arise from a request at all.
 */
public class IdentityProviderThreadLocal {

	// Each thread starts with no bindings.
	private static final ThreadLocal<Map<Long, IdentityProviderName>> identityProviderThreadLocal = ThreadLocal
			.withInitial(HashMap::new);

	/**
	 * The identity provider that authenticated the given principal in the current thread's request.
	 *
	 * @return {@link Optional#empty()} if nothing authenticated the given principal in this thread's
	 *         request.
	 */
	public static Optional<IdentityProviderName> get(Long principalId) {
		return Optional.ofNullable(identityProviderThreadLocal.get().get(principalId));
	}

	/**
	 * Bind the identity provider that authenticated the given principal to the calling thread.
	 */
	public static void set(Long principalId, IdentityProviderName identityProvider) {
		identityProviderThreadLocal.get().put(principalId, identityProvider);
	}

	/**
	 * Clear every identity provider bound to the calling thread.
	 */
	public static void clear() {
		identityProviderThreadLocal.remove();
	}

}
