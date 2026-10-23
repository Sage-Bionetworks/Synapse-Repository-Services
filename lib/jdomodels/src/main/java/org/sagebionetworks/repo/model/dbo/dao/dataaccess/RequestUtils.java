package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class RequestUtils {
	public static String REGEX = ",";

	public static void copyDtoToDbo(RequestInterface dto, DBORequest dbo) throws DatastoreException{
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setAccessRequirementId(Long.parseLong(dto.getAccessRequirementId()));
		dbo.setResearchProjectId(toLong(dto.getResearchProjectId()));
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setModifiedBy(Long.parseLong(dto.getModifiedBy()));
		dbo.setModifiedOn(dto.getModifiedOn().getTime());
		dbo.setEtag(dto.getEtag());
		dbo.setEDucEnvelopeId(dto.getEDucSignatureEnvelopeId());
		dbo.setDucFileHandleId(toLong(dto.getDucFileHandleId()));
		dbo.setIrbFileHandleId(toLong(dto.getIrbFileHandleId()));
		dbo.setAttachmentsJson(JDOSecondaryPropertyUtils.writeStringListToJson(dto.getAttachments()));
		dbo.setRequestJson(createRequestJson(dto));
	}

	/**
	 * @return The JSON document of the request: every field of the request except the ones stored in their own column
	 */
	static String createRequestJson(RequestInterface dto) {
		// Cleared on a copy so that the caller's object is left untouched and each value is stored once
		RequestInterface document = JDOSecondaryPropertyUtils.createObjectFromJSON(dto.getClass(),
				JDOSecondaryPropertyUtils.createJSONFromObject(dto));
		document.setId(null);
		document.setAccessRequirementId(null);
		document.setResearchProjectId(null);
		document.setCreatedBy(null);
		document.setCreatedOn(null);
		document.setModifiedBy(null);
		document.setModifiedOn(null);
		document.setEtag(null);
		document.setEDucSignatureEnvelopeId(null);
		document.setDucFileHandleId(null);
		document.setIrbFileHandleId(null);
		document.setAttachments(null);
		// The concrete type selects the implementation when the document is read back
		document.setConcreteType(dto.getClass().getName());
		return JDOSecondaryPropertyUtils.createJSONFromObject(document);
	}

	public static RequestInterface copyDboToDto(DBORequest dbo) {
		RequestInterface dto = JDOSecondaryPropertyUtils.createObjectFromJSON(RequestInterface.class, dbo.getRequestJson());
		dto.setId(dbo.getId().toString());
		dto.setAccessRequirementId(dbo.getAccessRequirementId().toString());
		dto.setResearchProjectId(toString(dbo.getResearchProjectId()));
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setModifiedBy(dbo.getModifiedBy().toString());
		dto.setModifiedOn(new Date(dbo.getModifiedOn()));
		dto.setEtag(dbo.getEtag());
		dto.setEDucSignatureEnvelopeId(dbo.getEDucEnvelopeId());
		dto.setDucFileHandleId(toString(dbo.getDucFileHandleId()));
		dto.setIrbFileHandleId(toString(dbo.getIrbFileHandleId()));
		dto.setAttachments(JDOSecondaryPropertyUtils.readJsonToStringList(dbo.getAttachmentsJson()));
		return dto;
	}

	private static Long toLong(String value) {
		return value == null ? null : Long.valueOf(value);
	}

	private static String toString(Long value) {
		return value == null ? null : value.toString();
	}

	public static Set<String> extractAllFileHandleIds(RequestInterface request) {
		Set<String> fileHandleIds = new HashSet<String>();
		if (request.getAttachments()!= null && !request.getAttachments().isEmpty()) {
			fileHandleIds.addAll(request.getAttachments());
		}
		if (request.getDucFileHandleId() != null) {
			fileHandleIds.add(request.getDucFileHandleId());
		}
		if (request.getIrbFileHandleId() != null) {
			fileHandleIds.add(request.getIrbFileHandleId());
		}
		return fileHandleIds;
	}
}
