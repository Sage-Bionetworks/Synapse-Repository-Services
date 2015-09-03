package org.sagebionetworks.repo.manager.table;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;

public class TableFileHandleAssociationProvider implements FileHandleAssociationProvider {

	@Override
	public Set<String> getFileHandleIdsAssociatedWithObject(
			List<String> fileHandleIds, String objectId) {
		// TODO Auto-generated method stub
		return new HashSet<String>(1);
	}

	@Override
	public ObjectType getObjectTypeForAssociationType() {
		return ObjectType.ENTITY;
	}

}
