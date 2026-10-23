package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.PrincipalInvestigator;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.SigningOfficial;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class RequestUtils {
	public static String REGEX = ",";

	public static void copyDtoToDbo(RequestInterface dto, DBORequest dbo) throws DatastoreException{
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setAccessRequirementId(Long.parseLong(dto.getAccessRequirementId()));
		dbo.setResearchProjectId(dto.getResearchProjectId() == null ? null : Long.parseLong(dto.getResearchProjectId()));
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setModifiedBy(Long.parseLong(dto.getModifiedBy()));
		dbo.setModifiedOn(dto.getModifiedOn().getTime());
		dbo.setEtag(dto.getEtag());
		dbo.setEDucEnvelopeId(dto.getEDucSignatureEnvelopeId());
		copyContentToDbo(dto, dbo);
	}

	/**
	 * Copies the content of the request (everything other than the identity, ownership, etag and eDUC envelope columns) into the
	 * dbo.
	 */
	static void copyContentToDbo(RequestInterface dto, DBORequest dbo) {
		dbo.setConcreteType(dto.getClass().getName());
		dbo.setAccessRequirementVersionNumber(dto.getAccessRequirementVersionNumber());
		dbo.setDucFileHandleId(dto.getDucFileHandleId() == null ? null : Long.parseLong(dto.getDucFileHandleId()));
		dbo.setIrbFileHandleId(dto.getIrbFileHandleId() == null ? null : Long.parseLong(dto.getIrbFileHandleId()));
		dbo.setAttachmentsJson(JDOSecondaryPropertyUtils.writeStringListToJson(dto.getAttachments()));
		dbo.setAccessorChangesJson(JDOSecondaryPropertyUtils.writeEntityListToJson(dto.getAccessorChanges()));
		dbo.setInstitution(dto.getInstitution());
		dbo.setPrincipalInvestigatorJson(JDOSecondaryPropertyUtils.createJSONFromObject(dto.getPrincipalInvestigator()));
		dbo.setSigningOfficialJson(JDOSecondaryPropertyUtils.createJSONFromObject(dto.getSigningOfficial()));
		dbo.setSchemaDataJson(dto.getSchemaData() == null ? null : new JSONObject(dto.getSchemaData().toString()).toString());
		dbo.setSchemaDataAttachmentsJson(JDOSecondaryPropertyUtils.writeStringListToJson(dto.getSchemaDataAttachments()));
		if (dto instanceof Renewal renewal) {
			dbo.setPublication(renewal.getPublication());
			dbo.setSummaryOfUse(renewal.getSummaryOfUse());
		} else {
			dbo.setPublication(null);
			dbo.setSummaryOfUse(null);
		}
	}

	public static RequestInterface copyDboToDto(DBORequest dbo) {
		RequestInterface dto = newRequest(dbo.getConcreteType());
		dto.setId(dbo.getId().toString());
		dto.setAccessRequirementId(dbo.getAccessRequirementId().toString());
		dto.setAccessRequirementVersionNumber(dbo.getAccessRequirementVersionNumber());
		dto.setResearchProjectId(dbo.getResearchProjectId() == null ? null : dbo.getResearchProjectId().toString());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setModifiedBy(dbo.getModifiedBy().toString());
		dto.setModifiedOn(new Date(dbo.getModifiedOn()));
		dto.setEtag(dbo.getEtag());
		dto.setEDucSignatureEnvelopeId(dbo.getEDucEnvelopeId());
		dto.setDucFileHandleId(dbo.getDucFileHandleId() == null ? null : dbo.getDucFileHandleId().toString());
		dto.setIrbFileHandleId(dbo.getIrbFileHandleId() == null ? null : dbo.getIrbFileHandleId().toString());
		dto.setAttachments(JDOSecondaryPropertyUtils.readJsonToStringList(dbo.getAttachmentsJson()));
		dto.setAccessorChanges(JDOSecondaryPropertyUtils.readJsonToEntityList(dbo.getAccessorChangesJson(), AccessorChange.class));
		dto.setInstitution(dbo.getInstitution());
		dto.setPrincipalInvestigator(
				JDOSecondaryPropertyUtils.createObjectFromJSON(PrincipalInvestigator.class, dbo.getPrincipalInvestigatorJson()));
		dto.setSigningOfficial(JDOSecondaryPropertyUtils.createObjectFromJSON(SigningOfficial.class, dbo.getSigningOfficialJson()));
		dto.setSchemaData(dbo.getSchemaDataJson() == null ? null : new JSONObject(dbo.getSchemaDataJson()));
		dto.setSchemaDataAttachments(JDOSecondaryPropertyUtils.readJsonToStringList(dbo.getSchemaDataAttachmentsJson()));
		if (dto instanceof Renewal renewal) {
			renewal.setPublication(dbo.getPublication());
			renewal.setSummaryOfUse(dbo.getSummaryOfUse());
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
		throw new IllegalStateException("Unsupported request concrete type: " + concreteType);
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
