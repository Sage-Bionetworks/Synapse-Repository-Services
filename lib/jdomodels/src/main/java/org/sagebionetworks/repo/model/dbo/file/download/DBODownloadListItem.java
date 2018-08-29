package org.sagebionetworks.repo.model.dbo.file.download;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_ASSOCIATED_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_ASSOCIATED_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DOWNLOAD_LIST_ITEM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST_ITEM;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBODownloadListItem implements MigratableDatabaseObject<DBODownloadListItem, DBODownloadListItem> {

	private Long principalId;
	private Long associatedObjectId;
	private String associatedObjectType;
	private Long fileHandleId;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("principalId", COL_DOWNLOAD_LIST_ITEM_PRINCIPAL_ID, true).withIsBackupId(true),
			new FieldColumn("associatedObjectId", COL_DOWNLOAD_LIST_ITEM_ASSOCIATED_OBJECT_ID),
			new FieldColumn("associatedObjectType", COL_DOWNLOAD_LIST_ITEM_ASSOCIATED_OBJECT_TYPE),
			new FieldColumn("fileHandleId", COL_DOWNLOAD_LIST_ITEM_FILE_HANDLE_ID) };

	@Override
	public TableMapping<DBODownloadListItem> getTableMapping() {
		return new TableMapping<DBODownloadListItem>() {

			@Override
			public DBODownloadListItem mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBODownloadListItem dbo = new DBODownloadListItem();
				dbo.setPrincipalId(rs.getLong(COL_DOWNLOAD_LIST_ITEM_PRINCIPAL_ID));
				dbo.setAssociatedObjectId(rs.getLong(COL_DOWNLOAD_LIST_ITEM_ASSOCIATED_OBJECT_ID));
				dbo.setAssociatedObjectType(rs.getString(COL_DOWNLOAD_LIST_ITEM_ASSOCIATED_OBJECT_TYPE));
				dbo.setFileHandleId(rs.getLong(COL_DOWNLOAD_LIST_ITEM_FILE_HANDLE_ID));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_DOWNLOAD_LIST_ITEM;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DOWNLOAD_LIST_ITEM;
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
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DOWNLOAD_LIST_ITEM;
	}

	@Override
	public MigratableTableTranslation<DBODownloadListItem, DBODownloadListItem> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBODownloadListItem> getBackupClass() {
		return DBODownloadListItem.class;
	}

	public Long getAssociatedObjectId() {
		return associatedObjectId;
	}

	public void setAssociatedObjectId(Long associatedObjectId) {
		this.associatedObjectId = associatedObjectId;
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

	public String getAssociatedObjectType() {
		return associatedObjectType;
	}

	public void setAssociatedObjectType(String associatedObjectType) {
		this.associatedObjectType = associatedObjectType;
	}

	public Long getFileHandleId() {
		return fileHandleId;
	}

	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((associatedObjectId == null) ? 0 : associatedObjectId.hashCode());
		result = prime * result + ((associatedObjectType == null) ? 0 : associatedObjectType.hashCode());
		result = prime * result + ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result + ((principalId == null) ? 0 : principalId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBODownloadListItem other = (DBODownloadListItem) obj;
		if (associatedObjectId == null) {
			if (other.associatedObjectId != null)
				return false;
		} else if (!associatedObjectId.equals(other.associatedObjectId))
			return false;
		if (associatedObjectType != other.associatedObjectType)
			return false;
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBODownloadListItem [principalId=" + principalId + ", assocaitedObjectId=" + associatedObjectId
				+ ", assocaitedObjectType=" + associatedObjectType + ", fileHandleId=" + fileHandleId + "]";
	}

}
