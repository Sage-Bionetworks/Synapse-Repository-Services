package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.io.IOException;
import java.util.Date;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class DataAccessRequestUtils {
	public static String REGEX = ",";

	public static void copyDtoToDbo(DataAccessRequestInterface dto, DBODataAccessRequest dbo) throws DatastoreException{
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setAccessRequirementId(Long.parseLong(dto.getAccessRequirementId()));
		dbo.setResearchProjectId(Long.parseLong(dto.getResearchProjectId()));
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setModifiedBy(Long.parseLong(dto.getModifiedBy()));
		dbo.setModifiedOn(dto.getModifiedOn().getTime());
		dbo.setEtag(dto.getEtag());
		copyToSerializedField(dto, dbo);
	}

	public static DataAccessRequestInterface copyDboToDto(DBODataAccessRequest dbo) {
		DataAccessRequestInterface dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId().toString());
		dto.setAccessRequirementId(dbo.getAccessRequirementId().toString());
		dto.setResearchProjectId(dbo.getResearchProjectId().toString());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setModifiedBy(dbo.getModifiedBy().toString());
		dto.setModifiedOn(new Date(dbo.getModifiedOn()));
		dto.setEtag(dbo.getEtag());
		return dto;
	}

	public static void copyToSerializedField(DataAccessRequestInterface dto, DBODataAccessRequest dbo) throws DatastoreException {
		try {
			dbo.setRequestSerialized(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	public static DataAccessRequestInterface copyFromSerializedField(DBODataAccessRequest dbo) throws DatastoreException {
		try {
			return (DataAccessRequestInterface)JDOSecondaryPropertyUtils.decompressedObject(dbo.getRequestSerialized());
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
}
