package org.sagebionetworks.repo.manager.grid.synch.core;

/**
 * Defines a strategy for merging changes between a copy item and a source item
 * when they don't match during Phase 1 of synchronization.
 *
 * @param <C> the type of items in the copy
 * @param <S> the type of items in the source
 */
public interface Merge<C extends CopyItem, S extends SourceItem> {

 /**
  * Merges changes between two items that exist in both copy and source but
  * don't match. Called during Phase 1 when items are found in both locations
  * but have different values.
  * 
  * <p>
  * The merge strategy determines how to resolve conflicts, typically by:
  * <ul>
  * <li>Prioritizing user changes in the copy and pushing them to the source</li>
  * <li>Accepting source changes for items not modified by the user</li>
  * <li>Updating the copy to reflect the merged result</li>
  * </ul>
  *
  * @param key        the unique identifier for the items being merged
  * @param copyItem   the item from the copy (includes user-change tracking)
  * @param sourceItem the item from the source (current source state)
  */
 void merge(String key, C copyItem, S sourceItem);

 /**
  * Creates a no-op merge strategy that performs no merge operation. Used when
  * merging is not needed or handled elsewhere (e.g., schema synchronization
  * where columns are simply added/removed).
  *
  * @param <C> the copy item type
  * @param <S> the source item type
  * @return a no-op merge instance that does nothing when items don't match
  */
 public static <C extends CopyItem, S extends SourceItem> Merge<C, S> noOp() {
  return (key, copyItem, sourceItem) -> {
  };
 }
}
