package org.sagebionetworks.table.cluster.metadata;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.springframework.stereotype.Component;

@Component
public class TestObjectFieldTypeEntityMapper implements ObjectFieldTypeMapper {

	@Override
	public ObjectType getObjectType() {
		return ObjectType.ENTITY;
	}

	@Override
	public ColumnType getIdColumnType() {
		return ColumnType.ENTITYID;
	}

	@Override
	public ColumnType getParentIdColumnType() {
		return ColumnType.ENTITYID;
	}

	@Override
	public ColumnType getBenefactorIdColumnType() {
		return ColumnType.ENTITYID;
	}

}
