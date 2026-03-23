package org.sagebionetworks.repo.manager.grid.synch.schema;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;

import com.google.common.base.Objects;

/**
 * Implementation of {@link SchemaSource} that provides access to the source
 * schema and operations for modifying the source during schema synchronization.
 *
 * <p>
 * This class bridges the synchronization logic with the external data source
 * by:
 * <ul>
 * <li>Reading the current schema state from {@link SourceHandler}</li>
 * <li>Applying schema changes (additions, deletions) back to the source</li>
 * <li>Providing column-level comparison with the copy schema</li>
 * <li>Tracking which source columns have been consumed during
 * synchronization</li>
 * </ul>
 *
 * <p>
 * Key responsibilities:
 * <ul>
 * <li>Stream source columns for comparison with copy during Phase 1 of
 * {@link SynchronizationLogic#synchronize}</li>
 * <li>Consume matched columns to track remaining unprocessed columns</li>
 * <li>Add columns to source that were added by users to the copy (pushing user
 * additions)</li>
 * <li>Remove columns from source that were deleted by users from the copy
 * (pushing user deletions)</li>
 * <li>Provide column matching logic based on column names</li>
 * </ul>
 *
 * <p>
 * The internal schema list is mutable and consumed during synchronization. As
 * copy columns are matched with source columns via {@link #consume(String)},
 * they are removed from the list. After Phase 1, {@link #streamRemaining()}
 * returns only the unmatched source columns that need to be added to the copy.
 */
public class SchemaSourceImpl implements SchemaSource {

	private final SourceHandler handler;
	private final List<ColumnSourceItem> schema;

	/**
	 * Creates a new schema source implementation for synchronization with the
	 * external data source.
	 *
	 * <p>
	 * Initializes the source schema from
	 * {@link SourceHandler#getCurrentSourceSchema()}, converting each column name
	 * into a {@link ColumnSourceItem} for processing during synchronization.
	 *
	 * @param handler the handler providing access to the source data and schema
	 *                operations
	 */
	public SchemaSourceImpl(SourceHandler handler) {
		this.handler = handler;
		this.schema = handler.getCurrentSourceSchema().stream().map(n -> new ColumnSourceItem().setColumnName(n))
				.collect(Collectors.toList());
	}

	/**
	 * Extracts the key (column name) from a copy item for matching with source
	 * columns. Called during Phase 1 of {@link SynchronizationLogic#synchronize} to
	 * obtain the lookup key for finding the corresponding source column.
	 *
	 * @param item the copy column item
	 * @return the column name to use as the matching key
	 */
	@Override
	public String getKey(ColumnCopyItem item) {
		return item.getColumnName();
	}

	/**
	 * Attempts to find and consume a source column by its key (column name). Called
	 * during Phase 1 of {@link SynchronizationLogic#synchronize} when processing
	 * each copy column.
	 *
	 * <p>
	 * If a matching source column is found, it is removed from the internal schema
	 * list to mark it as consumed. This ensures that {@link #streamRemaining()}
	 * only returns unmatched columns that need to be added to the copy.
	 *
	 * @param key the column name to find
	 * @return Optional containing the matching source column, or empty if not found
	 */
	@Override
	public Optional<ColumnSourceItem> consume(String key) {
		Optional<ColumnSourceItem> item = schema.stream().filter(i -> i.getColumnName().equals(key)).findFirst();
		if (item.isPresent()) {
			schema.remove(item.get());
		}
		return item;
	}

	/**
	 * Streams the remaining unconsumed source columns after Phase 1 matching
	 * completes. Called during Phase 2 of {@link SynchronizationLogic#synchronize}
	 * to process columns that exist in the source but not in the copy.
	 *
	 * <p>
	 * These columns represent external additions from the source that need to be
	 * added to the copy (unless they were deleted by the user).
	 *
	 * @return a stream of source columns that were not matched with copy columns
	 */
	@Override
	public Stream<ColumnSourceItem> streamRemaining() {
		return schema.stream();
	}

	/**
	 * Returns whether columns can be added to or removed from this source.
	 * Delegates to {@link SourceHandler#canAddRemoveColumns()}.
	 *
	 * @return true if columns can be added to or removed from this source, false
	 *         otherwise
	 */
	@Override
	public boolean isItemAdditionSupported() {
		return handler.canAddRemoveColumns();
	}

	/**
	 * Returns whether columns can be removed from this source. Delegates to
	 * {@link SourceHandler#canAddRemoveColumns()}.
	 *
	 * @return true if columns can be removed from this source, false otherwise
	 */
	@Override
	public boolean isItemRemovalSupported() {
		return handler.canAddRemoveColumns();
	}

	/**
	 * Adds a column to the source schema. Called during Phase 2 of
	 * {@link SynchronizationLogic#synchronize} when a column exists in the copy but
	 * not in the source, and was changed by the user (pushing user addition to
	 * source).
	 *
	 * <p>
	 * Delegates to {@link SourceHandler#addColumnToSource(String)} to perform the
	 * actual schema modification on the external data source.
	 *
	 * @param toAdd the copy column to add to the source
	 */
	@Override
	public void addItem(ColumnCopyItem toAdd) {
		handler.addColumnToSource(toAdd.getColumnName());
	}

	/**
	 * Removes a column from the source schema. Called during Phase 2 of
	 * {@link SynchronizationLogic#synchronize} when a column exists in the source
	 * but not in the copy, and was deleted by the user (pushing user deletion to
	 * source).
	 *
	 * <p>
	 * Delegates to {@link SourceHandler#removeColumn(String)} to perform the actual
	 * schema modification on the external data source.
	 *
	 * @param toRemove the source column to remove
	 */
	@Override
	public void removeItem(ColumnSourceItem toRemove) {
		handler.removeColumn(toRemove.getColumnName());
	}

	/**
	 * Determines whether a copy column matches a source column. Currently matches
	 * solely based on column name equality.
	 *
	 * <p>
	 * Called during Phase 1 of {@link SynchronizationLogic#synchronize} after a
	 * source column is found via {@link #consume(String)} to verify the match
	 * before deciding whether synchronization is needed.
	 *
	 * @param copyItem   the column from the copy schema
	 * @param sourceItem the column from the source schema
	 * @return true if the columns match (same name), false otherwise
	 */
	@Override
	public boolean matches(ColumnCopyItem copyItem, ColumnSourceItem sourceItem) {
		return Objects.equal(copyItem.getColumnName(), sourceItem.getColumnName());
	}

}
