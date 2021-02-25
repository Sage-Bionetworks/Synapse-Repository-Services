package org.sagebionetworks.repo.manager.dataaccess;

import static org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner.DEFAULT_BATCH_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirementRevision;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class AccessRequirementFileHandleAssociationProvider implements FileHandleAssociationProvider {
	
	private static Logger LOG = LogManager.getLogger(AccessRequirementFileHandleAssociationProvider.class);
	
	static final RowMapper<ScannedFileHandleAssociation> SCANNED_MAPPER = (ResultSet rs, int i) -> {
		
		final String objectId = rs.getString(COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID);
		
		ScannedFileHandleAssociation association = new ScannedFileHandleAssociation(objectId);

		final java.sql.Blob blob = rs.getBlob(COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY);
		
		if (blob == null) {
			return association;
		}
		
		byte[] serializedField = blob.getBytes(1, (int) blob.length());
		
		AccessRequirement ar;
		
		try {
			ar = AccessRequirementUtils.readSerializedField(serializedField);
		} catch (DatastoreException e) {
			LOG.warn(e.getMessage(),  e);
			return association;
		}
		
		if (!(ar instanceof ManagedACTAccessRequirement)) {
			return association;
		}
		
		ManagedACTAccessRequirement managedAr = (ManagedACTAccessRequirement) ar;
		
		if (StringUtils.isBlank(managedAr.getDucTemplateFileHandleId())) {
			return association;
		}
			
		Long fileHandleId;
		
		try {
			 fileHandleId = KeyFactory.stringToKey(managedAr.getDucTemplateFileHandleId());
		} catch (IllegalArgumentException e) {
			LOG.warn(e.getMessage(), e);
			return association;
		}
			
		return association.withFileHandleIds(Collections.singletonList(fileHandleId));
	};
	
	private AccessRequirementDAO accessRequirementDao;
	private FileHandleAssociationScanner scanner;
	
	@Autowired
	public AccessRequirementFileHandleAssociationProvider(AccessRequirementDAO accessRequirementDao, NamedParameterJdbcTemplate jdbcTemplate) {
		this.accessRequirementDao = accessRequirementDao;
		this.scanner = new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOAccessRequirementRevision().getTableMapping(), COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY, DEFAULT_BATCH_SIZE, SCANNED_MAPPER);
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		Set<String> associatedIds = new HashSet<String>();
		AccessRequirement accessRequirement = accessRequirementDao.get(objectId);
		if (accessRequirement instanceof ManagedACTAccessRequirement) {
			ManagedACTAccessRequirement actAR = (ManagedACTAccessRequirement) accessRequirement;
			String ducFileHandleId = actAR.getDucTemplateFileHandleId();
			if (ducFileHandleId != null && fileHandleIds.contains(ducFileHandleId)) {
				associatedIds.add(ducFileHandleId);
			}
		}
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
