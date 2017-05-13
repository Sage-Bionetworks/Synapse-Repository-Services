package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirementRevision;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSubjectAccessRequirement;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.ValidateArgument;

public class AccessRequirementUtils {

	// the convention is that the individual fields take precedence
	// over the serialized objects.  When restoring the dto we first deserialize
	// the 'blob' and then populate the individual fields

	// TODO: an AccessRequirement will be saved to DBOAccessRequirement & DBOAccessRequirementRevision
	public static void copyDtoToDbo(AccessRequirement dto, DBOAccessRequirement dbo) throws DatastoreException {
		dbo.setId(dto.getId());
		dbo.seteTag(dto.getEtag());
		if (dto.getCreatedBy()!=null) dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		if (dto.getCreatedOn()!=null) dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setModifiedBy(Long.parseLong(dto.getModifiedBy()));
		dbo.setModifiedOn(dto.getModifiedOn().getTime());
		dbo.setAccessType(dto.getAccessType().name());
		dbo.setConcreteType(dto.getConcreteType());
		dbo.setCurrentRevNumber(dto.getVersionNumber());
		copyToSerializedField(dto, dbo);
	}

	// TODO: this method will be changed: an AccessRequirement needs to be build from both DBOAccessRequirement & DBOAccessRequirementRevision
	public static AccessRequirement copyDboToDto(DBOAccessRequirement dbo, List<RestrictableObjectDescriptor> subjectIds) throws DatastoreException {
		AccessRequirement dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId());
		dto.setEtag(dbo.geteTag());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setModifiedBy(dbo.getModifiedBy().toString());
		dto.setModifiedOn(new Date(dbo.getModifiedOn()));
		dto.setSubjectIds(subjectIds);
		dto.setAccessType(ACCESS_TYPE.valueOf(dbo.getAccessType()));
		dto.setVersionNumber(dbo.getCurrentRevNumber());
		return dto;
	}

	// TODO: this method should be removed. Only DBOAccessRequirementRevision should store the blob
	public static void copyToSerializedField(AccessRequirement dto, DBOAccessRequirement dbo) throws DatastoreException {
		try {
			dbo.setSerializedEntity(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	// TODO: this method should be changed to take DBOAccessRequirementRevision
	public static AccessRequirement copyFromSerializedField(DBOAccessRequirement dbo) throws DatastoreException {
		try {
			return (AccessRequirement)JDOSecondaryPropertyUtils.decompressedObject(dbo.getSerializedEntity());
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	// TODO: this method should be combined with copyDtoToDbo()
	public static void copyDTOToDBOAccessRequirementRevision(AccessRequirement dto, DBOAccessRequirementRevision dbo, Long version) {
		dbo.setOwnerId(dto.getId());
		dbo.setModifiedBy(Long.parseLong(dto.getModifiedBy()));
		dbo.setModifiedOn(dto.getModifiedOn().getTime());
		dbo.setAccessType(dto.getAccessType().name());
		dbo.setConcreteType(dto.getConcreteType());
		dbo.setNumber(version);
		copyToSerializedField(dto, dbo);
	}

	private static void copyToSerializedField(AccessRequirement dto, DBOAccessRequirementRevision dbo) {
		try {
			dbo.setSerializedEntity(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	public static List<DBOSubjectAccessRequirement> createBatchDBOSubjectAccessRequirement(Long accessRequirementId, List<RestrictableObjectDescriptor> rodList) {
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(rodList, "rodList");
		List<DBOSubjectAccessRequirement> batch = new ArrayList<DBOSubjectAccessRequirement>();
		for (RestrictableObjectDescriptor rod: new HashSet<RestrictableObjectDescriptor>(rodList)) {
			DBOSubjectAccessRequirement nar = new DBOSubjectAccessRequirement();
			nar.setAccessRequirementId(accessRequirementId);
			nar.setSubjectId(KeyFactory.stringToKey(rod.getId()));
			nar.setSubjectType(rod.getType().toString());
			batch.add(nar);
		}
		return batch;
	}

	public static List<RestrictableObjectDescriptor> copyDBOSubjectsToDTOSubjects(
			List<DBOSubjectAccessRequirement> bdos) {
		ValidateArgument.required(bdos, "bdos");
		List<RestrictableObjectDescriptor> rodList = new ArrayList<RestrictableObjectDescriptor>();	
		for (DBOSubjectAccessRequirement dbo: bdos) {
			RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
			subjectId.setType(RestrictableObjectType.valueOf(dbo.getSubjectType()));
			if (RestrictableObjectType.ENTITY==subjectId.getType()) {
				subjectId.setId(KeyFactory.keyToString(dbo.getSubjectId()));
			} else {
				subjectId.setId(dbo.getSubjectId().toString());
			}
			rodList.add(subjectId);
		}
		return rodList;
	}
}
