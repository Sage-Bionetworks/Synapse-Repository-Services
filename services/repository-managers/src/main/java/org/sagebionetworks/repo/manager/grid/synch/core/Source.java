package org.sagebionetworks.repo.manager.grid.synch.core;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents the source of truth for items that are synchronized with a copy.
 * During synchronization, items from the source are compared with items in the
 * copy to determine what changes need to be made in both directions.
 *
 * @param <C> the type of items in the copy
 * @param <S> the type of items in the source
 */
public interface Source<C extends CopyItem, S extends SourceItem> {

 /**
  * Returns a unique key for the given copy item. This key is used to match
  * items between the copy and source during Phase 1 of synchronization.
  *
  * @param copyItem the copy item to get the key for
  * @return a unique string key identifying the item
  */
 String getKey(C copyItem);

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

 /**
  * Adds a new item to the source. Called during Phase 1 when an item exists
  * in the copy but not in the source and was changed by the user (push user's
  * addition).
  *
  * @param copyItem the copy item to add to the source
  */
 void addItem(C copyItem);

 /**
  * Removes an item from the source. Called during Phase 2 when an item exists
  * in the source but not in the copy and was deleted by the user (push user's
  * deletion).
  *
  * @param toRemove the source item to remove
  */
 void removeItem(S toRemove);

 /**
  * Determines if a copy item matches a source item. Called during Phase 1
  * when both items exist to decide if merging is needed.
  *
  * @param copyItem   the item from the copy
  * @param sourceItem the item from the source
  * @return true if items match (no merge needed), false if they differ
  */
 boolean matches(C copyItem, S sourceItem);

}
