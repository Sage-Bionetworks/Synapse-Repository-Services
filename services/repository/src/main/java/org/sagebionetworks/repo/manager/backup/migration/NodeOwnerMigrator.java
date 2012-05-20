package org.sagebionetworks.repo.manager.backup.migration;


public interface NodeOwnerMigrator extends RevisionMigrationStep {
	public Long getUserPrincipal(String userName);
}
