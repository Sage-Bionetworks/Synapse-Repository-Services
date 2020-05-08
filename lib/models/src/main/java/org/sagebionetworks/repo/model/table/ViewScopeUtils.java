package org.sagebionetworks.repo.model.table;

import java.util.Optional;

import org.sagebionetworks.repo.model.ObjectType;

public class ViewScopeUtils {

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

}
