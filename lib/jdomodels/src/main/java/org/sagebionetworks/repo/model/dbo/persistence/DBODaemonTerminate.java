package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BACKUP_FORCE_TERMINATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BACKUP_TERM_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DAEMON_TERMINATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_BACKUP_TERMINATE;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * DBO used to foce the termination of a job.
 * @author jmhill
 *
 */
public class DBODaemonTerminate implements DatabaseObject<DBODaemonTerminate> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("owner", COL_BACKUP_TERM_OWNER, true),
			new FieldColumn("forceTerminate", COL_BACKUP_FORCE_TERMINATION),
	};

	@Override
	public TableMapping<DBODaemonTerminate> getTableMapping() {
		return new TableMapping<DBODaemonTerminate>() {

			@Override
			public DBODaemonTerminate mapRow(ResultSet rs, int index)
					throws SQLException {
				DBODaemonTerminate term = new DBODaemonTerminate();
				term.setOwner(rs.getLong(COL_BACKUP_TERM_OWNER));
				term.setForceTerminate(rs.getBoolean(COL_BACKUP_FORCE_TERMINATION));
				return term;
			}

			@Override
			public String getTableName() {
				return TABLE_BACKUP_TERMINATE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DAEMON_TERMINATE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				// TODO Auto-generated method stub
				return FIELDS;
			}

			@Override
			public Class<? extends DBODaemonTerminate> getDBOClass() {
				return DBODaemonTerminate.class;
			}
		};
	}
	private Long owner;
	private Boolean forceTerminate;

	public Long getOwner() {
		return owner;
	}
	public void setOwner(Long owner) {
		this.owner = owner;
	}
	public Boolean getForceTerminate() {
		return forceTerminate;
	}
	public void setForceTerminate(Boolean forceTerminate) {
		this.forceTerminate = forceTerminate;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((forceTerminate == null) ? 0 : forceTerminate.hashCode());
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
		DBODaemonTerminate other = (DBODaemonTerminate) obj;
		if (forceTerminate == null) {
			if (other.forceTerminate != null)
				return false;
		} else if (!forceTerminate.equals(other.forceTerminate))
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
		return "DBODaemonTerminate [owner=" + owner + ", forceTerminate="
				+ forceTerminate + "]";
	}
	
}
