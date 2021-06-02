package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirementRevision;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSubjectAccessRequirement;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.ValidateArgument;

public class AccessRequirementUtils {
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(AccessRequirement.class).build();

	// the convention is that the individual fields take precedence
	// over the serialized objects.  When restoring the dto we first deserialize
	// the 'blob' and then populate the individual fields


	public static void copyDtoToDbo(AccessRequirement dto, DBOAccessRequirement dboRequirement, DBOAccessRequirementRevision dboRevision) throws DatastoreException {
		validateFields(dto);
		dto.setSubjectIds(getUniqueRestrictableObjectDescriptor(dto.getSubjectIds()));
		// requirement
		dboRequirement.setId(dto.getId());
		dboRequirement.seteTag(dto.getEtag());
		dboRequirement.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dboRequirement.setCreatedOn(dto.getCreatedOn().getTime());
		dboRequirement.setAccessType(dto.getAccessType().name());
		dboRequirement.setConcreteType(dto.getConcreteType());
		dboRequirement.setCurrentRevNumber(dto.getVersionNumber());

		// revision
		dboRevision.setOwnerId(dto.getId());
		dboRevision.setModifiedBy(Long.parseLong(dto.getModifiedBy()));
		dboRevision.setModifiedOn(dto.getModifiedOn().getTime());
		dboRevision.setNumber(dto.getVersionNumber());
		copyToSerializedField(dto, dboRevision);
	}
	
	/**
	 * Get the unique RestrictableObjectDescriptor from the passed set.
	 * 
	 * @param input
	 * @return
	 */
	public static List<RestrictableObjectDescriptor> getUniqueRestrictableObjectDescriptor(List<RestrictableObjectDescriptor> input){
		if(input == null){
			return null;
		}
		LinkedHashSet<RestrictableObjectDescriptor> set = new LinkedHashSet<RestrictableObjectDescriptor>(input);
		return new LinkedList<RestrictableObjectDescriptor>(set);
	}

	public static AccessRequirement copyDboToDto(DBOAccessRequirement dbo, DBOAccessRequirementRevision revision) throws DatastoreException {
		AccessRequirement dto = copyFromSerializedField(revision);
		if(dto.getSubjectIds() == null){
			dto.setSubjectIds(new LinkedList<RestrictableObjectDescriptor>());
		}
		dto.setId(dbo.getId());
		dto.setEtag(dbo.geteTag());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setModifiedBy(revision.getModifiedBy().toString());
		dto.setModifiedOn(new Date(revision.getModifiedOn()));
		dto.setAccessType(ACCESS_TYPE.valueOf(dbo.getAccessType()));
		dto.setVersionNumber(dbo.getCurrentRevNumber());
		return dto;
	}
	

	public static AccessRequirement copyFromSerializedField(DBOAccessRequirementRevision dbo) throws DatastoreException {
		return readSerializedField(dbo.getSerializedEntity());
	}
	
	public static AccessRequirement readSerializedField(byte[] serializedField) {
		try {
			return (AccessRequirement)JDOSecondaryPropertyUtils.decompressObject(X_STREAM, serializedField);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	public static void copyToSerializedField(AccessRequirement dto, DBOAccessRequirementRevision dbo) {
		dbo.setSerializedEntity(writeSerializedField(dto));
	}
	
	public static byte[] writeSerializedField(AccessRequirement dto) {
		try {
			return JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto);
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
			List<DBOSubjectAccessRequirement> dbos) {
		ValidateArgument.required(dbos, "dbos");
		List<RestrictableObjectDescriptor> rodList = new ArrayList<RestrictableObjectDescriptor>();	
		for (DBOSubjectAccessRequirement dbo: dbos) {
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
	
	/**
	 * Validate the required fields.
	 * @param dto
	 */
	public static void validateFields(AccessRequirement dto){
		ValidateArgument.required(dto, "AccessRequirement");
		ValidateArgument.required(dto.getAccessType(), "dto.accessType");
		ValidateArgument.required(dto.getConcreteType(), "dto.concreteType");
		ValidateArgument.requirement(dto.getClass().getName().equals(dto.getConcreteType()), "Unexpected concreteType: "+dto.getConcreteType()+" expected: "+dto.getClass().getName());
		ValidateArgument.required(dto.getCreatedBy(), "dto.createBy");
		ValidateArgument.required(dto.getCreatedOn(), "dto.createOn");
		ValidateArgument.required(dto.getEtag(), "dto.etag");
		ValidateArgument.required(dto.getId(), "dto.id");
		ValidateArgument.required(dto.getModifiedBy(), "dto.modifiedBy");
		ValidateArgument.required(dto.getModifiedOn(), "dto.modifiedOn");
		ValidateArgument.required(dto.getVersionNumber(), "dto.versionNumber");
	}
	
	/**
	 * @param accessRequirement
	 * @return The set of file handle ids assigned to the given access requirement
	 */
	public static Set<String> extractAllFileHandleIds(AccessRequirement accessRequirement) {
		if (accessRequirement instanceof ManagedACTAccessRequirement) {
			ManagedACTAccessRequirement actAR = (ManagedACTAccessRequirement) accessRequirement;
			String ducFileHandleId = actAR.getDucTemplateFileHandleId();
			if (ducFileHandleId != null) {
				return Collections.singleton(ducFileHandleId);
			}
		}		
		return Collections.emptySet();
	}
}
