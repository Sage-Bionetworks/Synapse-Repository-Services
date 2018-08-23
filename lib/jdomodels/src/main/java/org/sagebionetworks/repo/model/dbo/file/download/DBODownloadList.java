package org.sagebionetworks.repo.model.dbo.file.download;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DOWNLOAD_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

import com.google.common.collect.Lists;

public class DBODownloadList implements MigratableDatabaseObject<DBODownloadList, DBODownloadList> {
	
	private Long principalId;
	private Long updatedOn;
	private String etag;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("principalId", COL_DOWNLOAD_LIST_PRINCIPAL_ID, true).withIsBackupId(true),
			new FieldColumn("etag", COL_DOWNLOAD_LIST_ETAG).withIsEtag(true),
			new FieldColumn("updatedOn", COL_DOWNLOAD_LIST_UPDATED_ON),
			};


	@Override
	public TableMapping<DBODownloadList> getTableMapping() {

		return new TableMapping<DBODownloadList>() {

			@Override
			public DBODownloadList mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBODownloadList dbo =  new DBODownloadList();
				dbo.setEtag(rs.getString(COL_DOWNLOAD_LIST_ETAG));
				dbo.setPrincipalId(rs.getLong(COL_DOWNLOAD_LIST_PRINCIPAL_ID));
				dbo.setUpdatedOn(rs.getLong(COL_DOWNLOAD_LIST_UPDATED_ON));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_DOWNLOAD_LIST;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DOWNLOAD_LIST;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBODownloadList> getDBOClass() {
				return DBODownloadList.class;
			}
		};
	}

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}

	public Long getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Long updatedOn) {
		this.updatedOn = updatedOn;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DOWNLOAD_LIST;
	}

	@Override
	public MigratableTableTranslation<DBODownloadList, DBODownloadList> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBODownloadList> getBackupClass() {
		return DBODownloadList.class;
	}

	@Override
	public Class<? extends DBODownloadList> getDatabaseObjectClass() {
		return DBODownloadList.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return Lists.newArrayList(new DBODownloadListItem());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((principalId == null) ? 0 : principalId.hashCode());
		result = prime * result + ((updatedOn == null) ? 0 : updatedOn.hashCode());
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
		DBODownloadList other = (DBODownloadList) obj;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		if (updatedOn == null) {
			if (other.updatedOn != null)
				return false;
		} else if (!updatedOn.equals(other.updatedOn))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBODownloadList [principalId=" + principalId + ", updatedOn=" + updatedOn + ", etag=" + etag + "]";
	}

}
