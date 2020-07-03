package org.sagebionetworks.repo.manager.schema;

import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;

public enum SchemaObjectType {

	FILE_ENTITY(FileEntity.class.getName(), new EntityObjectTranslator()),
	FOLDER_ENTITY(Folder.class.getName(), new EntityObjectTranslator()),
	PROJECT_ENTITY(Project.class.getName(), new EntityObjectTranslator());

	String concreteType;
	ObjectTranslator objectTranslator;

	SchemaObjectType(String concreteType, ObjectTranslator objectTranslator) {
		this.concreteType = concreteType;
		this.objectTranslator = objectTranslator;
	}

	/**
	 * Lookup by concrete type.
	 * @param concreteType
	 * @return
	 */
	public static SchemaObjectType valueOfConcreteType(String concreteType) {
		for (SchemaObjectType type : SchemaObjectType.values()) {
			if (type.concreteType.equals(concreteType)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown concreteType: " + concreteType);
	}

	/**
	 * @return the concreteType
	 */
	public String getConcreteType() {
		return concreteType;
	}

	/**
	 * @return the objectTranslator
	 */
	public ObjectTranslator getObjectTranslator() {
		return objectTranslator;
	}
	
}
