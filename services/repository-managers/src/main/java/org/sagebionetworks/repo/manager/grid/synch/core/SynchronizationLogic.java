package org.sagebionetworks.repo.manager.grid.synch.core;

import java.util.Optional;
import java.util.stream.Stream;

import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Component;

/**
 * Classifies items between a copy (CRDT replica) and a source of truth into a set
 * of terminal outcomes, and reports each to a {@link SyncOutcomeHandler}.
 */
@Component
public class SynchronizationLogic {

	/**
	 * Classifies every item across a copy and a source into one of six terminal
	 * outcomes and reports each to the given {@link SyncOutcomeHandler}. This engine
	 * is a pure classifier: it never mutates the copy or the source itself. Every
	 * change — pushing to the source, pulling into the copy, dropping an item, or
	 * resolving a conflict — is performed by the handler, which also owns the policy
	 * for the situations the engine cannot resolve alone (e.g. whether the source
	 * can accept a user addition).
	 *
	 * <p>
	 * Phase 1: Process all items in the copy
	 * <ul>
	 * <li>excluded from matching (never looked up against the source) →
	 * {@code onCopyOnlyItemAddedByUser}</li>
	 * <li>For each matchable item in the copy, consume the matching item from the source (if
	 * it exists)</li>
	 * <li>present in the source and equal → {@code onCopyAndSourceMatch}</li>
	 * <li>present in the source but not equal → {@code onCopyAndSourceConflict}</li>
	 * <li>copy-only and changed by the user → {@code onCopyOnlyItemAddedByUser}</li>
	 * <li>copy-only and not changed by the user → {@code onCopyOnlyItemDeletedFromSource}</li>
	 * </ul>
	 *
	 * <p>
	 * Phase 2 (each remaining source item):
	 * <ul>
	 * <li>deleted by the user → {@code onSourceOnlyItemDeletedByUserFromCopy}</li>
	 * <li>otherwise → {@code onSourceOnlyItemAddedSinceLastSync}</li>
	 * </ul>
	 *
	 * @param <C>       the type of items in the copy
	 * @param <S>       the type of items in the source
	 * @param copyItems the items currently in the copy (Phase 1 input)
	 * @param source    the read-only source of truth
	 * @param rules     the keying/matching/user-intent rules
	 * @param handler   notified of the terminal outcome of every item
	 */
	public <C extends CopyItem, S extends SourceItem> void synchronize(Stream<C> copyItems, SourceReader<S> source,
			SyncRules<C, S> rules, SyncOutcomeHandler<C, S> handler) {
		ValidateArgument.required(copyItems, "copyItems");
		ValidateArgument.required(source, "source");
		ValidateArgument.required(rules, "rules");
		ValidateArgument.required(handler, "handler");

		// Phase 1: Process all items in the copy
		copyItems.forEach(copyItem -> {
			String key = rules.getKey(copyItem);
			ValidateArgument.required(key, "key");
			if (rules.isExcludedFromMatching(copyItem, key)) {
				handler.onNewCopyItem(copyItem, key);
				// Excluded items are never looked up against the source
				return;
			}
			Optional<S> sourceValue = source.consume(key);
			if (sourceValue.isPresent()) {
				// Item exists in both copy and source
				S sourceItem = sourceValue.get();
				if (rules.matches(copyItem, sourceItem)) {
					handler.onCopyAndSourceMatch(copyItem, sourceItem);
				} else {
					handler.onCopyAndSourceConflict(copyItem, sourceItem);
				}
			} else if (copyItem.wasChangedByUser()) {
				handler.onNewCopyItem(copyItem, key);
			} else {
				handler.onDeletedFromSource(copyItem);
			}
		});

		// Phase 2: Process remaining items in source (items not in copy)
		source.streamRemaining().forEach(sourceItem -> {
			if (rules.wasDeletedByUser(sourceItem)) {
				handler.onDeletedFromCopy(sourceItem);
			} else {
				handler.onNewSourceItem(sourceItem);
			}
		});
	}
}
