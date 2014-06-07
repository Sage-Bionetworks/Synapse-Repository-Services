package org.sagebionetworks.util;

import java.lang.reflect.Method;

public class Clock {
	private static ClockProvider systemProvider = new DefaultClockProvider();

	private static ClockProvider provider = systemProvider;

	public static long currentTimeMillis() {
		return provider.currentTimeMillis();
	}

	public static void sleep(long millis) throws InterruptedException {
		provider.sleep(millis);
	}

	public static void sleepNoInterrupt(long millis) {
		try {
			provider.sleep(millis);
		} catch (InterruptedException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

	}

	public static ClockProvider getClockProvider() {
		return provider;
	}

	/**
	 * Only call for unit testing. As per John, calling this method in production is grounds for termination
	 * 
	 * @param provider
	 */
	static void setProvider(ClockProvider provider) {
		try {
			Class<?> stackConfigurationClass = Class.forName("org.sagebionetworks.StackConfiguration");
			Method isProductionStackmethod = stackConfigurationClass.getMethod("isProductionStack");
			boolean result = (Boolean) isProductionStackmethod.invoke(null);
			if (result) {
				throw new RuntimeException("This method should never be called in production!");
			}
		} catch (ClassNotFoundException e) {
			// we haven't loaded lib-stackConfiguration
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("isProductionStack is no longer a method on StackConfiguration? " + e.getMessage(), e);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Clock.provider = provider;
	}

	static void setSystemProvider() {
		Clock.provider = systemProvider;
	}
}
