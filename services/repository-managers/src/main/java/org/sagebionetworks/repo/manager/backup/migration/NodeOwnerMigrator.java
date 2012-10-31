package org.sagebionetworks.repo.manager.backup.migration;


public interface NodeOwnerMigrator extends RevisionMigrationStep {
	/**
	 * returns 
	 * @param name group name or individual email address
	 * @return the UserGroup ID having the given name or null if name is not found
	 * 
	 */
	public Long getUserPrincipal(String name);
	
	
	/**
	 * 
	 * @param name group name or individual email address
	 * @return the UserGroup ID having the given name, substituing that of some administrator if the name is not found
	 * 
	 */
	public Long getUserPrincipalWithSubstitution(String userName);
}
