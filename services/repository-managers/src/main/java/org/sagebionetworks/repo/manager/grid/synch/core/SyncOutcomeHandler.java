package org.sagebionetworks.repo.manager.grid.synch.core;

/**
 * Receives the terminal outcome the {@link SynchronizationLogic} classifier
 * assigns to every item it visits, and performs whatever mutation that outcome
 * calls for. All writes — to the copy (CRDT replica), to the source of
 * truth, and any conflict merge are triggered by this handler. Each
 * layer (schema, rows, cells) must supply its own handler.
 *
 * <p>
 * The six methods correspond to the raw situations the classifier can observe.
 * The handler owns the follow-on policy — for example, whether a copy-only
 * item can actually be pushed to the source, or must instead be dropped from the
 * copy. There are intentionally no default implementations: a handler owns the
 * mutations, so a silent no-op default would be silent data loss.
 *
 * @param <C> the type of items in the copy
 * @param <S> the type of items in the source
 */
public interface SyncOutcomeHandler<C extends CopyItem, S extends SourceItem> {

	/**
	 * The copy item was matched to an equal source item during Phase 1 traversal —
	 * both sides already agree on this item's value. Whether this causes any write
	 * is handler-specific: a direct writer (e.g. EntityView) typically does
	 * nothing, while a rebuild-artifact writer (e.g. RecordSet) still includes this
	 * item in a new artifact (CSV).
	 *
	 * @param copyItem   the copy item that matched
	 * @param sourceItem the equal source item it was matched to
	 */
	void onCopyAndSourceMatch(C copyItem, S sourceItem);

	/**
	 * The item exists in both the copy and the source, but they are not equal. The
	 * handler resolves the conflict (e.g. a nested cell-level synchronization for
	 * rows).
	 *
	 * @param copyItem   the item from the copy
	 * @param sourceItem the conflicting item from the source
	 */
	void onCopyAndSourceConflict(C copyItem, S sourceItem);

	/**
	 * The item exists only in the copy and must be treated as an addition. The
	 * handler decides whether to push the addition to the source or — when the
	 * source cannot accept it — drop it from the copy.
	 *
	 * @param copyItem the copy item to add
	 * @param key      the precomputed key for this copy item (see
	 *                 {@link SyncRules#getKey})
	 */
	void onNewCopyItem(C copyItem, String key);

	/**
	 * The item exists only in the copy and was not changed by the user, meaning it
	 * was deleted from the source. The handler removes it from the copy.
	 *
	 * @param copyItem the copy item to remove
	 */
	void onDeletedFromSource(C copyItem);

	/**
	 * The item exists only in the source and was deleted by the user in the copy.
	 * The handler decides whether to push the deletion to the source or — if the
	 * source cannot remove it — pull the item back into the copy.
	 *
	 * @param sourceItem the source item the user deleted from the copy
	 */
	void onDeletedFromCopy(S sourceItem);

	/**
	 * The item exists only in the source and was not deleted by the user, meaning
	 * it was added to the source since the copy was last synchronized. The handler
	 * pulls it into the copy.
	 *
	 * @param sourceItem the source item to pull into the copy
	 */
	void onNewSourceItem(S sourceItem);

}
