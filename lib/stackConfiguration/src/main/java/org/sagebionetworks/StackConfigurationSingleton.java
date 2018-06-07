package org.sagebionetworks;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Provides static access to the dependency injected StackConfiguration
 *
 */
public class StackConfigurationSingleton {

	private static final StackConfiguration singleton;
	static {
		// Guice provides dependency injection.
		Injector injector = Guice.createInjector(new StackConfigurationGuiceModule());
		singleton = injector.getInstance(StackConfiguration.class);
	}
	
	/**
	 * Singleton access to the dependency injected StackConfiguration
	 * @return
	 */
	public static StackConfiguration singleton() {
		return singleton;
	}
}
