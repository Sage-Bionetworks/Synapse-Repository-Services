package org.sagebionetworks;

import org.sagebionetworks.securitytools.StackEncrypter;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Provides static access to the dependency injected EncryptionUtils
 *
 */
public class EncryptionUtilsSingleton {

	private static final StackEncrypter singleton;
	static {
		// Guice provides dependency injection.
		Injector injector = Guice.createInjector(new StackConfigurationGuiceModule());
		singleton = injector.getInstance(StackEncrypter.class);
	}
	
	/**
	 * Singleton access to the dependency injected EncryptionUtils
	 * @return
	 */
	public static StackEncrypter singleton() {
		return singleton;
	}
}
