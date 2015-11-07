package org.sagebionetworks.object.snapshot.worker.utils;

import java.util.Map;

import org.sagebionetworks.repo.model.ObjectType;

public class ObjectRecordWriterFactory {

	private Map<ObjectType, ObjectRecordWriter> writerMap;

	// injected
	public void setBuilderMap(Map<ObjectType, ObjectRecordWriter> builderMap) {
		this.writerMap = builderMap;
	}

	/**
	 * 
	 * @param type
	 * @return the object record builder for the requested type
	 */
	public ObjectRecordWriter getObjectRecordBuilder(ObjectType type){
		return writerMap.get(type);
	}
}
