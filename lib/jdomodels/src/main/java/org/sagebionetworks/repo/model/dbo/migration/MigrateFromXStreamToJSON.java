package org.sagebionetworks.repo.model.dbo.migration;

import java.util.Arrays;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;

/**
 * The translator will convert an old field that was serialized a compressed
 * XStream xml, to a new field serialized as JSON.
 * 
 * @param <D>
 */
public class MigrateFromXStreamToJSON<D extends DatabaseObject<?>> implements MigratableTableTranslation<D, D> {

	private final XStreamToJsonTranslator[] translators;

	public MigrateFromXStreamToJSON(XStreamToJsonTranslator... translators) {
		this.translators = translators;
	}

	@Override
	public D createDatabaseObjectFromBackup(D backup) {
		Arrays.stream(translators).forEach(t -> t.translate(backup));
		return backup;
	}

	@Override
	public D createBackupFromDatabaseObject(D dbo) {
		return dbo;
	}

}
