package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.Timestamp;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBODoi;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

public class DoiUtils {

	/**
	 * Converts a DOI DBO into a Doi DTO
	 * @param dbo A DOI database object
	 * @return A DOI data transfer object with all fields filled in.
	 */
	@Deprecated
	public static Doi convertToDto(DBODoi dbo) {
		if (dbo == null) {
			throw new IllegalArgumentException("DBO cannot be null.");
		}
		Doi dto = new Doi();
		dto.setId(dbo.getId().toString());
		dto.setEtag(dbo.getETag());
		dto.setDoiStatus(DoiStatus.valueOf(dbo.getDoiStatus()));
		final ObjectType objectType = ObjectType.valueOf(dbo.getObjectType());
		if (ObjectType.ENTITY.equals(objectType)) {
			dto.setObjectId(KeyFactory.keyToString(dbo.getObjectId()));
		} else {
			dto.setObjectId(dbo.getObjectId().toString());
		}
		dto.setObjectType(objectType);
		if (dbo.getObjectVersion().equals(DBODoi.NULL_OBJECT_VERSION)) {
			dto.setObjectVersion(null);
		} else {
			dto.setObjectVersion(dbo.getObjectVersion());
		}
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(dbo.getCreatedOn());
		dto.setUpdatedOn(dbo.getUpdatedOn());
		return dto;
	}

	/**
	 * Converts a DOI DTO into a DOI DBO
	 * @param dto A DOI data transfer object to convert to a DBO
	 * @return A corresponding DOI database object.
	 */
	@Deprecated
	public static DBODoi convertToDbo(Doi dto) {
		if (dto == null) {
			throw new IllegalArgumentException("DTO cannot be null.");
		}
		if (dto.getId() == null) {
			throw new IllegalArgumentException("ID cannot be null.");
		}
		if (dto.getCreatedBy() == null) {
			throw new IllegalArgumentException("Created By cannot be null.");
		}
		if (dto.getEtag() == null) {
			throw new IllegalArgumentException("Etag cannot be null.");
		}
		if (dto.getDoiStatus() == null) {
			throw new IllegalArgumentException("Status cannot be null.");
		}
		if (dto.getObjectId() == null) {
			throw new IllegalArgumentException("Object ID cannot be null.");
		}
		if (dto.getObjectType() == null) {
			throw new IllegalArgumentException("Object type cannot be null.");
		}
		if (dto.getCreatedOn() == null) {
			throw new IllegalArgumentException("Created On cannot be null.");
		}
		if (dto.getUpdatedOn() == null) {
			throw new IllegalArgumentException("Updated On cannot be null.");
		}

		DBODoi dbo = new DBODoi();
		dbo.setId(Long.valueOf(dto.getId()));
		dbo.setETag(dto.getEtag());
		dbo.setDoiStatus(dto.getDoiStatus());
		dbo.setObjectId(KeyFactory.stringToKey(dto.getObjectId()));
		dbo.setObjectType(dto.getObjectType());
		if (dto.getObjectVersion() == null) {
			dbo.setObjectVersion(DBODoi.NULL_OBJECT_VERSION);
		} else {
			dbo.setObjectVersion(dto.getObjectVersion());
		}
		dbo.setCreatedBy(Long.valueOf(dto.getCreatedBy()));
		dbo.setCreatedOn(new Timestamp(dto.getCreatedOn().getTime()));
		dbo.setUpdatedBy(Long.valueOf(dto.getCreatedBy()));
		dbo.setUpdatedOn(new Timestamp(dto.getUpdatedOn().getTime()));
		return dbo;
	}


	/**
	 * Converts a DOI DBO into a DOI Asscociation DTO, the DTO for the DOI V2 iteration
	 * @param dbo A DOI database object
	 * @return A DoiAssociation data transfer object with all fields filled in.
	 */
	public static DoiAssociation convertToDtoV2(DBODoi dbo) {
		if (dbo == null) {
			throw new IllegalArgumentException("DBO cannot be null.");
		}
		DoiAssociation dto = new DoiAssociation();
		dto.setAssociationId(dbo.getId().toString());
		dto.setEtag(dbo.getETag());
		final ObjectType objectType = ObjectType.valueOf(dbo.getObjectType());
		if (ObjectType.ENTITY.equals(objectType)) {
			dto.setObjectId(KeyFactory.keyToString(dbo.getObjectId()));
		} else {
			dto.setObjectId(dbo.getObjectId().toString());
		}
		dto.setObjectType(objectType);
		if (dbo.getObjectVersion().equals(DBODoi.NULL_OBJECT_VERSION)) {
			dto.setObjectVersion(null);
		} else {
			dto.setObjectVersion(dbo.getObjectVersion());
		}
		dto.setAssociatedBy(dbo.getCreatedBy().toString());
		dto.setAssociatedOn(dbo.getCreatedOn());
		dto.setUpdatedBy(dbo.getUpdatedBy().toString());
		dto.setUpdatedOn(dbo.getUpdatedOn());
		return dto;
	}

	/**
	 * Converts a DOI Association DTO into a DOI DBO
	 * @param dto A DOI data transfer object to convert to a DBO
	 * @return A corresponding DOI database object.
	 */
	public static DBODoi convertToDbo(DoiAssociation dto) {
		if (dto == null) {
			throw new IllegalArgumentException("DTO cannot be null.");
		}
		if (dto.getAssociationId() == null) {
			throw new IllegalArgumentException("ID cannot be null.");
		}
		if (dto.getAssociatedBy() == null) {
			throw new IllegalArgumentException("Associated By cannot be null.");
		}
		if (dto.getUpdatedBy() == null) {
			throw new IllegalArgumentException("Updated By cannot be null.");
		}
		if (dto.getEtag() == null) {
			throw new IllegalArgumentException("Etag cannot be null.");
		}
		if (dto.getObjectId() == null) {
			throw new IllegalArgumentException("Object ID cannot be null.");
		}
		if (dto.getObjectType() == null) {
			throw new IllegalArgumentException("Object type cannot be null.");
		}
		if (dto.getAssociatedOn() == null) {
			throw new IllegalArgumentException("Associated On cannot be null.");
		}
		if (dto.getUpdatedOn() == null) {
			throw new IllegalArgumentException("Updated On cannot be null.");
		}

		DBODoi dbo = new DBODoi();
		dbo.setId(Long.valueOf(dto.getAssociationId()));
		dbo.setETag(dto.getEtag());
		// By convention, a DOI DBO should not be created from a v2 DTO unless the DOI is "Ready"
		dbo.setDoiStatus(DoiStatus.READY);
		dbo.setObjectId(KeyFactory.stringToKey(dto.getObjectId()));
		dbo.setObjectType(dto.getObjectType());
		if (dto.getObjectVersion() == null) {
			dbo.setObjectVersion(DBODoi.NULL_OBJECT_VERSION);
		} else {
			dbo.setObjectVersion(dto.getObjectVersion());
		}
		dbo.setCreatedBy(Long.valueOf(dto.getAssociatedBy()));
		dbo.setCreatedOn(new Timestamp(dto.getAssociatedOn().getTime()));
		dbo.setUpdatedBy(Long.valueOf(dto.getUpdatedBy()));
		dbo.setUpdatedOn(new Timestamp(dto.getUpdatedOn().getTime()));
		return dbo;
	}
}
