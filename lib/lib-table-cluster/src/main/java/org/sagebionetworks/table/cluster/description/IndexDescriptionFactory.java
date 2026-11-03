package org.sagebionetworks.table.cluster.description;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Rebuilds an {@link IndexDescription} from the {@link IndexDescriptionState} captured with it, so
 * a caller holding only that state recovers the queried type's own behavior rather than
 * re-implementing it. The state carries no dependencies, so a defining-SQL type resolves the
 * sources its SQL names through the supplied {@link IndexDescriptionLookup}.
 */
public class IndexDescriptionFactory {

	/**
	 * Rebuild the description the given state was captured from.
	 *
	 * @param state  the captured state
	 * @param lookup resolves the sources a defining-SQL type's SQL names
	 * @return a description of the state's type, carrying that type's full behavior
	 */
	public static IndexDescription createIndexDescription(IndexDescriptionState state, IndexDescriptionLookup lookup) {
		ValidateArgument.required(state, "state");
		ValidateArgument.required(lookup, "lookup");

		IdAndVersion idAndVersion = state.getIdAndVersion();
		TableType tableType = state.getTableType();

		switch (tableType) {
		case table:
			return new TableIndexDescription(idAndVersion, state.getIndexVersion().orElse(null));
		case entityview:
		case dataset:
		case datasetcollection:
		case submissionview:
			return new ViewIndexDescription(idAndVersion, tableType, state.getIndexVersion().orElse(null));
		case materializedview:
			return new MaterializedViewIndexDescription(idAndVersion, requiredDefiningSql(state), lookup);
		case virtualtable:
			return new VirtualTableIndexDescription(idAndVersion, requiredDefiningSql(state), lookup);
		case recordset:
			return new RecordSetIndexDescription(idAndVersion, state.getIndexVersion().orElse(null));
		default:
			throw new IllegalArgumentException("Unexpected table type: " + tableType);
		}
	}

	private static String requiredDefiningSql(IndexDescriptionState state) {
		// A defining-SQL type is defined by nothing else, so state without it cannot be rebuilt at
		// all. Failing here beats rebuilding a description that would translate the query wrong.
		return state.getDefiningSql().orElseThrow(() -> new IllegalStateException(
				state.getTableType() + " " + state.getIdAndVersion() + " was captured without its defining SQL"));
	}
}
