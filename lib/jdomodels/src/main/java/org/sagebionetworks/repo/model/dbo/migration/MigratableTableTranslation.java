package org.sagebionetworks.repo.model.dbo.migration;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;

/**
 * <p>
 * Database Objects (DBOs) are objects that are tightly coupled to the current schema of a database table.
 * A Backup object provides a layer of abstraction from the DBOs and are used to export and import rows of 
 * data across stacks.  Since the backup objects are abstracted form the database table schema they act as 
 * a buffer for schema changes across stacks.  This works well as long as only additive changes are made
 * to the backup objects.
 * </p><p>
 * Lets use an an example to illustrate the concepts.
 * </p><p>
 * In the first version of a database table, we have column
 * called 'foo'.  For this first version the backup object would mirror the database object, so they both would have
 * a field called 'foo'.  In the second version of the database table we decide to rename 'foo' to be 'bar'. In 
 * the current version the DBO will have a field called 'bar' but will no longer contain 'foo'.  To our backup
 * object we add new field call 'bar' but keep the 'foo' field (maybe with a @Depcated tag).  Now when we migrate
 * data from version one to version two we have all of the tools needed to translate from the old to the new.
 * This type of translation is performed by implementations of this interface. 
 * </p>
 * @param <D> the type of database object (DBO)
 * @param <B> the type of the backup object
 * 
 * @author John
 *
 */
public interface MigratableTableTranslation<D extends DatabaseObject<?>, B> {

	/**
	 * Create a database object from a given backup object.
	 * 
	 * @param backup
	 * @return
	 */
	public D createDatabaseObjectFromBackup(B backup);
	
	/**
	 * Create a backup object from a given database object.
	 * @param dbo
	 * @return
	 */
	public B createBackupFromDatabaseObject(D dbo);
}
