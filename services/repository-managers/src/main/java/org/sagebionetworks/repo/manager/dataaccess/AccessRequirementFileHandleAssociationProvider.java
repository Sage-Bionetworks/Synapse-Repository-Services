package org.sagebionetworks.repo.manager.dataaccess;

import static org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner.DEFAULT_BATCH_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.RowMapperSupplier;
import org.sagebionetworks.repo.manager.file.scanner.SerializedFieldRowMapperSupplier;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirementRevision;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AccessRequirementFileHandleAssociationProvider implements FileHandleAssociationProvider {
		
	static final RowMapperSupplier ROW_MAPPER_SUPPLIER = new SerializedFieldRowMapperSupplier<>(AccessRequirementUtils::readSerializedField, AccessRequirementUtils::extractAllFileHandleIds);
	
	private AccessRequirementDAO accessRequirementDao;
	private FileHandleAssociationScanner scanner;
	
	@Autowired
	public AccessRequirementFileHandleAssociationProvider(AccessRequirementDAO accessRequirementDao, NamedParameterJdbcTemplate jdbcTemplate) {
		this.accessRequirementDao = accessRequirementDao;
		this.scanner = new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOAccessRequirementRevision().getTableMapping(), COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY, DEFAULT_BATCH_SIZE, ROW_MAPPER_SUPPLIER);
	}
	
	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.AccessRequirementAttachment;
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		AccessRequirement accessRequirement = accessRequirementDao.get(objectId);
		Set<String> associatedIds = AccessRequirementUtils.extractAllFileHandleIds(accessRequirement);
		associatedIds.retainAll(fileHandleIds);
		return associatedIds;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.ACCESS_REQUIREMENT;
	}
	
	@Override
	public FileHandleAssociationScanner getAssociationScanner() {
		return scanner;
	}

}
