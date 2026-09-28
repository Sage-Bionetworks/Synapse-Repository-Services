package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dbo.search.OpaqueJsonColumnCodecUtil;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class RequestUtils {
	public static String REGEX = ",";
	private static final String SCHEMA_DATA = "Request.schemaData";
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder()
			.allowTypeHierarchy(RequestInterface.class)
			.allowTypesByWildcard(new String[] {"org.sagebionetworks.repo.model.**"})
			.build();


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
		copyToSerializedField(dto, dbo);
	}

	public static RequestInterface copyDboToDto(DBORequest dbo) {
		RequestInterface dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId().toString());
		dto.setAccessRequirementId(dbo.getAccessRequirementId().toString());
		dto.setResearchProjectId(dbo.getResearchProjectId() == null ? null : dbo.getResearchProjectId().toString());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setModifiedBy(dbo.getModifiedBy().toString());
		dto.setModifiedOn(new Date(dbo.getModifiedOn()));
		dto.setEtag(dbo.getEtag());
		return dto;
	}

	public static void copyToSerializedField(RequestInterface dto, DBORequest dbo) throws DatastoreException {
		dbo.setRequestSerialized(writeSerializedField(dto));
	}
	
	public static byte[] writeSerializedField(RequestInterface dto) {
		// schemaData is free-form JSON, so it reaches this layer as whatever shape the wire
		// deserializer produced (a JSONObjectAdapter for an object, a boxed scalar otherwise).
		// None of those types are in the X_STREAM allowlist, and widening the allowlist would
		// bake a third-party field layout into both this blob and the migration backup XML. It
		// is therefore stored as canonical JSON text, and the caller's object is left as it was.
		Object schemaData = dto.getSchemaData();
		dto.setSchemaData(OpaqueJsonColumnCodecUtil.serialize(schemaData, SCHEMA_DATA));
		try {
			return JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto);
		} catch (IOException e) {
			throw new DatastoreException(e);
		} finally {
			dto.setSchemaData(schemaData);
		}
	}

	public static RequestInterface copyFromSerializedField(DBORequest dbo) throws DatastoreException {
		return readSerializedField(dbo.getRequestSerialized());
	}

	public static RequestInterface readSerializedField(byte[] serializedField) {
		try {
			RequestInterface dto = (RequestInterface) JDOSecondaryPropertyUtils.decompressObject(X_STREAM,
					serializedField);
			if (dto.getSchemaData() instanceof String schemaDataJson) {
				dto.setSchemaData(OpaqueJsonColumnCodecUtil.deserialize(schemaDataJson, SCHEMA_DATA));
			}
			return dto;
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
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
