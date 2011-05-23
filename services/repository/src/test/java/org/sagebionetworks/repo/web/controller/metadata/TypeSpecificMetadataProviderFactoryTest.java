package org.sagebionetworks.repo.web.controller.metadata;
import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;

/**
 * 
 * @author jmhill
 *
 */
public class TypeSpecificMetadataProviderFactoryTest {

	@Test
	public void testAllTypes() throws DatastoreException{
		// Make sure we can get a metadata provider for each type
		ObjectType[] array = ObjectType.values();
		for(ObjectType type: array){
			// Get provider for this type
			TypeSpecificMetadataProvider provider = TypeSpecificMetadataProviderFactory.getProvider(type);
			assertNotNull(provider);
		}
	}
}
