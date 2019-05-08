package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.io.IOException;
import java.util.Date;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class RequestUtils {
	public static String REGEX = ",";
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypeHierarchy(RequestInterface.class).build();


	public static void copyDtoToDbo(RequestInterface dto, DBORequest dbo) throws DatastoreException{
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

	public static RequestInterface copyDboToDto(DBORequest dbo) {
		RequestInterface dto = copyFromSerializedField(dbo);
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

	public static void copyToSerializedField(RequestInterface dto, DBORequest dbo) throws DatastoreException {
		try {
			dbo.setRequestSerialized(JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	public static RequestInterface copyFromSerializedField(DBORequest dbo) throws DatastoreException {
		try {
			return (RequestInterface)JDOSecondaryPropertyUtils.decompressObject(X_STREAM, dbo.getRequestSerialized());
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
}
