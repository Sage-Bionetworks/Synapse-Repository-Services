package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_REPOSITORY_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_REPOSITORY_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DOCKER_REPOSITORY_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOCKER_REPOSITORY_NAME;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/*
 * This secondary table to Node holds the fully qualified name of a managed Docker repository.
 */
public class DBODockerManagedRepositoryName
		implements MigratableDatabaseObject<DBODockerManagedRepositoryName, DBODockerManagedRepositoryName> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("owner", COL_DOCKER_REPOSITORY_OWNER_ID).withIsBackupId(true).withIsPrimaryKey(true),
			new FieldColumn("repositoryName", COL_DOCKER_REPOSITORY_NAME)};
	
	private Long owner;
	private String repositoryName;

	@Override
	public TableMapping<DBODockerManagedRepositoryName> getTableMapping() {
		return new TableMapping<DBODockerManagedRepositoryName>() {
			
			@Override
			public DBODockerManagedRepositoryName mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBODockerManagedRepositoryName dbo = new DBODockerManagedRepositoryName();
				dbo.setOwner(rs.getLong(COL_DOCKER_REPOSITORY_OWNER_ID));
				dbo.setRepositoryName(rs.getString(COL_DOCKER_REPOSITORY_NAME));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_DOCKER_REPOSITORY_NAME;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return  DDL_DOCKER_REPOSITORY_NAME;
			}
			
			@Override
			public Class<? extends DBODockerManagedRepositoryName> getDBOClass() {
				return DBODockerManagedRepositoryName.class; 
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DOCKER_REPOSITORY_NAME;
	}

	@Override
	public MigratableTableTranslation<DBODockerManagedRepositoryName, DBODockerManagedRepositoryName> getTranslator() {
		return new BasicMigratableTableTranslation<DBODockerManagedRepositoryName>();
	}

	@Override
	public Class<? extends DBODockerManagedRepositoryName> getBackupClass() {
		return DBODockerManagedRepositoryName.class;
	}

	@Override
	public Class<? extends DBODockerManagedRepositoryName> getDatabaseObjectClass() {
		return DBODockerManagedRepositoryName.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getOwner() {
		return owner;
	}

	public void setOwner(Long owner) {
		this.owner = owner;
	}

	public String getRepositoryName() {
		return repositoryName;
	}

	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(owner, repositoryName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBODockerManagedRepositoryName other = (DBODockerManagedRepositoryName) obj;
		return Objects.equals(owner, other.owner) && Objects.equals(repositoryName, other.repositoryName);
	}

	@Override
	public String toString() {
		return "DBODockerManagedRepositoryName [owner=" + owner + ", repositoryName=" + repositoryName + "]";
	}

}
