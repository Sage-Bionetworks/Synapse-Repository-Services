package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_TYPE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_TYPE_VIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_TYPE_VIEW_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_TYPE_VIEW_TYPE_MASK;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_VIEW_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VIEW_TYPE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOViewType implements MigratableDatabaseObject<DBOViewType, DBOViewType> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("viewId", COL_VIEW_TYPE_VIEW_ID, true).withIsBackupId(true),
		new FieldColumn("viewObjectType", COL_VIEW_TYPE_VIEW_OBJECT_TYPE),
		new FieldColumn("viewTypeMask", COL_VIEW_TYPE_VIEW_TYPE_MASK),
		new FieldColumn("etag", COL_VIEW_TYPE_ETAG).withIsEtag(true),
	};

	private static final TableMapping<DBOViewType> TABLE_MAPPING = new TableMapping<DBOViewType>() {

		@Override
		public DBOViewType mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOViewType dbo = new DBOViewType();
			dbo.setViewId(rs.getLong(COL_VIEW_TYPE_VIEW_ID));
			dbo.setViewObjectType(rs.getString(COL_VIEW_TYPE_VIEW_OBJECT_TYPE));
			// removed with PLFM-4956
			dbo.setViewType(null);
			dbo.setEtag(rs.getString(COL_VIEW_TYPE_ETAG));
			dbo.setViewTypeMask(rs.getLong(COL_VIEW_TYPE_VIEW_TYPE_MASK));
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
		}
	};
	
	private static final MigratableTableTranslation<DBOViewType, DBOViewType> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();
	
	Long viewId;
	String viewObjectType;
	String viewType;
	String etag;
	Long viewTypeMask;
	
	public Long getViewId() {
		return viewId;
	}

	public void setViewId(Long viewId) {
		this.viewId = viewId;
	}

	public String getViewObjectType() {
		return viewObjectType;
	}
	
	public void setViewObjectType(String viewObjectType) {
		this.viewObjectType = viewObjectType;
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
	
	public Long getViewTypeMask() {
		return viewTypeMask;
	}

	public void setViewTypeMask(Long viewTypeMask) {
		this.viewTypeMask = viewTypeMask;
	}

	@Override
	public TableMapping<DBOViewType> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VIEW_TYPE;
	}

	@Override
	public MigratableTableTranslation<DBOViewType, DBOViewType> getTranslator() {
		return MIGRATION_TRANSLATOR;
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

	@Override
	public int hashCode() {
		return Objects.hash(etag, viewId, viewObjectType, viewType, viewTypeMask);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DBOViewType other = (DBOViewType) obj;
		return Objects.equals(etag, other.etag) && Objects.equals(viewId, other.viewId)
				&& Objects.equals(viewObjectType, other.viewObjectType) && Objects.equals(viewType, other.viewType)
				&& Objects.equals(viewTypeMask, other.viewTypeMask);
	}
	
	

}
