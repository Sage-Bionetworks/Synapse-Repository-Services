package org.sagebionetworks.repo.model.dbo.file.download.v2;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DOWNLOAD_LIST_ITEM_V2;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST_ITEM_V2;

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

public class DBODownloadListItem implements MigratableDatabaseObject<DBODownloadListItem, DBODownloadListItem> {

	private Long principalId;
	private Long entityId;
	private Long versionNumber;
	private Timestamp addedOn;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("principalId", COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID, true).withIsBackupId(true),
			new FieldColumn("entityId", COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID),
			new FieldColumn("versionNumber", COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER),
			new FieldColumn("addedOn", COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON) };

	public static final TableMapping<DBODownloadListItem> MAPPING = new TableMapping<DBODownloadListItem>() {

		@Override
		public DBODownloadListItem mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBODownloadListItem dbo = new DBODownloadListItem();
			dbo.setPrincipalId(rs.getLong(COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID));
			dbo.setEntityId(rs.getLong(COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID));
			dbo.setVersionNumber(rs.getLong(COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER));
			dbo.setAddedOn(rs.getTimestamp(COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_DOWNLOAD_LIST_ITEM_V2;
		}

		@Override
		public String getDDLFileName() {
			return DDL_DOWNLOAD_LIST_ITEM_V2;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBODownloadListItem> getDBOClass() {
			return DBODownloadListItem.class;
		}
	};

	@Override
	public TableMapping<DBODownloadListItem> getTableMapping() {
		return MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DOWNLOAD_LIST_ITEM_2;
	}

	@Override
	public MigratableTableTranslation<DBODownloadListItem, DBODownloadListItem> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBODownloadListItem> getBackupClass() {
		return DBODownloadListItem.class;
	}

	@Override
	public Class<? extends DBODownloadListItem> getDatabaseObjectClass() {
		return DBODownloadListItem.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}

	/**
	 * @return the entityId
	 */
	public Long getEntityId() {
		return entityId;
	}

	/**
	 * @param entityId the entityId to set
	 */
	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	/**
	 * @return the versionNumber
	 */
	public Long getVersionNumber() {
		return versionNumber;
	}

	/**
	 * @param versionNumber the versionNumber to set
	 */
	public void setVersionNumber(Long versionNumber) {
		this.versionNumber = versionNumber;
	}

	/**
	 * @return the addedOn
	 */
	public Timestamp getAddedOn() {
		return addedOn;
	}

	/**
	 * @param addedOn the addedOn to set
	 */
	public void setAddedOn(Timestamp addedOn) {
		this.addedOn = addedOn;
	}

	@Override
	public int hashCode() {
		return Objects.hash(addedOn, entityId, principalId, versionNumber);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBODownloadListItem)) {
			return false;
		}
		DBODownloadListItem other = (DBODownloadListItem) obj;
		return Objects.equals(addedOn, other.addedOn) && Objects.equals(entityId, other.entityId)
				&& Objects.equals(principalId, other.principalId) && Objects.equals(versionNumber, other.versionNumber);
	}

	@Override
	public String toString() {
		return "DBODownloadListItem2 [principalId=" + principalId + ", entityId=" + entityId + ", versionNumber="
				+ versionNumber + ", addedOn=" + addedOn + "]";
	}

}
