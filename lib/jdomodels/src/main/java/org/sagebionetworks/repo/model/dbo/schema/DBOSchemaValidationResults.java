package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_ALL_ERRORS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_ERROR_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_EXCEPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_IS_VALID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_OBJECT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_JSON_SCHEMA_VALIDATION_VALIDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_JSON_SCHEMA_VALIDATION_RESULTS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SCHEMA_VALIDATION_RESULTS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DBOSchemaValidationResults implements DatabaseObject<DBOSchemaValidationResults> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("objectId", COL_JSON_SCHEMA_VALIDATION_OBJECT_ID, true),
			new FieldColumn("objectType", COL_JSON_SCHEMA_VALIDATION_OBJECT_TYPE, true),
			new FieldColumn("objectEtag", COL_JSON_SCHEMA_VALIDATION_OBJECT_ETAG),
			new FieldColumn("schema$id", COL_JSON_SCHEMA_VALIDATION_SCHEMA_ID),
			new FieldColumn("isValid", COL_JSON_SCHEMA_VALIDATION_IS_VALID),
			new FieldColumn("validatedOn", COL_JSON_SCHEMA_VALIDATION_VALIDATED_ON),
			new FieldColumn("errorMessage", COL_JSON_SCHEMA_VALIDATION_ERROR_MESSAGE),
			new FieldColumn("allErrorMessages", COL_JSON_SCHEMA_VALIDATION_ALL_ERRORS),
			new FieldColumn("validationException", COL_JSON_SCHEMA_VALIDATION_EXCEPTION), };

	private Long objectId;
	private String objectType;
	private String objectEtag;
	private String schema$id;
	private Boolean isValid;
	private Timestamp validatedOn;
	private String errorMessage;
	private String allErrorMessages;
	private String validationException;

	TableMapping<DBOSchemaValidationResults> MAPPING = new TableMapping<DBOSchemaValidationResults>() {

		@Override
		public DBOSchemaValidationResults mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOSchemaValidationResults dbo = new DBOSchemaValidationResults();
			dbo.setObjectId(rs.getLong(COL_JSON_SCHEMA_VALIDATION_OBJECT_ID));
			dbo.setObjectType(rs.getString(COL_JSON_SCHEMA_VALIDATION_OBJECT_TYPE));
			dbo.setObjectEtag(rs.getString(COL_JSON_SCHEMA_VALIDATION_OBJECT_ETAG));
			dbo.setSchema$id(rs.getString(COL_JSON_SCHEMA_VALIDATION_SCHEMA_ID));
			dbo.setIsValid(rs.getBoolean(COL_JSON_SCHEMA_VALIDATION_IS_VALID));
			dbo.setValidatedOn(rs.getTimestamp(COL_JSON_SCHEMA_VALIDATION_VALIDATED_ON));
			dbo.setErrorMessage(rs.getString(COL_JSON_SCHEMA_VALIDATION_ERROR_MESSAGE));
			dbo.setAllErrorMessages(rs.getString(COL_JSON_SCHEMA_VALIDATION_ALL_ERRORS));
			dbo.setValidationException(rs.getString(COL_JSON_SCHEMA_VALIDATION_EXCEPTION));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_SCHEMA_VALIDATION_RESULTS;
		}

		@Override
		public String getDDLFileName() {
			return DDL_FILE_JSON_SCHEMA_VALIDATION_RESULTS;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOSchemaValidationResults> getDBOClass() {
			return DBOSchemaValidationResults.class;
		}
	};

	@Override
	public TableMapping<DBOSchemaValidationResults> getTableMapping() {
		return MAPPING;
	}

	/**
	 * @return the objectId
	 */
	public Long getObjectId() {
		return objectId;
	}

	/**
	 * @param objectId the objectId to set
	 */
	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	/**
	 * @return the objectType
	 */
	public String getObjectType() {
		return objectType;
	}

	/**
	 * @param objectType the objectType to set
	 */
	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	/**
	 * @return the objectEtag
	 */
	public String getObjectEtag() {
		return objectEtag;
	}

	/**
	 * @param objectEtag the objectEtag to set
	 */
	public void setObjectEtag(String objectEtag) {
		this.objectEtag = objectEtag;
	}

	/**
	 * @return the schema$id
	 */
	public String getSchema$id() {
		return schema$id;
	}

	/**
	 * @param schema$id the schema$id to set
	 */
	public void setSchema$id(String schema$id) {
		this.schema$id = schema$id;
	}

	/**
	 * @return the isValid
	 */
	public Boolean getIsValid() {
		return isValid;
	}

	/**
	 * @param isValid the isValid to set
	 */
	public void setIsValid(Boolean isValid) {
		this.isValid = isValid;
	}

	/**
	 * @return the validatedOn
	 */
	public Timestamp getValidatedOn() {
		return validatedOn;
	}

	/**
	 * @param validatedOn the validatedOn to set
	 */
	public void setValidatedOn(Timestamp validatedOn) {
		this.validatedOn = validatedOn;
	}

	/**
	 * @return the errorMessage
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @param errorMessage the errorMessage to set
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/**
	 * @return the allErrorMessages
	 */
	public String getAllErrorMessages() {
		return allErrorMessages;
	}

	/**
	 * @param allErrorMessages the allErrorMessages to set
	 */
	public void setAllErrorMessages(String allErrorMessages) {
		this.allErrorMessages = allErrorMessages;
	}

	/**
	 * @return the validationException
	 */
	public String getValidationException() {
		return validationException;
	}

	/**
	 * @param validationException the validationException to set
	 */
	public void setValidationException(String validationException) {
		this.validationException = validationException;
	}

	@Override
	public int hashCode() {
		return Objects.hash(allErrorMessages, errorMessage, isValid, objectEtag, objectId, objectType, schema$id,
				validatedOn, validationException);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOSchemaValidationResults)) {
			return false;
		}
		DBOSchemaValidationResults other = (DBOSchemaValidationResults) obj;
		return Objects.equals(allErrorMessages, other.allErrorMessages)
				&& Objects.equals(errorMessage, other.errorMessage) && Objects.equals(isValid, other.isValid)
				&& Objects.equals(objectEtag, other.objectEtag) && Objects.equals(objectId, other.objectId)
				&& objectType == other.objectType && Objects.equals(schema$id, other.schema$id)
				&& Objects.equals(validatedOn, other.validatedOn)
				&& Objects.equals(validationException, other.validationException);
	}

	@Override
	public String toString() {
		return "DBOSchemaValidationResults [objectId=" + objectId + ", objectType=" + objectType + ", objectEtag="
				+ objectEtag + ", schema$id=" + schema$id + ", isValid=" + isValid + ", validatedOn=" + validatedOn
				+ ", errorMessage=" + errorMessage + ", allErrorMessages=" + allErrorMessages + ", validationException="
				+ validationException + "]";
	}

}
