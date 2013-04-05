package org.sagebionetworks.repo.model.dbo.migration;

/**
 * Abstraction for a backup object.
 * 
 * @author John
 *
 */
public interface BackupObject {
	
	/**
	 * When this backup object is marshaled to/from XML, it is important to use an immutable element name.
	 * This alais will be used as 
	 * 
	 * While the backup object will change with every schema change, the alias should never change.
	 * 
	 * @return
	 */
	public String getImmutableAlias();
}
