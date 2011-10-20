package org.sagebionetworks.registry;

import java.util.ArrayList;

import org.junit.Test;
import org.sagebionetworks.EntityRegistry;
import org.sagebionetworks.EntityTypeMetadata;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * This test has a limited live span. After we completely convert from ObjectType to EntityType we can delete this test.
 * 
 * @author jmhill
 *
 */
public class ObjectTypeToRegister {
	
	@Test
	public void testCreateRegisterFromObjectType() throws JSONObjectAdapterException{
		// Build up an example register		
		EntityRegistry sample = new EntityRegistry();
		sample.setEntityTypes(new ArrayList<EntityTypeMetadata>());
		
		// Create a copy using the current Object type as a template
		for(ObjectType type: ObjectType.values()){
			EntityTypeMetadata typeMeta = new EntityTypeMetadata();
			typeMeta.setDefaultParentPath(type.getDefaultParentPath());
			typeMeta.setUrlPrefix(type.getUrlPrefix());
			typeMeta.setClassName("org.sagebionetworks."+type.getClassForType().getSimpleName());
			typeMeta.setValidParentTypes(new ArrayList<String>());
			for(String parentType: type.getValidParentTypes()){
				typeMeta.getValidParentTypes().add(parentType);
			}
			sample.getEntityTypes().add(typeMeta);
		}
		
		// Now write this to JSON
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		sample.writeToJSONObject(adapter);
		System.out.println(adapter.toJSONString());
	}

}
