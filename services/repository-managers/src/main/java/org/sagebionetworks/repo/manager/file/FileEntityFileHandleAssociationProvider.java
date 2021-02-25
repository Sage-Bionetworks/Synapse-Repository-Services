package org.sagebionetworks.repo.manager.file;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class FileEntityFileHandleAssociationProvider implements FileHandleAssociationProvider {

	private NodeManager nodeManager;
	
	private FileHandleAssociationScanner scanner;
	
	@Autowired
	public FileEntityFileHandleAssociationProvider(NodeManager nodeManager, NamedParameterJdbcTemplate jdbcTemplate) {
		this.nodeManager = nodeManager;
		this.scanner = new BasicFileHandleAssociationScanner(jdbcTemplate, new DBORevision().getTableMapping());
	}
	
	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		return nodeManager.getFileHandleIdsAssociatedWithFileEntity(fileHandleIds, objectId);
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.ENTITY;
	}

	@Override
	public FileHandleAssociationScanner getAssociationScanner() {
		return scanner;
	}
}
