package org.sagebionetworks;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Provides static access to the dependency injected StackConfiguration
 *
 */
public class EncryptionUtilsSingleton {

	private static final EncryptionUtils singleton;
	static {
		// Guice provides dependency injection.
		Injector injector = Guice.createInjector(new StackConfigurationGuiceModule());
		singleton = injector.getInstance(EncryptionUtils.class);
	}
	
	/**
	 * Singleton access to the dependency injected StackConfiguration
	 * @return
	 */
	public static EncryptionUtils singleton() {
		return singleton;
	}
}
