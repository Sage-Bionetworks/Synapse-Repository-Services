package org.sagebionetworks;

import org.sagebionetworks.securitytools.EncryptionUtils;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Provides static access to the dependency injected EncryptionUtils
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
	 * Singleton access to the dependency injected EncryptionUtils
	 * @return
	 */
	public static EncryptionUtils singleton() {
		return singleton;
	}
}
