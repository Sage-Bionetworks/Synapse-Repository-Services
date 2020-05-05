package org.sagebionetworks.table.cluster.metadata;

import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;

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
	Set<ObjectType> supportedObjectTypes();

	/**
	 * @param objectType The object type
	 * @return Whether a {@link MetadataIndexProvider} is registered for the given
	 *         object type
	 */
	boolean supports(ObjectType objectType);

	/**
	 * @param objectType The object type
	 * @return The {@link MetadataIndexProvider} mapped to the given object type
	 * @throws IllegalArgumentException If no {@link MetadataIndexProvider} is
	 *                                  registered for the given object type
	 */
	MetadataIndexProvider getMetadataIndexProvider(ObjectType objectType);

	/**
	 * @param objectType The object type
	 * @return The {@link ObjectFieldModelResolver} mapped to the given object type
	 * @throws IllegalArgumentException If no {@link MetadataIndexProvider} is
	 *                                  registered for the given object type
	 */
	ObjectFieldModelResolver getObjectFieldModelResolver(ObjectType objectType);

}
