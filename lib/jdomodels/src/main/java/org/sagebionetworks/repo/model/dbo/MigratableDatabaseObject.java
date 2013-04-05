package org.sagebionetworks.repo.model.dbo;

import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigratableTableType;

/**
 * A database object that is Migratable.
 * 
 * @author John
 *
 * @param <D>
 * @param <B>
 */
public interface MigratableDatabaseObject<D extends DatabaseObject<?>, B> extends DatabaseObject<D>, MigratableTableTranslation<D, B> {

	/**
	 * The type of this table.
	 * @return
	 */
	public MigratableTableType getMigratableTableType();
}
