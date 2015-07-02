package org.sagebionetworks.object.snapshot.worker.utils;

import java.util.Map;

import org.sagebionetworks.repo.model.ObjectType;

public class ObjectRecordBuilderFactory {

	private Map<ObjectType, ObjectRecordBuilder> builderMap;

	// injected
	public void setBuilderMap(Map<ObjectType, ObjectRecordBuilder> builderMap) {
		this.builderMap = builderMap;
	}

	/**
	 * 
	 * @param type
	 * @return the object record builder for the requested type
	 */
	public ObjectRecordBuilder getObjectRecordBuilder(ObjectType type){
		return builderMap.get(type);
	}
}
