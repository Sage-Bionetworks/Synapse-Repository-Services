package org.sagebionetworks.repo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class EntityBundleList implements JSONEntity, Serializable {

	List<EntityBundle> entityBundles;
	
	public static final String JSON_ENTITY_LIST = "entityList";
	
	public EntityBundleList() {
		entityBundles = new ArrayList<EntityBundle>();
	}

	public EntityBundleList(JSONObjectAdapter initializeFrom) throws JSONObjectAdapterException {
		this();
		initializeFromJSONObject(initializeFrom);
	}
	
	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		if (toInitFrom == null) {
            throw new IllegalArgumentException("org.sagebionetworks.schema.adapter.JSONObjectAdapter cannot be null");
        }
		if (toInitFrom.has(JSON_ENTITY_LIST)) {
			JSONArrayAdapter array = toInitFrom.getJSONArray(JSON_ENTITY_LIST);
			for (int i = 0; i < array.length(); i++) {
				entityBundles.add(new EntityBundle(array.getJSONObject(i)));
			}
		}
		return toInitFrom;
	}
	
	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
		if (writeTo == null) {
		        throw new IllegalArgumentException("JSONObjectAdapter cannot be null");
		}
		if (entityBundles != null) {
			JSONArrayAdapter arrayAdapter = writeTo.createNewArray();
			for (int i=0; i<entityBundles.size(); i++) {
				JSONObjectAdapter joa = arrayAdapter.createNew();
				entityBundles.get(i).writeToJSONObject(joa);
				arrayAdapter.put(i, joa);
			}
			writeTo.put(JSON_ENTITY_LIST, arrayAdapter);
		}
		return writeTo;
	}

	@Override
	public String getJSONSchema() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String toString() {
		return "EntityBundleList [entityBundles=" + entityBundles + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((entityBundles == null) ? 0 : entityBundles.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EntityBundleList other = (EntityBundleList) obj;
		if (entityBundles == null) {
			if (other.entityBundles != null)
				return false;
		} else if (!entityBundles.equals(other.entityBundles))
			return false;
		return true;
	}

	public List<EntityBundle> getEntityBundles() {
		return entityBundles;
	}

	public void setEntityBundles(List<EntityBundle> entityBundles) {
		this.entityBundles = entityBundles;
	}
	
}
