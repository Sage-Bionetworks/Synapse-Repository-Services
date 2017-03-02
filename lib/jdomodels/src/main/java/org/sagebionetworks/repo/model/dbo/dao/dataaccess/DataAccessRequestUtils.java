package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
		dbo.setAccessors(convertListToString(dto.getAccessors()));
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
		dto.setAccessors(convertStringToList(dbo.getAccessors()));
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

	public static String convertListToString(List<String> accessors) {
		String result = "";
		if (accessors == null || accessors.isEmpty()) {
			return result;
		}
		result = accessors.get(0);
		for (int i = 1; i < accessors.size(); i++) {
			result += ","+accessors.get(i);
		}
		return result;
	}

	public static List<String> convertStringToList(String accessorsString) {
		List<String> result = new ArrayList<String>();
		if (accessorsString == null || accessorsString.equals("")) {
			return result;
		}
		String[] accessors = accessorsString.split(REGEX);
		for (int i = 0; i < accessors.length; i++) {
			result.add(accessors[i]);
		}
		return result;
	}
}
