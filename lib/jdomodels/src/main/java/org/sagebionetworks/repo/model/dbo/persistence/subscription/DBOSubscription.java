package org.sagebionetworks.repo.model.dbo.persistence.subscription;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBSCRIPTION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBSCRIPTION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBSCRIPTION_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBSCRIPTION_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBSCRIPTION_SUBSCRIBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_SUBSCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SUBSCRIPTION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOSubscription implements MigratableDatabaseObject<DBOSubscription, DBOSubscription>{


	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_SUBSCRIPTION_ID, true).withIsBackupId(true),
		new FieldColumn("subscriberId", COL_SUBSCRIPTION_SUBSCRIBER_ID),
		new FieldColumn("objectId", COL_SUBSCRIPTION_OBJECT_ID),
		new FieldColumn("objectType", COL_SUBSCRIPTION_OBJECT_TYPE),
		new FieldColumn("createdOn", COL_SUBSCRIPTION_CREATED_ON)
	};

	private Long id;
	private Long subscriberId;
	private Long objectId;
	private String objectType;
	private Long createdOn;

	@Override
	public String toString() {
		return "DBOSubscription [id=" + id + ", subscriberId=" + subscriberId
				+ ", objectId=" + objectId + ", objectType=" + objectType
				+ ", createdOn=" + createdOn + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdOn, id, objectId, objectType, subscriberId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOSubscription other = (DBOSubscription) obj;
		return Objects.equals(createdOn, other.createdOn) && Objects.equals(id, other.id)
				&& Objects.equals(objectId, other.objectId) && Objects.equals(objectType, other.objectType)
				&& Objects.equals(subscriberId, other.subscriberId);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getSubscriberId() {
		return subscriberId;
	}

	public void setSubscriberId(Long subscriberId) {
		this.subscriberId = subscriberId;
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

	public Long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	@Override
	public TableMapping<DBOSubscription> getTableMapping() {
		return new TableMapping<DBOSubscription>(){

			@Override
			public DBOSubscription mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOSubscription dbo = new DBOSubscription();
				dbo.setId(rs.getLong(COL_SUBSCRIPTION_ID));
				dbo.setSubscriberId(rs.getLong(COL_SUBSCRIPTION_SUBSCRIBER_ID));
				dbo.setObjectId(rs.getLong(COL_SUBSCRIPTION_OBJECT_ID));
				dbo.setObjectType(rs.getString(COL_SUBSCRIPTION_OBJECT_TYPE));
				dbo.setCreatedOn(rs.getLong(COL_SUBSCRIPTION_CREATED_ON));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_SUBSCRIPTION;
			}

			@Override
			public String getDDLFileName() {
				return DDL_SUBSCRIPTION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOSubscription> getDBOClass() {
				return DBOSubscription.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.SUBSCRIPTION;
	}

	@Override
	public MigratableTableTranslation<DBOSubscription, DBOSubscription> getTranslator() {
		return new BasicMigratableTableTranslation<DBOSubscription>();
	}

	@Override
	public Class<? extends DBOSubscription> getBackupClass() {
		return DBOSubscription.class;
	}

	@Override
	public Class<? extends DBOSubscription> getDatabaseObjectClass() {
		return DBOSubscription.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
