package org.sagebionetworks.repo.model;

import java.util.Optional;

/**
 * Provides both binding and retrieval of the identity provider that authenticated the current
 * thread's request.
 *
 * The name is read from the access token at the start of a web service request and bound to the
 * thread local, from where {@link UserInfo} picks it up as it is constructed. It is absent for a
 * request that nothing authenticated, and for work that does not arise from a request at all.
 */
public class IdentityProviderThreadLocal {

	// The default initial value will be null for each thread.
	private static final ThreadLocal<String> identityProviderThreadLocal = new ThreadLocal<String>();

	/**
	 * The name of the identity provider bound to the current thread.
	 *
	 * @return {@link Optional#empty()} if nothing authenticated this thread's request.
	 */
	public static Optional<String> getThreadsIdentityProvider() {
		return Optional.ofNullable(identityProviderThreadLocal.get());
	}

	/**
	 * Bind the given identity provider name to the calling thread. A null clears it.
	 */
	public static void setThreadsIdentityProvider(String identityProvider) {
		identityProviderThreadLocal.set(identityProvider);
	}

	/**
	 * Clear the identity provider for the calling thread.
	 */
	public static void clearThreadsIdentityProvider() {
		identityProviderThreadLocal.set(null);
	}

}
