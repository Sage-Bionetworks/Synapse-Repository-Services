package org.sagebionetworks.table.cluster.metadata;

/**
 * Factory for {@link ObjectFieldModelResolver} instances
 * 
 * @author Marco Marasca
 *
 */
public interface ObjectFieldModelResolverFactory {

	/**
	 * @param fieldTypeMapper
	 * @return An instance of an {@link ObjectFieldModelResolver} that can be used
	 *         to map default object fields
	 */
	ObjectFieldModelResolver getObjectFieldModelResolver(ObjectFieldTypeMapper fieldTypeMapper);

}
