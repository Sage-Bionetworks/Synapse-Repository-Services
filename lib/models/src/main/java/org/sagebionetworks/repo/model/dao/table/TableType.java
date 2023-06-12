package org.sagebionetworks.repo.model.dao.table;

import java.util.Optional;
import java.util.stream.Stream;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Enumeration that maps all of the types of tables/views to {@link EntityType},
 * {@link ObjectType}, and {@link ViewEntityType}
 *
 */
public enum TableType {

	table(ObjectType.TABLE),
	entityview(ObjectType.ENTITY_VIEW),
	submissionview(ObjectType.ENTITY_VIEW),
	dataset(ObjectType.ENTITY_VIEW),
	datasetcollection(ObjectType.ENTITY_VIEW),
	materializedview(ObjectType.MATERIALIZED_VIEW),
	virtualtable(ObjectType.VIRTUAL_TABLE);

	// There is worker which handle each object type.This Object type should match expected type.
	// entityview, submissionview, dataset, datasetcollection is handled by same worker.
	private final ObjectType objectType;
	private final EntityType entityType;
	private final ViewEntityType viewEntityType;

	TableType(ObjectType objectType) {
		this.entityType = EntityType.valueOf(this.name());
		this.objectType = objectType;
		this.viewEntityType = Stream.of(ViewEntityType.values()).filter((v) -> v.name().equals(this.name())).findFirst()
				.orElse(null);
	};

	public EntityType getEntityType() {
		return entityType;
	}

	public ObjectType getObjectType() {
		return objectType;
	}

	public Optional<ViewEntityType> getViewEntityType() {
		return Optional.ofNullable(viewEntityType);
	}

	/**
	 * Does this type have an {@link ViewEntityType}? 
	 * @return
	 */
	public boolean isViewEntityType() {
		return viewEntityType != null;
	}

	/**
	 * Lookup the TableType given an EntityType. Note: Only a sub-set of EntityTypes
	 * are tables.
	 * 
	 * @param entityType
	 * @return
	 */
	public static Optional<TableType> lookupByEntityType(EntityType entityType) {
		ValidateArgument.required(entityType, "entityType");
		return Stream.of(TableType.values()).filter((t) -> t.entityType.equals(entityType)).findFirst();
	}
}
