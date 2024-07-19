package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_OWNER_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_OWNER_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_BOUND_COLUMN_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_BOUND_COLUMN_OWNER;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * The of the bound columns.  This controls bound column migration.
 * 
 * @author jmhill
 *
 */
public class DBOBoundColumnOwner implements MigratableDatabaseObject<DBOBoundColumnOwner, DBOBoundColumnOwner> {

	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("objectId", COL_BOUND_OWNER_OBJECT_ID).withIsPrimaryKey(true).withIsBackupId(true),
		new FieldColumn("etag", COL_BOUND_OWNER_ETAG).withIsEtag(true)
	};

	private Long objectId;
	private String etag;
	
	@Override
	public TableMapping<DBOBoundColumnOwner> getTableMapping() {
		return new TableMapping<DBOBoundColumnOwner>() {
			
			@Override
			public DBOBoundColumnOwner mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOBoundColumnOwner dbo = new DBOBoundColumnOwner();
				dbo.setObjectId(rs.getLong(COL_BOUND_OWNER_OBJECT_ID));
				dbo.setEtag(rs.getString(COL_BOUND_OWNER_ETAG));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_BOUND_COLUMN_OWNER;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_BOUND_COLUMN_OWNER;
			}
			
			@Override
			public Class<? extends DBOBoundColumnOwner> getDBOClass() {
				return DBOBoundColumnOwner.class;
			}
		};
	}

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.BOUND_COLUMN_OWNER;
	}

	@Override
	public MigratableTableTranslation<DBOBoundColumnOwner, DBOBoundColumnOwner> getTranslator() {
		return new BasicMigratableTableTranslation<DBOBoundColumnOwner>();
	}

	@Override
	public Class<? extends DBOBoundColumnOwner> getBackupClass() {
		return DBOBoundColumnOwner.class;
	}

	@Override
	public Class<? extends DBOBoundColumnOwner> getDatabaseObjectClass() {
		return DBOBoundColumnOwner.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?,?>> seconday = new LinkedList<MigratableDatabaseObject<?,?>>();
		seconday.add(new DBOBoundColumnOrdinal());
		return seconday;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
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
		DBOBoundColumnOwner other = (DBOBoundColumnOwner) obj;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (objectId == null) {
			if (other.objectId != null)
				return false;
		} else if (!objectId.equals(other.objectId))
			return false;
		return true;
	}
	
}
