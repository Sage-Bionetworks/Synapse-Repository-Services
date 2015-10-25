package org.sagebionetworks.repo.model.dbo;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * A database object that is Migratable.
 * 
 * @author John
 *
 * @param <D> - database object type
 * @param <B> - backup object type
 */
public interface MigratableDatabaseObject<D extends DatabaseObject<?>, B> extends DatabaseObject<D> {

	/**
	 * The type of this table.
	 * @return
	 */
	public MigrationType getMigratableTableType();
	
	/**
	 * Get the translator that will be used to translate from one object to another.
	 * @return
	 */
	public MigratableTableTranslation<D, B> getTranslator();
	
	/**
	 * The class for <B>
	 * @return
	 */
	public Class<? extends B> getBackupClass();
	
	/**
	 * The class for <D>
	 * @return
	 */
	public Class<? extends D> getDatabaseObjectClass();
	
	/**
	 * If this object is the 'owner' of other objects, then it is a primary type. All secondary types should be returned in their
	 * migration order.
	 * For example, if A owns B and B owns C (A->B->C) then A is the primary, and both B and C are secondary. For this case, return B followed by C.
	 * Both B and C must have a backup ID column that is a foreign key to the backup ID of A, as the IDs of A will drive the migration of B and C.
	 * @return
	 */
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes();
}
