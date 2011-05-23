package org.sagebionetworks.repo.web.controller.metadata;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;

/**
 * Factory for creating TypeSpecificMetadataProviders 
 * @author jmhill
 *
 */
public class TypeSpecificMetadataProviderFactory {
	
	public static final String METADATA_PROVIDER_IMPL_SUFFIX = "MetadataProvider";
	
	/**
	 * Get the provider for a given Object type.
	 * @param type
	 * @return
	 * @throws DatastoreException 
	 */
	public static <T extends Base> TypeSpecificMetadataProvider<T> getProvider(ObjectType type) throws DatastoreException {
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// Use the DTO class name to determine what metadata provider class name
		Class<? extends Base> dtoClazz = type.getClassForType();
		// Use this package name.
		String packageName = TypeSpecificMetadataProviderFactory.class.getPackage().getName();
		String className = packageName+"."+dtoClazz.getSimpleName()+METADATA_PROVIDER_IMPL_SUFFIX;
		// Use reflection to create the class
		try {
			@SuppressWarnings("unchecked")
			Class<? extends TypeSpecificMetadataProvider<T>>  providerClass  = (Class<? extends TypeSpecificMetadataProvider<T>>) Class.forName(className);
			return providerClass.newInstance();
		} catch (ClassNotFoundException e) {
			throw new DatastoreException("Cannot find a MedataDataProvider Class for: "+dtoClazz.getSimpleName()+" using: "+className);
		} catch (InstantiationException e) {
			throw new DatastoreException("Missing public no-argument constrcutor for: "+className);
		} catch (IllegalAccessException e) {
			throw new DatastoreException("Missing public no-argument constrcutor for: "+className);
		}
	}

}
