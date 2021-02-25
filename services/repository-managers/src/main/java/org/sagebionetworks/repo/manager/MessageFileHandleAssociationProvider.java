package org.sagebionetworks.repo.manager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageContent;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class MessageFileHandleAssociationProvider implements FileHandleAssociationProvider {

	private MessageDAO messageDAO;
	private FileHandleAssociationScanner scanner;
	
	@Autowired
	public MessageFileHandleAssociationProvider(MessageDAO messageDao, NamedParameterJdbcTemplate jdbcTemplate) {
		this.messageDAO = messageDao;
		this.scanner = new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOMessageContent().getTableMapping());
	}

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

	@Override
	public FileHandleAssociationScanner getAssociationScanner() {
		return scanner;
	}

}
