package org.sagebionetworks.repo.manager.schema;

public interface ObjectTranslatorProvider {

	/**
	 * Get an ObjectTranslator for a given concrete type. 
	 * @param concreteType
	 * @return
	 */
	public ObjectTranslator getTranslatorForConcreteType(String concreteType);
}
