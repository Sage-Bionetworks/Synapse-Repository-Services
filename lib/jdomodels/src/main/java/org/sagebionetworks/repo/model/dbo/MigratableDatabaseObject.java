package org.sagebionetworks.repo.model.dbo;

import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigratableTableType;

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
	public MigratableTableType getMigratableTableType();
	
	/**
	 * Get the translator that will be used to translate from one object to another.
	 * @return
	 */
	public MigratableTableTranslation<D, B> getTranslator();
}
