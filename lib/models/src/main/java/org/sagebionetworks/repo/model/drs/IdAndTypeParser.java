package org.sagebionetworks.repo.model.drs;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Immutable representation of a DRS Object ID
 */
public class IdAndTypeParser {
	public enum Type {
		FILE_ENTITY,
		FILE_HANDLE;
	}

	private static final String ENTITY_ID_PREFIX = "syn";
	private static final String FILE_HANDLE_ID_PREFIX = "fh";

	private Type type;
	private IdAndVersion idAndVersion;

	public IdAndTypeParser(Type type, IdAndVersion idAndVersion) {
		this.type = type;
		this.idAndVersion = idAndVersion;
	}

	public static Builder Builder() {
		return new Builder();
	}

	public Type getType() {
		return type;
	}

	public IdAndVersion getIdAndVersion() {
		return idAndVersion;
	}

	public static class Builder {

		private String objectId;

		public Builder setObjectId(final String objectId) {
			this.objectId = objectId;
			return this;
		}

		public IdAndTypeParser build() {
			ValidateArgument.required(objectId, "objectId");
			return parseIdAndVersion(objectId);
		}

		public IdAndTypeParser parseIdAndVersion(String objectId) {
			String strippedObjectId = StringUtils.strip(objectId);

			if (StringUtils.startsWithIgnoreCase(strippedObjectId, FILE_HANDLE_ID_PREFIX)) {
				IdAndVersion idAndVersion = parseFileHandleId(strippedObjectId);
				return new IdAndTypeParser(Type.FILE_HANDLE, idAndVersion);
			} else if (StringUtils.startsWithIgnoreCase(strippedObjectId, ENTITY_ID_PREFIX)) {
				IdAndVersion idAndVersion = IdAndVersion.parse(strippedObjectId);
				validateIdHasVersion(idAndVersion);
				return new IdAndTypeParser(Type.FILE_ENTITY, idAndVersion);
			} else {
				throw new IllegalArgumentException("Invalid Object ID: " + objectId);
			}
		}

		static IdAndVersion parseFileHandleId(String objectId) {
			try {
				return IdAndVersion.parse(objectId.substring(2));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Invalid Object ID: " + objectId, e);
			}
		}

		public static void validateIdHasVersion(final IdAndVersion idAndVersion) {
			if (!idAndVersion.getVersion().isPresent()) {
				throw new IllegalArgumentException("Object id should include version. e.g syn123.1");
			}
		}
	}
}
