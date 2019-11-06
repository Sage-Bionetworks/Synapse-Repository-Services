package org.sagebionetworks;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Provides static access to the dependency injected StackConfiguration
 *
 */
public class StackEncrypterSingleton {

	private static final StackEncrypter singleton;
	static {
		// Guice provides dependency injection.
		Injector injector = Guice.createInjector(new StackConfigurationGuiceModule());
		singleton = injector.getInstance(StackEncrypter.class);
	}
	
	/**
	 * Singleton access to the dependency injected StackConfiguration
	 * @return
	 */
	public static StackEncrypter singleton() {
		return singleton;
	}
}
