package org.sagebionetworks.repo.model.jdo;

import com.thoughtworks.xstream.XStream;

/**
 *
 * According to XStream's documentation, a creation of an XStream instance is expensive, so it should be reused.
 * When not parsing XStream's Annotations on a Object, the created instance is thread-safe.
 * Additional configuration the XStream object should be done in a static initialization block.
 *
 */
public class XStreamSingleton {
	private static final XStream INSTANCE = new XStream();

	{
		INSTANCE.allowTypeHierarchy(Object.class);
		INSTANCE.ignoreUnknownElements();
	}

	public static XStream getInstance() {
		return INSTANCE;
	}
}
