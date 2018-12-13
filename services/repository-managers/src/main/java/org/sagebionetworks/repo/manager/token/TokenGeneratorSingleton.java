package org.sagebionetworks.repo.manager.token;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.common.util.ClockImpl;

/**
 * This is a temporary class used to bridge static access to
 * {@link TokenGenerator}.
 * 
 * @deprecated The {@link TokenGenerator} has dependencies and should only be
 *             accessed via dependency injection.
 */
public class TokenGeneratorSingleton {

	/**
	 * Dependency injected singleton.
	 * 
	 */
	private static final TokenGenerator tokenGenerator;
	static {
		tokenGenerator = new TokenGeneratorImpl(StackConfigurationSingleton.singleton(), new ClockImpl());
	}

	/**
	 * Singleton access to the token generator.
	 * 
	 * @deprecated The {@link TokenGenerator} has dependencies and should only be
	 *             accessed via dependency injection.
	 * @return
	 */
	public static TokenGenerator singleton() {
		return tokenGenerator;
	}
}
