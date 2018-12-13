package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SCOPE_CONTAINER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SCOPE_VIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_VIEW_SCOPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VIEW_SCOPE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOViewScope implements MigratableDatabaseObject<DBOViewScope, DBOViewScope> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("viewId", COL_VIEW_SCOPE_VIEW_ID, true).withIsBackupId(true),
		new FieldColumn("containerId", COL_VIEW_SCOPE_CONTAINER_ID, true),
	};
	
	Long id;
	Long viewId;
	Long containerId;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getViewId() {
		return viewId;
	}

	public void setViewId(Long viewId) {
		this.viewId = viewId;
	}

	public Long getContainerId() {
		return containerId;
	}

	public void setContainerId(Long containerId) {
		this.containerId = containerId;
	}

	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((containerId == null) ? 0 : containerId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((viewId == null) ? 0 : viewId.hashCode());
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
		DBOViewScope other = (DBOViewScope) obj;
		if (containerId == null) {
			if (other.containerId != null)
				return false;
		} else if (!containerId.equals(other.containerId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (viewId == null) {
			if (other.viewId != null)
				return false;
		} else if (!viewId.equals(other.viewId))
			return false;
		return true;
	}

	@Override
	public TableMapping<DBOViewScope> getTableMapping() {
		return new TableMapping<DBOViewScope>(){

			@Override
			public DBOViewScope mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBOViewScope dto = new DBOViewScope();
				dto.setViewId(rs.getLong(COL_VIEW_SCOPE_VIEW_ID));
				dto.setContainerId(rs.getLong(COL_VIEW_SCOPE_CONTAINER_ID));
				return dto;
			}

			@Override
			public String getTableName() {
				return TABLE_VIEW_SCOPE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_VIEW_SCOPE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOViewScope> getDBOClass() {
				return DBOViewScope.class;
			}};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VIEW_SCOPE;
	}

	@Override
	public MigratableTableTranslation<DBOViewScope, DBOViewScope> getTranslator() {
		return new BasicMigratableTableTranslation<DBOViewScope>();
	}

	@Override
	public Class<? extends DBOViewScope> getBackupClass() {
		return DBOViewScope.class;
	}

	@Override
	public Class<? extends DBOViewScope> getDatabaseObjectClass() {
		return DBOViewScope.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
