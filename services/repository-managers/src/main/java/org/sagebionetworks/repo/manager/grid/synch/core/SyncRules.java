package org.sagebionetworks.repo.manager.grid.synch.core;

/**
 * The rules that let the {@link SynchronizationLogic} classifier decide how a
 * copy item and a source item relate. This is the "policy" collaborator:
 * it makes no changes to either side and reads no bulk data. It is
 * kept separate from {@link SourceReader} (the source's data) and
 * {@link SyncOutcomeHandler} (the mutations) so each concern can vary and be
 * tested in isolation.
 *
 * @param <C> the type of items in the copy
 * @param <S> the type of items in the source
 */
public interface SyncRules<C extends CopyItem, S extends SourceItem> {

	/**
	 * Returns a unique key for the given copy item. This key is used to match
	 * items between the copy and source during Phase 1 of synchronization.
	 *
	 * @param copyItem the copy item to get the key for
	 * @return a unique string key identifying the item
	 */
	String getKey(C copyItem);

	/**
	 * Determines if a copy item matches a source item. Called during Phase 1 when
	 * both items exist to decide whether they are already equal (no change) or in
	 * conflict.
	 *
	 * @param copyItem   the item from the copy
	 * @param sourceItem the item from the source
	 * @return true if items match (unchanged), false if they differ (conflict)
	 */
	boolean matches(C copyItem, S sourceItem);

	/**
	 * Returns whether the given copy item should be excluded from matching during
	 * Phase 1 traversal. An excluded item is preserved in the copy and added
	 * to the source. The source owns this decision because it depends on the
	 * source's keying rules (e.g. a RecordSet copy row with an incomplete upsert
	 * key cannot be matched to a source row, but must not be removed from the copy;
	 * or a RecordSet copy row whose key collides with an earlier row's key in this
	 * same run).
	 *
	 * <p>
	 * The {@code key} is the value {@link #getKey(C)} returns for this item.
	 *
	 * <p>
	 * Implementations may be stateful (e.g. tracking which keys have already been
	 * seen this run to detect collisions). A {@code SyncRules} instance is used for
	 * exactly one {@link SynchronizationLogic#synchronize} run.
	 *
	 * <p>
	 * Defaults to false. Sources whose copy item identity is intrinsic (e.g. entity
	 * view rows refer to a specific entity ID) inherit the default and exclude nothing.
	 *
	 * @param copyItem the copy item to test
	 * @param key      the precomputed key for this copy item (see {@link #getKey(C)})
	 * @return true if the item should be left untouched by synchronization
	 */
	default boolean isExcludedFromMatching(C copyItem, String key) {
		return false;
	}

	/**
	 * Determines whether the given source item — present in the source but absent
	 * from the copy during Phase 2 — was deleted by the user in the copy.
	 *
	 * <p>
	 * The source owns this decision because, for row-based sources, the copy (CRDT)
	 * is not rich enough to determine that a user deleted an item. The answer can
	 * only be inferred from the synced baseline, which is source-side state.
	 *
	 * <p>
	 * Defaults to false; sources without a baseline concept (e.g. entity views)
	 * inherit the default.
	 *
	 * @param sourceItem the unmatched source item to test
	 * @return true if the user deleted this item from the copy, false otherwise
	 */
	default boolean wasDeletedByUser(S sourceItem) {
		return false;
	}

}
