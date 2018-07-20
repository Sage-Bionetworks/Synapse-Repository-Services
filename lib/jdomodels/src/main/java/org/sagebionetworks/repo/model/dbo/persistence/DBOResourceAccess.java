package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_RES_ACCESS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOResourceAccess implements MigratableDatabaseObject<DBOResourceAccess, DBOResourceAccess>{
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_ID, true),
		new FieldColumn("owner", COL_RESOURCE_ACCESS_OWNER).withIsBackupId(true),
		new FieldColumn("userGroupId", COL_RESOURCE_ACCESS_GROUP_ID),
		};

	@Override
	public TableMapping<DBOResourceAccess> getTableMapping() {

		return new TableMapping<DBOResourceAccess>(){

			@Override
			public DBOResourceAccess mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOResourceAccess ra = new DBOResourceAccess();
				ra.setId(rs.getLong(COL_ID));
				ra.setOwner(rs.getLong(COL_RESOURCE_ACCESS_OWNER));
				ra.setUserGroupId(rs.getLong(COL_RESOURCE_ACCESS_GROUP_ID));
				return ra;
			}

			@Override
			public String getTableName() {
				return TABLE_RESOURCE_ACCESS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_RES_ACCESS;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOResourceAccess> getDBOClass() {
				return DBOResourceAccess.class;
			}};
	}
	

	private Long id;
	private Long owner;
	private Long userGroupId;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getOwner() {
		return owner;
	}
	public void setOwner(Long owner) {
		this.owner = owner;
	}
	public Long getUserGroupId() {
		return userGroupId;
	}
	public void setUserGroupId(Long userGroupId) {
		this.userGroupId = userGroupId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((userGroupId == null) ? 0 : userGroupId.hashCode());
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
		DBOResourceAccess other = (DBOResourceAccess) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (userGroupId == null) {
			if (other.userGroupId != null)
				return false;
		} else if (!userGroupId.equals(other.userGroupId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOResourceAccess [id=" + id + ", owner=" + owner
				+ ", userGroupId=" + userGroupId + "]";
	}
	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.ACL_ACCESS;
	}
	@Override
	public MigratableTableTranslation<DBOResourceAccess, DBOResourceAccess> getTranslator() {
		return new BasicMigratableTableTranslation<DBOResourceAccess>();
	}
	@Override
	public Class<? extends DBOResourceAccess> getBackupClass() {
		return DBOResourceAccess.class;
	}
	@Override
	public Class<? extends DBOResourceAccess> getDatabaseObjectClass() {
		return DBOResourceAccess.class;
	}
	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
	

}
