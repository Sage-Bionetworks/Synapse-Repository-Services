package org.sagebionetworks.repo.model.drs;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Immutable representation of a DRS Object ID
 */
public class DrsObjectId {
	public enum Type {
		ENTITY,
		FILE_HANDLE;
	}

	private static final String ENTITY_ID_PREFIX = "syn";
	private static final String FILE_HANDLE_ID_PREFIX = "fh";

	private Type type;
	private Long fileHandleId;
	private IdAndVersion idAndVersion;


	public DrsObjectId(Type type, IdAndVersion idAndVersion) {
		this.type = type;
		this.idAndVersion = idAndVersion;
	}

	public DrsObjectId(Type type, Long fileHandleId) {
		this.type = type;
		this.fileHandleId = fileHandleId;
	}

	public Type getType() {
		return type;
	}

	public IdAndVersion getEntityId() {
		if (!Type.ENTITY.equals(type)) {
			throw new UnsupportedOperationException("IdAndVersion can only be accessed for type ENTITY");
		}
		return idAndVersion;
	}

	public Long getFileHandleId() {
		if (!Type.FILE_HANDLE.equals(type)) {
			throw new UnsupportedOperationException("File handle ID can only be accessed for type FILE_HANDLE");
		}

		return fileHandleId;
	}

	public static DrsObjectId parse(String objectId) {
		ValidateArgument.required(objectId, "objectId");

		String strippedObjectId = StringUtils.strip(objectId);

		if (StringUtils.startsWithIgnoreCase(strippedObjectId, FILE_HANDLE_ID_PREFIX)) {
			return new DrsObjectId(Type.FILE_HANDLE, parseFileHandleId(strippedObjectId));
		} else if (StringUtils.startsWithIgnoreCase(strippedObjectId, ENTITY_ID_PREFIX)) {
			IdAndVersion idAndVersion = IdAndVersion.parse(strippedObjectId);
			validateIdHasVersion(idAndVersion);
			return new DrsObjectId(Type.ENTITY, idAndVersion);
		} else {
			throw new IllegalArgumentException("Object Id must be entity ID with version (e.g syn32132536.1), or the file handle ID prepended with the string \"fh\" (e.g. fh123)");
		}
	}

	static Long parseFileHandleId(String objectId) {
		try {
			return Long.parseLong(objectId.substring(2));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("File Handle ID must contain prefix \"fh\" followed by a Long", e);
		}
	}

	public static void validateIdHasVersion(final IdAndVersion idAndVersion) {
		if (!idAndVersion.getVersion().isPresent()) {
			throw new IllegalArgumentException("Entity ID must include version. e.g syn123.1");
		}
	}
}
