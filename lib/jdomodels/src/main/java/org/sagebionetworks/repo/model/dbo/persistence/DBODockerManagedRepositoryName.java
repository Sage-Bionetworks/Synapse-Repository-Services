package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_REPOSITORY_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_REPOSITORY_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOCKER_REPOSITORY_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/*
 * This secondary table to Node holds the fully qualified name of a managed Docker repository.
 */
@Table(name = TABLE_DOCKER_REPOSITORY_NAME, constraints = { 
		"unique key UNIQUE_DOCKER_REPO_NAME ("+ COL_DOCKER_REPOSITORY_NAME +")" })
public class DBODockerManagedRepositoryName implements MigratableDatabaseObject<DBODockerManagedRepositoryName, DBODockerManagedRepositoryName> {

	@Field(name = COL_DOCKER_REPOSITORY_OWNER_ID, backupId = true, primary = true, nullable = false)
	@ForeignKey(table = TABLE_NODE, field = COL_NODE_ID, cascadeDelete = true, name = "DOCKER_REPO_NAME_FK")
	private Long owner;

	@Field(name = COL_DOCKER_REPOSITORY_NAME, varchar = 512, backupId = false, primary = false, nullable = false)
	private String repositoryName;

	private static TableMapping<DBODockerManagedRepositoryName> TABLE_MAPPING = 
			AutoTableMapping.create(DBODockerManagedRepositoryName.class);
	
	@Override
	public TableMapping<DBODockerManagedRepositoryName> getTableMapping() {
		return TABLE_MAPPING;
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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result
				+ ((repositoryName == null) ? 0 : repositoryName.hashCode());
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
		DBODockerManagedRepositoryName other = (DBODockerManagedRepositoryName) obj;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (repositoryName == null) {
			if (other.repositoryName != null)
				return false;
		} else if (!repositoryName.equals(other.repositoryName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBODockerManagedRepositoryName [owner=" + owner
				+ ", repositoryName=" + repositoryName + "]";
	}
	
	

}
