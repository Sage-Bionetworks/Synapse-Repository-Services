package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.internal.RequestContent;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class RequestUtils {

	/**
	 * @return The row of the given request. The content hash is not part of the request and is not set.
	 */
	public static DBORequest toDbo(RequestInterface dto) {
		DBORequest dbo = new DBORequest();
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
		dbo.setSchemaDataAttachmentsJson(JDOSecondaryPropertyUtils.writeStringListToJson(dto.getSchemaDataAttachments()));
		dbo.setRequestJson(JDOSecondaryPropertyUtils.createJSONFromObject(toContent(dto)));
		return dbo;
	}

	/**
	 * @return The fields of the given request that have no column of their own
	 */
	static RequestContent toContent(RequestInterface dto) {
		RequestContent content = new RequestContent()
				.setConcreteType(dto.getClass().getName())
				.setAccessRequirementVersionNumber(dto.getAccessRequirementVersionNumber())
				.setInstitution(dto.getInstitution())
				.setPrincipalInvestigator(dto.getPrincipalInvestigator())
				.setSigningOfficial(dto.getSigningOfficial())
				.setAccessorChanges(dto.getAccessorChanges())
				.setSchemaData(dto.getSchemaData());
		if (dto instanceof Renewal renewal) {
			content.setPublication(renewal.getPublication());
			content.setSummaryOfUse(renewal.getSummaryOfUse());
		}
		return content;
	}

	/**
	 * @return The request stored in the given row, its concrete type is the one recorded in the document
	 */
	public static RequestInterface toDto(DBORequest dbo) {
		RequestContent content = JDOSecondaryPropertyUtils.createObjectFromJSON(RequestContent.class, dbo.getRequestJson());
		RequestInterface dto = newRequest(content.getConcreteType());
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
		dto.setSchemaDataAttachments(JDOSecondaryPropertyUtils.readJsonToStringList(dbo.getSchemaDataAttachmentsJson()));
		dto.setAccessRequirementVersionNumber(content.getAccessRequirementVersionNumber());
		dto.setInstitution(content.getInstitution());
		dto.setPrincipalInvestigator(content.getPrincipalInvestigator());
		dto.setSigningOfficial(content.getSigningOfficial());
		dto.setAccessorChanges(content.getAccessorChanges());
		dto.setSchemaData(content.getSchemaData());
		if (dto instanceof Renewal renewal) {
			renewal.setPublication(content.getPublication());
			renewal.setSummaryOfUse(content.getSummaryOfUse());
		}
		return dto;
	}

	private static RequestInterface newRequest(String concreteType) {
		if (Request.class.getName().equals(concreteType)) {
			return new Request();
		}
		if (Renewal.class.getName().equals(concreteType)) {
			return new Renewal();
		}
		throw new IllegalArgumentException(String.format("Unknown concreteType : '%s'", concreteType));
	}

	static Long toLong(String value) {
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
		if (request.getSchemaDataAttachments() != null) {
			fileHandleIds.addAll(request.getSchemaDataAttachments());
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
