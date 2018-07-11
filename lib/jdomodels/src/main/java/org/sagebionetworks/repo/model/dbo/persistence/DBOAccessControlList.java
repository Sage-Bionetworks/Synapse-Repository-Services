package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_ACL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_CONTROL_LIST;

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

public class DBOAccessControlList implements MigratableDatabaseObject<DBOAccessControlList, DBOAccessControlList> {
	private Long id;
	private String etag;
	private Long creationDate;
	private Long ownerId;
	private String ownerType;
	
	public static final String OWNER_ID_FIELD_NAME = "ownerId";
	public static final String OWNER_TYPE_FIELD_NAME = "ownerType";

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_ACL_ID, false).withIsBackupId(true),
		new FieldColumn("etag", COL_ACL_ETAG).withIsEtag(true),
		new FieldColumn(OWNER_TYPE_FIELD_NAME, COL_ACL_OWNER_TYPE, true),
		new FieldColumn(OWNER_ID_FIELD_NAME, COL_ACL_OWNER_ID, true),
		new FieldColumn("creationDate", COL_ACL_CREATED_ON)
		};

	@Override
	public TableMapping<DBOAccessControlList> getTableMapping() {
		return new TableMapping<DBOAccessControlList>(){

			@Override
			public DBOAccessControlList mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOAccessControlList acl = new DBOAccessControlList();
				acl.setId(rs.getLong(COL_ACL_ID));
				acl.setEtag(rs.getString(COL_ACL_ETAG));
				acl.setCreationDate(rs.getLong(COL_ACL_CREATED_ON));
				acl.setOwnerId(rs.getLong(COL_ACL_OWNER_ID));
				acl.setOwnerType(rs.getString(COL_ACL_OWNER_TYPE));
				return acl;
			}

			@Override
			public String getTableName() {
				return TABLE_ACCESS_CONTROL_LIST;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_ACL;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOAccessControlList> getDBOClass() {
				return DBOAccessControlList.class;
			}};
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}


	public String getOwnerType() {
		return ownerType;
	}

	public void setOwnerType(String ownerType) {
		this.ownerType = ownerType;
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getEtag() {
		return etag;
	}
	public void setEtag(String etag) {
		this.etag = etag;
	}
	public Long getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Long creationDate) {
		this.creationDate = creationDate;
	}
	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.ACL;
	}
	
	@Override
	public MigratableTableTranslation<DBOAccessControlList, DBOAccessControlList> getTranslator() {
		return new BasicMigratableTableTranslation<DBOAccessControlList>();
	}
	@Override
	public Class<? extends DBOAccessControlList> getBackupClass() {
		return DBOAccessControlList.class;
	}
	@Override
	public Class<? extends DBOAccessControlList> getDatabaseObjectClass() {
		return DBOAccessControlList.class;
	}
	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<MigratableDatabaseObject<?,?>>();
		list.add(new DBOResourceAccess());
		list.add(new DBOResourceAccessType());
		return list;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
		result = prime * result
				+ ((ownerType == null) ? 0 : ownerType.hashCode());
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
		DBOAccessControlList other = (DBOAccessControlList) obj;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (ownerId == null) {
			if (other.ownerId != null)
				return false;
		} else if (!ownerId.equals(other.ownerId))
			return false;
		if (ownerType == null) {
			if (other.ownerType != null)
				return false;
		} else if (!ownerType.equals(other.ownerType))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DBOAccessControlList [id=" + id + ", etag=" + etag
				+ ", creationDate=" + creationDate + ", ownerId=" + ownerId
				+ ", ownerType=" + ownerType + "]";
	}
}
