package org.sagebionetworks.repo.model.dbo.webhook;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_INVOKE_ENDPOINT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_IS_AUTHENTICATION_ENABLED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_IS_WEBHOOK_ENABLED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_USER_ID;
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
	private Long objectId;
	private String objectType;
	private Long userId;
	private String invokeEndpoint;
	private boolean isWebhookEnabled;
	private boolean isAuthenticationEnabled;
	private String etag;
	private Timestamp createdOn;
	private Timestamp modifiedOn;
	private Long createdBy;
	private Long modifiedBy;

	public static final String FIELD_COLUMN_ID = "id";
	public static final String FIELD_COLUMN_ETAG = "etag";
	public static final String FIELD_COLUMN_OBJECT_ID = "objectId";
	public static final String FIELD_COLUMN_OBJECT_TYPE = "objectType";
	public static final String FIELD_COLUMN_USER_ID = "userId";
	public static final String FIELD_COLUMN_INVOKE_ENDPOINT = "invokeEndpoint";
	public static final String FIELD_COLUMN_IS_WEBHOOK_ENABLED = "isWebhookEnabled";
	public static final String FIELD_COLUMN_IS_AUTHENTICATION_ENABLED = "isAuthenticationEnabled";
	public static final String FIELD_COLUMN_CREATED_BY = "createdBy";
	public static final String FIELD_COLUMN_MODIFIED_BY = "modifiedBy";
	public static final String FIELD_COLUMN_CREATED_ON = "createdOn";
	public static final String FIELD_COLUMN_MODIFIED_ON = "modifiedOn";

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn(FIELD_COLUMN_ID, COL_WEBHOOK_ID, true).withIsBackupId(true),
			new FieldColumn(FIELD_COLUMN_ETAG, COL_WEBHOOK_ETAG).withIsEtag(true),
			new FieldColumn(FIELD_COLUMN_OBJECT_ID, COL_WEBHOOK_OBJECT_ID),
			new FieldColumn(FIELD_COLUMN_OBJECT_TYPE, COL_WEBHOOK_OBJECT_TYPE),
			new FieldColumn(FIELD_COLUMN_USER_ID, COL_WEBHOOK_USER_ID),
			new FieldColumn(FIELD_COLUMN_INVOKE_ENDPOINT, COL_WEBHOOK_INVOKE_ENDPOINT),
			new FieldColumn(FIELD_COLUMN_IS_WEBHOOK_ENABLED, COL_WEBHOOK_IS_WEBHOOK_ENABLED),
			new FieldColumn(FIELD_COLUMN_IS_AUTHENTICATION_ENABLED, COL_WEBHOOK_IS_AUTHENTICATION_ENABLED),
			new FieldColumn(FIELD_COLUMN_CREATED_BY, COL_WEBHOOK_CREATED_BY),
			new FieldColumn(FIELD_COLUMN_MODIFIED_BY, COL_WEBHOOK_MODIFIED_BY),
			new FieldColumn(FIELD_COLUMN_CREATED_ON, COL_WEBHOOK_CREATED_ON),
			new FieldColumn(FIELD_COLUMN_MODIFIED_ON, COL_WEBHOOK_MODIFIED_ON) };

	@Override
	public TableMapping<DBOWebhook> getTableMapping() {
		return new TableMapping<DBOWebhook>() {
			@Override
			public DBOWebhook mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOWebhook dbo = new DBOWebhook();
				dbo.setId(rs.getLong(COL_WEBHOOK_ID));
				dbo.setObjectId(rs.getLong(COL_WEBHOOK_OBJECT_ID));
				dbo.setObjectType(rs.getString(COL_WEBHOOK_OBJECT_TYPE));
				dbo.setUserId(rs.getLong(COL_WEBHOOK_USER_ID));
				dbo.setInvokeEndpoint(rs.getString(COL_WEBHOOK_INVOKE_ENDPOINT));
				dbo.setIsWebhookEnabled(rs.getBoolean(COL_WEBHOOK_IS_WEBHOOK_ENABLED));
				dbo.setIsAuthenticationEnabled(rs.getBoolean(COL_WEBHOOK_IS_AUTHENTICATION_ENABLED));
				dbo.setEtag(rs.getString(COL_WEBHOOK_ETAG));
				dbo.setCreatedBy(rs.getLong(COL_WEBHOOK_CREATED_BY));
				dbo.setModifiedBy(rs.getLong(COL_WEBHOOK_MODIFIED_BY));
				dbo.setCreatedOn(rs.getTimestamp(COL_WEBHOOK_CREATED_ON));
				dbo.setModifiedOn(rs.getTimestamp(COL_WEBHOOK_MODIFIED_ON));
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

	public void setId(Long id) {
		this.id = id;
	}

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getInvokeEndpoint() {
		return invokeEndpoint;
	}

	public void setInvokeEndpoint(String invokeEndpoint) {
		this.invokeEndpoint = invokeEndpoint;
	}

	public boolean getIsWebhookEnabled() {
		return isWebhookEnabled;
	}

	public void setIsWebhookEnabled(boolean isWebhookEnabled) {
		this.isWebhookEnabled = isWebhookEnabled;
	}

	public boolean getIsAuthenticationEnabled() {
		return isAuthenticationEnabled;
	}

	public void setIsAuthenticationEnabled(boolean isAuthenticationEnabled) {
		this.isAuthenticationEnabled = isAuthenticationEnabled;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public Timestamp getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Timestamp modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, createdOn, etag, id, invokeEndpoint, isAuthenticationEnabled, isWebhookEnabled,
				modifiedBy, modifiedOn, objectId, objectType, userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOWebhook other = (DBOWebhook) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(etag, other.etag) && Objects.equals(id, other.id)
				&& Objects.equals(invokeEndpoint, other.invokeEndpoint)
				&& isAuthenticationEnabled == other.isAuthenticationEnabled
				&& isWebhookEnabled == other.isWebhookEnabled && Objects.equals(modifiedBy, other.modifiedBy)
				&& Objects.equals(modifiedOn, other.modifiedOn) && Objects.equals(objectId, other.objectId)
				&& Objects.equals(objectType, other.objectType) && Objects.equals(userId, other.userId);
	}

	@Override
	public String toString() {
		return "DBOWebhook [id=" + id + ", objectId=" + objectId + ", objectType=" + objectType + ", userId=" + userId
				+ ", invokeEndpoint=" + invokeEndpoint + ", isWebhookEnabled=" + isWebhookEnabled
				+ ", isAuthenticationEnabled=" + isAuthenticationEnabled + ", etag=" + etag + ", createdOn=" + createdOn
				+ ", modifiedOn=" + modifiedOn + ", createdBy=" + createdBy + ", modifiedBy=" + modifiedBy + "]";
	}

}
