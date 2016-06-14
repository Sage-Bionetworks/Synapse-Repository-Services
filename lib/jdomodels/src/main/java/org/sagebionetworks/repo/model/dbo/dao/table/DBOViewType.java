package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOViewType implements MigratableDatabaseObject<DBOViewType, DBOViewType> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("viewId", COL_VIEW_TYPE_VIEW_ID, true).withIsBackupId(true),
		new FieldColumn("viewType", COL_VIEW_TYPE_VIEW_TYPE),
		new FieldColumn("etag", COL_VIEW_TYPE_ETAG).withIsEtag(true),
	};
	
	Long viewId;
	String viewType;
	String etag;
	
	public Long getViewId() {
		return viewId;
	}

	public void setViewId(Long viewId) {
		this.viewId = viewId;
	}

	public String getViewType() {
		return viewType;
	}

	public void setViewType(String viewType) {
		this.viewType = viewType;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((viewId == null) ? 0 : viewId.hashCode());
		result = prime * result
				+ ((viewType == null) ? 0 : viewType.hashCode());
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
		DBOViewType other = (DBOViewType) obj;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (viewId == null) {
			if (other.viewId != null)
				return false;
		} else if (!viewId.equals(other.viewId))
			return false;
		if (viewType == null) {
			if (other.viewType != null)
				return false;
		} else if (!viewType.equals(other.viewType))
			return false;
		return true;
	}

	@Override
	public TableMapping<DBOViewType> getTableMapping() {
		return new TableMapping<DBOViewType>(){

			@Override
			public DBOViewType mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBOViewType dbo = new DBOViewType();
				dbo.setViewId(rs.getLong(COL_VIEW_TYPE_VIEW_ID));
				dbo.setViewType(rs.getString(COL_VIEW_TYPE_VIEW_TYPE));
				dbo.setEtag(rs.getString(COL_VIEW_TYPE_ETAG));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_VIEW_TYPE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_VIEW_TYPE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOViewType> getDBOClass() {
				return DBOViewType.class;
			}};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VIEW_TYPE;
	}

	@Override
	public MigratableTableTranslation<DBOViewType, DBOViewType> getTranslator() {
		return new MigratableTableTranslation<DBOViewType, DBOViewType>(){

			@Override
			public DBOViewType createDatabaseObjectFromBackup(DBOViewType backup) {
				return backup;
			}

			@Override
			public DBOViewType createBackupFromDatabaseObject(DBOViewType dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends DBOViewType> getBackupClass() {
		return DBOViewType.class;
	}

	@Override
	public Class<? extends DBOViewType> getDatabaseObjectClass() {
		return DBOViewType.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?, ?>> secondary = new LinkedList<MigratableDatabaseObject<?,?>>();
		secondary.add(new DBOViewScope());
		return secondary;
	}

}
