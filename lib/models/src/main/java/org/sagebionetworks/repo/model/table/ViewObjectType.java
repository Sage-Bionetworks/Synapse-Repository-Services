package org.sagebionetworks.repo.model.table;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Subset of {@link ObjectType} that are supported by the replication index
 * 
 * @author Marco Marasca
 *
 */
public enum ViewObjectType {

	ENTITY(ViewEntityType.entityview, ReplicationType.ENTITY),
	SUBMISSION(ViewEntityType.submissionview, ReplicationType.SUBMISSION),
	DATASET(ViewEntityType.dataset, ReplicationType.ENTITY),
	DATASET_COLLECTION(ViewEntityType.datasetcollection, ReplicationType.ENTITY);

	private ViewEntityType viewEntityType;
	private ReplicationType mainType;
	private ObjectType objectType;

	private ViewObjectType(ViewEntityType viewEntityType, ReplicationType mainType) {
		this.viewEntityType = viewEntityType;
		this.mainType = mainType;
		this.objectType = ObjectType.valueOf(mainType.name());
	}

	/**
	 * @return The {@link ViewEntityType} that maps to this object type
	 */
	public ViewEntityType getViewEntityType() {
		return viewEntityType;
	}

	/**
	 * @return The {@link ObjectType} that maps to this object type
	 */
	public ObjectType getObjectType() {
		return objectType;
	}
	
	public ReplicationType getMainType() {
		return this.mainType;
	}

	/**
	 * @param objectType
	 * @return An optional containing the {@link ViewObjectType} that maps to the given {@link ObjectType}
	 * @throws IllegalArgumentException If the given {@link ObjectType} is null
	 */
	public static Optional<ViewObjectType> map(ObjectType objectType) {
		ValidateArgument.required(objectType, "objectType");

		return map((type) -> type.getObjectType() == objectType);
	}

	/**
	 * @param viewEntityType
	 * @return The {@link ViewObjectType} that maps to the given {@link ViewEntityType}
	 * @throws IllegalArgumentException If the given {@link ViewEntityType} is null
	 * @throws IllegalStateException If the given {@link ViewEntityType} is not supported
	 */
	public static ViewObjectType map(ViewEntityType viewEntityType) {
		ValidateArgument.required(viewEntityType, "viewEntityType");

		return map((type) -> type.getViewEntityType() == viewEntityType)
				.orElseThrow(() -> new IllegalStateException("Unsupported type " + viewEntityType));
	}

	private static Optional<ViewObjectType> map(Predicate<ViewObjectType> predicate) {
		return Stream.of(ViewObjectType.values()).filter(predicate).findFirst();
	}
}
