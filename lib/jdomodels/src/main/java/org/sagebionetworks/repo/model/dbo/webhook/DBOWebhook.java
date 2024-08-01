package org.sagebionetworks.repo.model.dbo.webhook;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_INVOKE_ENDPOINT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_IS_ENABLED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_EVENT_TYPES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_MSG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_WEBHOOK;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WEBHOOK;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOWebhook implements MigratableDatabaseObject<DBOWebhook, DBOWebhook> {

	private Long id;
	private String etag;
	private Long createdBy;
	private Timestamp createdOn;
	private Timestamp modifiedOn;
	private Long objectId;
	private String objectType;
	private String eventTypes;
	private String invokeEndpoint;
	private Boolean isEnabled;
	private String verificationStatus;
	private String verificationMessage;
	

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_WEBHOOK_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_WEBHOOK_ETAG).withIsEtag(true),
		new FieldColumn("createdBy", COL_WEBHOOK_CREATED_BY),
		new FieldColumn("createdOn", COL_WEBHOOK_CREATED_ON),
		new FieldColumn("modifiedOn", COL_WEBHOOK_MODIFIED_ON), 
		new FieldColumn("objectId", COL_WEBHOOK_OBJECT_ID),
		new FieldColumn("objectType", COL_WEBHOOK_OBJECT_TYPE),
		new FieldColumn("eventTypes", COL_WEBHOOK_EVENT_TYPES),
		new FieldColumn("invokeEndpoint", COL_WEBHOOK_INVOKE_ENDPOINT),
		new FieldColumn("isEnabled", COL_WEBHOOK_IS_ENABLED),
		new FieldColumn("verificationStatus", COL_WEBHOOK_VERIFICATION_STATUS),
		new FieldColumn("verificationMessage", COL_WEBHOOK_VERIFICATION_MSG)
	};

	@Override
	public TableMapping<DBOWebhook> getTableMapping() {
		return new TableMapping<DBOWebhook>() {
			@Override
			public DBOWebhook mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOWebhook dbo = new DBOWebhook();
				dbo.setId(rs.getLong(COL_WEBHOOK_ID));
				dbo.setEtag(rs.getString(COL_WEBHOOK_ETAG));
				dbo.setCreatedBy(rs.getLong(COL_WEBHOOK_CREATED_BY));
				dbo.setCreatedOn(rs.getTimestamp(COL_WEBHOOK_CREATED_ON));
				dbo.setModifiedOn(rs.getTimestamp(COL_WEBHOOK_MODIFIED_ON));
				dbo.setObjectId(rs.getLong(COL_WEBHOOK_OBJECT_ID));
				dbo.setObjectType(rs.getString(COL_WEBHOOK_OBJECT_TYPE));
				dbo.setEventTypes(rs.getString(COL_WEBHOOK_EVENT_TYPES));
				dbo.setInvokeEndpoint(rs.getString(COL_WEBHOOK_INVOKE_ENDPOINT));
				dbo.setIsEnabled(rs.getBoolean(COL_WEBHOOK_IS_ENABLED));
				dbo.setVerificationStatus(rs.getString(COL_WEBHOOK_VERIFICATION_STATUS));
				dbo.setVerificationMessage(rs.getString(COL_WEBHOOK_VERIFICATION_MSG));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_WEBHOOK;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_WEBHOOK;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOWebhook> getDBOClass() {
				return DBOWebhook.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.WEBHOOK;
	}

	@Override
	public MigratableTableTranslation<DBOWebhook, DBOWebhook> getTranslator() {
		return new BasicMigratableTableTranslation<DBOWebhook>();
	}

	@Override
	public Class<? extends DBOWebhook> getBackupClass() {
		return DBOWebhook.class;
	}

	@Override
	public Class<? extends DBOWebhook> getDatabaseObjectClass() {
		return DBOWebhook.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public DBOWebhook setId(Long id) {
		this.id = id;
		return this;
	}

	public String getEtag() {
		return etag;
	}
	
	public DBOWebhook setEtag(String etag) {
		this.etag = etag;
		return this;
	}
	
	public Long getCreatedBy() {
		return createdBy;
	}
	
	public DBOWebhook setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}
	
	public Timestamp getCreatedOn() {
		return createdOn;
	}
	
	public DBOWebhook setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
		return this;
	}
	
	public Timestamp getModifiedOn() {
		return modifiedOn;
	}
	
	public DBOWebhook setModifiedOn(Timestamp modifiedOn) {
		this.modifiedOn = modifiedOn;
		return this;
	}
	
	public Long getObjectId() {
		return objectId;
	}

	public DBOWebhook setObjectId(Long objectId) {
		this.objectId = objectId;
		return this;
	}

	public String getObjectType() {
		return objectType;
	}

	public DBOWebhook setObjectType(String objectType) {
		this.objectType = objectType;
		return this;
	}
	
	public String getEventTypes() {
		return eventTypes;
	}
	
	public DBOWebhook setEventTypes(String eventTypes) {
		this.eventTypes = eventTypes;
		return this;
	}

	public String getInvokeEndpoint() {
		return invokeEndpoint;
	}

	public DBOWebhook setInvokeEndpoint(String invokeEndpoint) {
		this.invokeEndpoint = invokeEndpoint;
		return this;
	}

	public Boolean getIsEnabled() {
		return isEnabled;
	}
	
	public DBOWebhook setIsEnabled(Boolean isEnabled) {
		this.isEnabled = isEnabled;
		return this;
	}
	
	public String getVerificationStatus() {
		return verificationStatus;
	}
	
	public DBOWebhook setVerificationStatus(String verificationStatus) {
		this.verificationStatus = verificationStatus;
		return this;
	}
	
	public String getVerificationMessage() {
		return verificationMessage;
	}
	
	public DBOWebhook setVerificationMessage(String verificationMessage) {
		this.verificationMessage = verificationMessage;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, createdOn, etag, eventTypes, id, invokeEndpoint, isEnabled, modifiedOn, objectId, objectType,
				verificationMessage, verificationStatus);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOWebhook)) {
			return false;
		}
		DBOWebhook other = (DBOWebhook) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn) && Objects.equals(etag, other.etag)
				&& Objects.equals(eventTypes, other.eventTypes) && Objects.equals(id, other.id)
				&& Objects.equals(invokeEndpoint, other.invokeEndpoint) && Objects.equals(isEnabled, other.isEnabled)
				&& Objects.equals(modifiedOn, other.modifiedOn) && Objects.equals(objectId, other.objectId)
				&& Objects.equals(objectType, other.objectType) && Objects.equals(verificationMessage, other.verificationMessage)
				&& Objects.equals(verificationStatus, other.verificationStatus);
	}

	@Override
	public String toString() {
		return "DBOWebhook [id=" + id + ", etag=" + etag + ", createdBy=" + createdBy + ", createdOn=" + createdOn + ", modifiedOn="
				+ modifiedOn + ", objectId=" + objectId + ", objectType=" + objectType + ", eventTypes=" + eventTypes + ", invokeEndpoint="
				+ invokeEndpoint + ", isEnabled=" + isEnabled + ", verificationStatus=" + verificationStatus + ", verificationMessage="
				+ verificationMessage + "]";
	}

}
