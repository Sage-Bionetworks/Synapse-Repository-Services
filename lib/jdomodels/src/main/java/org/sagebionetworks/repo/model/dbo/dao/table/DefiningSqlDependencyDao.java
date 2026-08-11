package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.model.entity.IdAndVersion;

/**
 * DAO for the reverse-lookup mapping of a defining-SQL object (a materialized view or a search index)
 * to the source tables/views it depends on. When a source table becomes available, the dependent
 * objects of a given type can be found via this mapping and rebuilt.
 */
public interface DefiningSqlDependencyDao {

	/**
	 * Associates the given set of source table ids with the object of the given id and type.
	 *
	 * @param objectId       The id and (optional) version of the dependent object
	 * @param objectType     The type discriminator (e.g. {@code materializedview}, {@code searchindex})
	 * @param sourceTableIds The set of source table ids the object depends on
	 */
	void addSourceTables(IdAndVersion objectId, String objectType, Set<IdAndVersion> sourceTableIds);

	/**
	 * Removes the association of the given set of source table ids from the object with the given id.
	 *
	 * @param objectId       The id and (optional) version of the dependent object
	 * @param sourceTableIds The set of source table ids to remove
	 */
	void deleteSourceTables(IdAndVersion objectId, Set<IdAndVersion> sourceTableIds);

	/**
	 * @param objectId The id and (optional) version of the dependent object
	 * @return The set of source table ids currently associated with the object
	 */
	Set<IdAndVersion> getSourceTables(IdAndVersion objectId);

	/**
	 * Records (replacing any existing associations) the single source table that the given object
	 * depends on. Convenience for single-source objects such as a search index.
	 *
	 * @param objectId      The id and (optional) version of the dependent object
	 * @param objectType    The type discriminator
	 * @param sourceTableId The id and (optional) version of the single source table/view
	 */
	void setSourceTable(IdAndVersion objectId, String objectType, IdAndVersion sourceTableId);

	/**
	 * Forward lookup of the single source table an object depends on.
	 *
	 * @param objectId The id and (optional) version of the dependent object
	 * @return The source table's id and (optional) version, or empty if none is registered
	 */
	Optional<IdAndVersion> getSourceTable(IdAndVersion objectId);

	/**
	 * Removes all source dependency associations for the object with the given id.
	 *
	 * @param objectId The id of the dependent object
	 */
	void deleteObject(IdAndVersion objectId);

	/**
	 * A dependent defining-SQL object: its id/version paired with its type discriminator.
	 */
	record DependentObject(IdAndVersion objectId, String objectType) {}

	/**
	 * Reverse lookup of every dependent object (of any type) that depends on the given source table,
	 * each paired with its {@code OBJECT_TYPE} — used by the generic source-dependency fan-out.
	 *
	 * @param sourceTableId The id and (optional) version of a source table
	 * @return A page of dependents (id/version + type) that depend on the source table
	 */
	List<DependentObject> getDependentsPage(IdAndVersion sourceTableId, long limit, long offset);

}
