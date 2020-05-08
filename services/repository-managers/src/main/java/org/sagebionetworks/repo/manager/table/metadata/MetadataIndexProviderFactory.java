package org.sagebionetworks.repo.manager.table.metadata;

import java.util.Set;

import org.sagebionetworks.repo.model.table.ViewObjectType;

/**
 * Factory for metadata index providers
 * 
 * @author Marco Marasca
 */
public interface MetadataIndexProviderFactory {

	/**
	 * @return The set of object type for which a {@link MetadataIndexProvider} is
	 *         registered
	 */
	Set<ViewObjectType> supportedObjectTypes();

	/**
	 * @param objectType The object type
	 * @return Whether a {@link MetadataIndexProvider} is registered for the given
	 *         object type
	 */
	boolean supports(ViewObjectType objectType);

	/**
	 * @param objectType The object type
	 * @return The {@link MetadataIndexProvider} mapped to the given object type
	 * @throws IllegalArgumentException If no {@link MetadataIndexProvider} is
	 *                                  registered for the given object type
	 */
	MetadataIndexProvider getMetadataIndexProvider(ViewObjectType objectType);

}
