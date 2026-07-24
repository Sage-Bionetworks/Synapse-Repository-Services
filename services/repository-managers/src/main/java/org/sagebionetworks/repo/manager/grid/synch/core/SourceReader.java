package org.sagebionetworks.repo.manager.grid.synch.core;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Read-only access to the items in a source during synchronization.
 * This is one of the three collaborators the {@link SynchronizationLogic}
 * classifier consumes.
 *
 * <p>
 * A reader is single-use and stateful: {@link #consume(String)} removes matched
 * items during Phase 1 so that {@link #streamRemaining()} yields exactly the
 * source items that were never matched to a copy item during Phase 2.
 *
 * @param <S> the type of items in the source
 */
public interface SourceReader<S extends SourceItem> {

	/**
	 * Retrieves and removes an item with the given key from the source. Called
	 * during Phase 1 for each copy item to find and consume its matching source
	 * item.
	 *
	 * @param key the unique key identifying the item
	 * @return an Optional containing the item if found, or empty if not found
	 */
	Optional<S> consume(String key);

	/**
	 * Returns a stream of all remaining items in the source that have not been
	 * consumed. Used during Phase 2 to process items that exist only in the
	 * source (items not found in the copy).
	 *
	 * @return a stream of remaining unconsumed items
	 */
	Stream<S> streamRemaining();

}
