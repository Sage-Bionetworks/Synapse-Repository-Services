package org.sagebionetworks.repo.model.table;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sagebionetworks.repo.model.ObjectType;

public class ViewScopeUtils {
	
	private static final Map<ViewEntityType, ViewObjectType> ENTITY_TYPE_MAP = Stream.of(ViewEntityType.values())
			.collect(Collectors.toMap(Function.identity(), (entityType) -> {
				// entityview -> ENTITY
				// submissionview -> SUBMISSION
				return ViewObjectType.valueOf(entityType.name().toUpperCase().replace("VIEW", ""));
			}));
			

	/**
	 * @param objectType The {@link ObjectType} to map from
	 * @return An optional containing a {@link ViewObjectType} mapped to the given
	 *         object type if such mapping exist. The mapping is done by name value.
	 */
	public static Optional<ViewObjectType> map(ObjectType objectType) {
		try {
			return Optional.of(ViewObjectType.valueOf(objectType.name()));
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	/**
	 * @param viewObjectType The {@link ViewObjectType} to map from
	 * @return The {@link ObjectType} mapped to the given {@link ViewObjectType},
	 *         should always exist. The mapping is done by name value.
	 * @throws IllegalArgumentException If a mapping could not be found
	 */
	public static ObjectType map(ViewObjectType viewObjectType) {
		return ObjectType.valueOf(viewObjectType.name());
	}

	/**
	 * @param viewObjectType The {@link ViewObjectType}
	 * @return The default subtype value used for replication for the given object
	 *         type (e.g. use when there is not subtype for the given type)
	 */
	public static String defaultSubType(ViewObjectType viewObjectType) {
		return viewObjectType.name().toLowerCase();
	}
	
	/**
	 * @param type
	 * @return The {@link ViewObjectType} mapped to the given {@link ViewEntityType}
	 */
	public static ViewObjectType map(ViewEntityType type) {
		ViewObjectType viewObjectType = ENTITY_TYPE_MAP.get(type);
		if (viewObjectType == null) {
			throw new IllegalArgumentException("Unsupported type " + type);
		}
		return viewObjectType;
	}

}
