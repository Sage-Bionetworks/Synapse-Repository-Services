package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_RES_ACCESS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS_TYPE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOResourceAccessType implements MigratableDatabaseObject<DBOResourceAccessType, DBOResourceAccessType> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_RESOURCE_ACCESS_TYPE_ID, true),
		new FieldColumn("owner", COL_RESOURCE_ACCESS_TYPE_OWNER).withIsBackupId(true),
		new FieldColumn("element", COL_RESOURCE_ACCESS_TYPE_ELEMENT, true),
		};

	@Override
	public TableMapping<DBOResourceAccessType> getTableMapping() {
		return new TableMapping<DBOResourceAccessType>(){
			@Override
			public DBOResourceAccessType mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOResourceAccessType act = new DBOResourceAccessType();
				act.setId(rs.getLong(COL_RESOURCE_ACCESS_TYPE_ID));
				act.setOwner(rs.getLong(COL_RESOURCE_ACCESS_TYPE_OWNER));
				act.setElement(rs.getString(COL_RESOURCE_ACCESS_TYPE_ELEMENT));
				return act;
			}

			@Override
			public String getTableName() {
				return TABLE_RESOURCE_ACCESS_TYPE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_RES_ACCESS_TYPE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOResourceAccessType> getDBOClass() {
				return DBOResourceAccessType.class;
			}};
	}

	private Long id;
	private String element;
	private Long owner;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getElement() {
		return element;
	}
	public void setElement(String element) {
		this.element = element;
	}
	
	public Long getOwner() {
		return owner;
	}
	public void setOwner(Long owner) {
		this.owner = owner;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((element == null) ? 0 : element.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
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
		DBOResourceAccessType other = (DBOResourceAccessType) obj;
		if (element == null) {
			if (other.element != null)
				return false;
		} else if (!element.equals(other.element))
			return false;
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
		return true;
	}
	
	@Override
	public String toString() {
		return "DBOResourceAccessType [id=" + id + ", element=" + element
				+ ", owner=" + owner + "]";
	}
	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.ACL_ACCESS_TYPE;
	}
	@Override
	public MigratableTableTranslation<DBOResourceAccessType, DBOResourceAccessType> getTranslator() {
		return new BasicMigratableTableTranslation<DBOResourceAccessType>();
	}
	@Override
	public Class<? extends DBOResourceAccessType> getBackupClass() {
		return DBOResourceAccessType.class;
	}
	@Override
	public Class<? extends DBOResourceAccessType> getDatabaseObjectClass() {
		return DBOResourceAccessType.class;
	}
	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
	
}
