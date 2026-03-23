/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REALM_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REALM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REALM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_REALM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REALM;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * @author brucehoff
 *
 */
public class DBORealm implements MigratableDatabaseObject<DBORealm, DBORealm> {
	private Long id;
	private Date creationDate;
	private String name;


	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_REALM_ID, true).withIsBackupId(true),
		new FieldColumn("creationDate", COL_REALM_CREATED_ON),
		new FieldColumn("name", COL_REALM_NAME)
		};

	@Override
	public TableMapping<DBORealm> getTableMapping() {
		return new TableMapping<DBORealm>() {
			// Map a result set to this object
			@Override
			public DBORealm mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBORealm realm = new DBORealm();
				realm.setId(rs.getLong(COL_REALM_ID));
				Timestamp ts = rs.getTimestamp(COL_REALM_CREATED_ON);
				realm.setCreationDate(new Date(ts.getTime()));
				realm.setName(rs.getString(COL_REALM_NAME));
				return realm;
			}

			@Override
			public String getTableName() {
				return TABLE_REALM;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_REALM;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBORealm> getDBOClass() {
				return DBORealm.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.REALM;
	}

	@Override
	public Class<? extends DBORealm> getBackupClass() {
		return DBORealm.class;
	}

	@Override
	public Class<? extends DBORealm> getDatabaseObjectClass() {
		return DBORealm.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return Collections.singletonList(new DBORealmIdentityProvider());
	}

	@Override
	public MigratableTableTranslation<DBORealm, DBORealm> getTranslator() {
		return new BasicMigratableTableTranslation<DBORealm>();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		DBORealm other = (DBORealm) obj;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBORealm [id=" + id + ", creationDate=" + creationDate + ", name=" + name + "]";
	}


}
