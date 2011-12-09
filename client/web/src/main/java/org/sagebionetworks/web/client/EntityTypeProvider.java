package org.sagebionetworks.web.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.gwt.client.schema.adapter.JSONObjectGwt;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.repo.model.registry.EntityTypeMetadata;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.web.shared.EntityType;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

public class EntityTypeProvider {

	private List<EntityTypeMetadata> typeMetadatas;
	private List<EntityType> values;
	
	@Inject
	public EntityTypeProvider(SynapseClientAsync synapseClient, final JSONObjectAdapter jsonObjectAdapter) {		
		synapseClient.getEntityTypeRegistryJSON(new AsyncCallback<String>() {			
			@Override
			public void onSuccess(String registryJson) {
				try {
					JSONObjectAdapter registryAdaptor = jsonObjectAdapter.createNew(registryJson);
					EntityRegistry registry = new EntityRegistry(registryAdaptor);
					typeMetadatas = registry.getEntityTypes();
					createEntityTypes();					
				} catch (JSONObjectAdapterException e) {
					int i=0;
				}
				
			}
			
			@Override
			public void onFailure(Throwable caught) {				
			}
		});
	}
	
		
	public List<EntityType> getEntityTypes() {
		return values;
	}

	public EntityType getEntityTypeForEntity(Entity entity) {
		return getEntityTypeForUri(entity.getUri());
	}
	
	public EntityType getEntityTypeForUri(String uri) {
		if(uri == null) throw new IllegalArgumentException("URI cannot be null");
		int maxIndex = -1;
		EntityType maxType = null;
		for(EntityType type: values) {
			int index = uri.lastIndexOf(type.getUrlPrefix());
			if(index > maxIndex){
				maxIndex = index;
				maxType = type;
			}
		}
		if(maxType != null) return maxType;
		throw new IllegalArgumentException("Unknown Entity type for URI: "+uri);
	}	
	
	/*
	 * Private Methods
	 */
	private void createEntityTypes() {
		values = new ArrayList<EntityType>();
		if(typeMetadatas != null) {
			Map<String, EntityType> pathToType = new HashMap<String, EntityType>();
			
			// create each type
			for(EntityTypeMetadata meta : typeMetadatas) {				
				EntityType type = new EntityType(meta.getName(),
						meta.getUrlPrefix(), meta.getClassName(),
						meta.getDefaultParentPath(), meta);
				pathToType.put(type.getUrlPrefix(), type);
				values.add(type);				
			}
			
			// fill in parents
			for(EntityType type : values) {
				List<EntityType> parents = new ArrayList<EntityType>();
				for(String parentUrlString : type.getMetadata().getValidParentTypes()) {
					if(pathToType.containsKey(parentUrlString)) {
						EntityType parent = pathToType.get(parentUrlString);
						if(!parents.contains(parent)) {
							parents.add(parent);
						}
					}
				}
				type.setValidParentTypes(parents);
			}
			
			// calculate and fill children			
			Map<EntityType, Set<EntityType>> typeToChildTypes = new HashMap<EntityType, Set<EntityType>>();
			for(EntityType type : values) {
				for(EntityType parent : type.getValidParentTypes()) {					
					if(!typeToChildTypes.containsKey(parent)) {
						typeToChildTypes.put(parent, new HashSet<EntityType>());
					}
					// add this type to its parent
					typeToChildTypes.get(parent).add(type);
				}
			}
			for(EntityType type : values) {
				if(typeToChildTypes.containsKey(type)) {
					Set<EntityType> children = typeToChildTypes.get(type);					
					type.setValidChildTypes(new ArrayList<EntityType>(children));
				}
			}

		}
	}

}
