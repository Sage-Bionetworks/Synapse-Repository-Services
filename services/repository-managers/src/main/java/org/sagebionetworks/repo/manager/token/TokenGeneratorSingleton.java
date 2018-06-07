package org.sagebionetworks.repo.manager.token;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.common.util.ClockImpl;

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
	 * @return
	 */
	public static TokenGenerator singleton() {
		return tokenGenerator;
	}
}
