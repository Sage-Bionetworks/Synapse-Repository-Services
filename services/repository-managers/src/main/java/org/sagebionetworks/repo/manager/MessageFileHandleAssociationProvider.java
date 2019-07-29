package org.sagebionetworks.repo.manager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.springframework.beans.factory.annotation.Autowired;

public class MessageFileHandleAssociationProvider implements FileHandleAssociationProvider {

	@Autowired
	private MessageDAO messageDAO;

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		MessageToUser message = messageDAO.getMessage(objectId);
		Set<String> associatedIds = new HashSet<String>();
		if (fileHandleIds.contains(message.getFileHandleId())) {
			associatedIds.add(message.getFileHandleId());
		}
		return associatedIds;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.MESSAGE;
	}

}
