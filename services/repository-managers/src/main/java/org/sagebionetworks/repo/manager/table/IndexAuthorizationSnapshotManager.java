package org.sagebionetworks.repo.manager.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.BenefactorColumn;
import org.sagebionetworks.repo.model.table.ColumnLineageEntry;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.DerivationKind;
import org.sagebionetworks.repo.model.table.IndexAuthorizationSnapshot;
import org.sagebionetworks.repo.model.table.IndexDescriptionSnapshot;
import org.sagebionetworks.repo.model.table.SourceColumnReference;
import org.sagebionetworks.repo.model.table.SourceDependency;
import org.sagebionetworks.table.cluster.SQLTranslatorUtils;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.cluster.columntranslation.SchemaColumnTranslationReference;
import org.sagebionetworks.table.cluster.description.BenefactorDescription;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.IndexDescriptionState;
import org.sagebionetworks.table.cluster.description.SnapshotIndexDescription;
import org.sagebionetworks.table.cluster.description.VirtualTableIndexDescription;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;
import org.sagebionetworks.table.query.model.SqlContext;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

/**
 * Builds the {@link IndexAuthorizationSnapshot} that captures the as-built authorization state of a
 * queryable index at build time, and exposes the persisted snapshot for reads. A defining-SQL object
 * (MaterializedView, SearchIndex) is captured from its defining SQL and bound schema; a base index type
 * (plain table, entity/dataset/submission view, record set) is captured as an identity of its own bound
 * columns. A VirtualTable is never captured - it has no physical index and is inlined at query time.
 * <p>
 * A snapshot has two parts. The {@link IndexDescriptionSnapshot} is a serialized authorization
 * projection of the runtime {@link IndexDescription}: the object's id, version, table type, its
 * baked-in benefactor columns, and the flattened transitive closure of its dependencies. It is
 * reconstituted at query time so the transitive-dependency ACL check and benefactor row-level filter
 * run against as-built state through the same authorization code path used for current-truth state.
 * The column lineage records each output column's derivation, flattened at build time to leaf source
 * columns.
 * <p>
 * Both the transitive dependency closure and the column lineage are composed solely from each immediate
 * dependency's as-built snapshot, never from the dependency's current defining SQL or schema. Every
 * physical index captures its snapshot at build time, and an index builds only once its dependencies are
 * available, so each dependency's persisted snapshot exists. It rode that source's atomic index swap and
 * is held frozen by our read lock, so it records exactly what the bytes we consume were built from. A
 * VirtualTable never materializes, so its snapshot is computed on the fly from its defining SQL over its
 * own sources' snapshots. A dependency with neither is a broken invariant and fails the build.
 */
@Service
public class IndexAuthorizationSnapshotManager {

	private final TableManagerSupport tableManagerSupport;
	private final TableIndexConnectionFactory connectionFactory;

	public IndexAuthorizationSnapshotManager(TableManagerSupport tableManagerSupport,
			TableIndexConnectionFactory connectionFactory) {
		this.tableManagerSupport = tableManagerSupport;
		this.connectionFactory = connectionFactory;
	}

	/**
	 * Build the as-built authorization snapshot for a defining-SQL object being indexed.
	 *
	 * @param indexDescription the runtime description of the index being built, carrying its id, table
	 *                         type, benefactor columns, and the full transitive tree of dependent
	 *                         descriptions. On the shadow-rebuild path this carries the temporary id the
	 *                         build targets.
	 * @param definingSql      the object's defining SQL, used to compute column lineage.
	 * @param boundSchema      the object's bound output schema, in select-list order, supplying each
	 *                         output column's stable id.
	 * @return the snapshot to persist alongside the index.
	 */
	public IndexAuthorizationSnapshot buildSnapshot(IndexDescription indexDescription, String definingSql,
			List<ColumnModel> boundSchema) {
		ValidateArgument.required(indexDescription, "indexDescription");
		ValidateArgument.required(definingSql, "definingSql");
		ValidateArgument.required(boundSchema, "boundSchema");

		IdAndVersion object = indexDescription.getIdAndVersion();
		Map<IdAndVersion, IndexAuthorizationSnapshot> sources = resolveSourceSnapshots(indexDescription);
		return new IndexAuthorizationSnapshot()
				.setObjectId(toObjectIdString(object))
				.setVersionNumber(object.getVersion().orElse(null))
				.setIndexDescription(buildIndexDescriptionSnapshot(indexDescription, sources))
				.setColumnLineage(flattenedLineage(indexDescription, definingSql, boundSchema, sources));
	}

	/**
	 * Build the as-built authorization snapshot for a base index type that has no defining SQL: a plain
	 * table, an entity/dataset/submission view, or a record set. Such an index selects its own columns
	 * directly rather than deriving them from sources, so each output column is an identity of itself -
	 * its lineage is a single leaf reference to the same object and column. The authorization projection
	 * (benefactor columns and the flattened transitive dependency closure) is captured exactly as for a
	 * defining-SQL object; for a plain table and record set both are empty, while a view carries its
	 * single row-level benefactor column.
	 *
	 * @param indexDescription the runtime description of the index being built.
	 * @param boundSchema      the exact output schema the build used, supplying each output column's id.
	 *                         Passed in (not re-read here) so the snapshot reflects the columns actually
	 *                         built even if the bound schema drifts after the build.
	 * @return the snapshot to persist alongside the index.
	 */
	public IndexAuthorizationSnapshot buildSnapshot(IndexDescription indexDescription, List<ColumnModel> boundSchema) {
		ValidateArgument.required(indexDescription, "indexDescription");
		ValidateArgument.required(boundSchema, "boundSchema");

		IdAndVersion object = indexDescription.getIdAndVersion();
		return new IndexAuthorizationSnapshot()
				.setObjectId(toObjectIdString(object))
				.setVersionNumber(object.getVersion().orElse(null))
				.setIndexDescription(buildIndexDescriptionSnapshot(indexDescription, resolveSourceSnapshots(indexDescription)))
				.setColumnLineage(identityLineage(object, boundSchema));
	}

	/**
	 * Resolve each immediate dependency of the index being built to its as-built snapshot, in dependency
	 * order. The snapshots are resolved up front, before any composition, so a missing or corrupt one
	 * fails the build rather than letting the capture fall back to live state.
	 */
	private Map<IdAndVersion, IndexAuthorizationSnapshot> resolveSourceSnapshots(IndexDescription indexDescription) {
		Map<IdAndVersion, IndexAuthorizationSnapshot> sources = new LinkedHashMap<>();
		for (IndexDescription dependency : indexDescription.getDependencies()) {
			sources.computeIfAbsent(dependency.getIdAndVersion(), this::resolveAuthorizationSnapshot);
		}
		return sources;
	}

	/**
	 * The column lineage of a base index type that selects its own columns: each bound output column is an
	 * identity whose sole input is a leaf reference to the same object and column.
	 */
	private List<ColumnLineageEntry> identityLineage(IdAndVersion object, List<ColumnModel> boundSchema) {
		return boundSchema.stream()
				.map(column -> new ColumnLineageEntry()
						.setOutputColumnId(column.getId())
						.setDerivationKind(DerivationKind.IDENTITY)
						.setInputs(Collections.singletonList(new SourceColumnReference()
								.setSourceObjectId(toObjectIdString(object))
								.setSourceVersionNumber(object.getVersion().orElse(null))
								.setSourceColumnId(column.getId()))))
				.collect(Collectors.toList());
	}

	/**
	 * The {@code "syn"}-prefixed id string a snapshot records for its own object. A materialized view
	 * shadow rebuild builds the index under a temporary <em>negated</em> id (see
	 * {@code MaterializedViewManagerImpl}), then atomically swaps the index - and its snapshot - into the
	 * real id. Because the snapshot is served for the real id after that swap, its content must record the
	 * real object id, not the temporary build-target id. The temporary id is exactly the negation of the
	 * real id (Synapse object ids are always positive), so its absolute value recovers the real id; for an
	 * in-place build the id is already positive and this is a no-op.
	 */
	private static String toObjectIdString(IdAndVersion object) {
		return KeyFactory.keyToString(Math.abs(object.getId()));
	}

	/**
	 * Get the persisted authorization snapshot for an object, resolving the connection to the index
	 * database that holds it.
	 *
	 * @param object the object version to read.
	 * @return the snapshot, or empty when none has been captured (a VirtualTable, or an object whose index
	 *         has not been built).
	 */
	public Optional<IndexAuthorizationSnapshot> getAuthorizationSnapshot(IdAndVersion object) {
		ValidateArgument.required(object, "object");
		try {
			return connectionFactory.connectToTableIndex(object).getAuthorizationSnapshot(object);
		} catch (TableIndexConnectionUnavailableException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Resolve the query-time index description for a queried object: a snapshot-backed description that
	 * carries the as-built authorization projection and column lineage. For objects with a persisted
	 * snapshot (materialized indexes like MaterializedView and SearchIndex, or base indexes like plain
	 * tables and views where a snapshot was captured during the build), the persisted snapshot is loaded.
	 * For VirtualTables (which are never materialized and have no persisted snapshot), a snapshot is
	 * computed on-the-fly from the VirtualTable's defining SQL and bound schema, with its source resolved
	 * recursively.
	 *
	 * @param idAndVersion the object being queried (or a dependent inlined by a VirtualTable)
	 * @return a snapshot-backed description with correct lineage aligned to the object's bound schema
	 * @throws IllegalStateException if the object has no persisted snapshot and is not a VirtualTable
	 */
	public SnapshotIndexDescription getSnapshotIndexDescription(IdAndVersion idAndVersion) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		// The change-number provider must yield the same value the live IndexDescription is built with
		// (TableManagerSupport.getTableVersion): the truth change number for a table, but the index version
		// for a view/dataset/recordset. Binding it to getLastTableChangeNumber instead would leave the
		// query-cache hash unchanged across incremental view/dataset index updates, serving stale
		// count/facet results.
		return SnapshotIndexDescription.fromSnapshot(resolveAuthorizationSnapshot(idAndVersion),
				id -> Optional.of(tableManagerSupport.getTableVersion(id)));
	}

	/**
	 * The as-built snapshot of an object: its persisted snapshot, or for a VirtualTable (which is never
	 * materialized) one computed on the fly.
	 *
	 * @throws IllegalStateException if the object has no persisted snapshot and is not a VirtualTable
	 */
	private IndexAuthorizationSnapshot resolveAuthorizationSnapshot(IdAndVersion idAndVersion) {
		Optional<IndexAuthorizationSnapshot> persistedSnapshot = getAuthorizationSnapshot(idAndVersion);
		if (persistedSnapshot.isPresent()) {
			return persistedSnapshot.get();
		}

		TableType tableType = tableManagerSupport.getTableType(idAndVersion);
		if (!TableType.virtualtable.equals(tableType)) {
			throw new IllegalStateException(
					"No authorization snapshot exists for " + idAndVersion + " and it is not a VirtualTable");
		}

		// The VT's source is resolved recursively through getSnapshotIndexDescription, so nested
		// VirtualTables are handled and each level gets lineage aligned to its own bound schema.
		String definingSql = tableManagerSupport.getDefiningSql(idAndVersion).orElseThrow(
				() -> new IllegalStateException("VirtualTable " + idAndVersion + " has no defining SQL"));
		List<ColumnModel> boundSchema = tableManagerSupport.getTableSchema(idAndVersion);
		VirtualTableIndexDescription vtDescription = new VirtualTableIndexDescription(idAndVersion, definingSql,
				this::getSnapshotIndexDescription);
		return buildSnapshot(vtDescription, definingSql, boundSchema);
	}

	/**
	 * Project the runtime index description onto the authorization-relevant data preflight consumes: the
	 * root's id/version/type, its baked-in benefactor columns, the flattened transitive closure of
	 * its dependencies, and the minimal state needed to reconstruct the real IndexDescription with all
	 * its type-specific behavior.
	 *
	 * @param sources the as-built snapshot of each immediate dependency, which supplies that dependency's
	 *                own already-flattened closure.
	 */
	IndexDescriptionSnapshot buildIndexDescriptionSnapshot(IndexDescription indexDescription,
			Map<IdAndVersion, IndexAuthorizationSnapshot> sources) {
		IdAndVersion object = indexDescription.getIdAndVersion();
		IndexDescriptionState state = indexDescription.getState();
		List<BenefactorColumn> benefactors = indexDescription.getBenefactors().stream()
				.map(IndexAuthorizationSnapshotManager::toBenefactorColumn).collect(Collectors.toList());
		// The closure excludes the root: each immediate dependency followed by its own closure, de-duplicated
		// in first-seen order.
		LinkedHashMap<IdAndVersion, SourceDependency> dependencies = new LinkedHashMap<>();
		for (IndexDescription dependency : indexDescription.getDependencies()) {
			IdAndVersion dependencyId = dependency.getIdAndVersion();
			dependencies.putIfAbsent(dependencyId, new SourceDependency()
					.setObjectId(KeyFactory.keyToString(dependencyId.getId()))
					.setVersionNumber(dependencyId.getVersion().orElse(null))
					.setTableType(dependency.getTableType().name()));
			for (SourceDependency transitive : sources.get(dependencyId).getIndexDescription().getDependencies()) {
				dependencies.putIfAbsent(toIdAndVersion(transitive), transitive);
			}
		}
		return new IndexDescriptionSnapshot()
				.setObjectId(toObjectIdString(object))
				.setVersionNumber(object.getVersion().orElse(null))
				.setTableType(indexDescription.getTableType().name())
				.setBenefactors(benefactors)
				.setDependencies(new ArrayList<>(dependencies.values()))
				.setDefiningSql(state.getDefiningSql().orElse(null))
				.setIndexVersion(state.getIndexVersion().orElse(null));
	}

	private static BenefactorColumn toBenefactorColumn(BenefactorDescription description) {
		return new BenefactorColumn()
				.setBenefactorColumnName(description.getBenefactorColumnName())
				.setBenefactorType(description.getBenefactorType().name());
	}

	private static IdAndVersion toIdAndVersion(SourceDependency dependency) {
		return IdAndVersion.newBuilder()
				.setId(KeyFactory.stringToKey(dependency.getObjectId()))
				.setVersion(dependency.getVersionNumber())
				.build();
	}

	/**
	 * Compute a node's fully flattened column lineage: compute its immediate lineage from its defining SQL,
	 * align it to its bound output schema to assign each output column its stable id, then flatten every
	 * input to leaf source columns by composing against each source's as-built lineage, which is already
	 * flattened to leaves.
	 *
	 * @param node    the node being captured.
	 * @param sql     the node's defining SQL.
	 * @param schema  the node's bound output schema, in select-list order.
	 * @param sources the as-built snapshot of each immediate dependency.
	 * @throws IllegalStateException if a source snapshot carries no column lineage
	 */
	private List<ColumnLineageEntry> flattenedLineage(IndexDescription node, String sql, List<ColumnModel> schema,
			Map<IdAndVersion, IndexAuthorizationSnapshot> sources) {
		List<ColumnLineageEntry> immediate = alignToBoundSchema(node.getIdAndVersion(), computeColumns(sql), schema);
		// Index each source's lineage by its object id, so an immediate input naming a source's output
		// column can be replaced by that column's leaf inputs. An empty lineage is a legitimate zero-column
		// source; a missing one is a corrupt capture.
		Map<Long, List<ColumnLineageEntry>> childLineage = new HashMap<>();
		sources.forEach((sourceId, snapshot) -> {
			if (snapshot.getColumnLineage() == null) {
				throw new IllegalStateException("The authorization snapshot of " + sourceId + " has no column lineage");
			}
			childLineage.put(sourceId.getId(), snapshot.getColumnLineage());
		});
		return immediate.stream().map(entry -> flatten(entry, childLineage)).collect(Collectors.toList());
	}

	/**
	 * Pair each computed output column with the bound output column at the same select-list position to
	 * assign its stable id. Both are in select-list order ('select *' expanded identically at bind and
	 * build time), so position - not name - is the pairing key, because output names need not be unique
	 * (e.g. 'select foo, foo'). As defence-in-depth the bound column name is confirmed to still equal the
	 * name the defining SQL produces at each position. A count or name divergence means the bound schema
	 * and the defining SQL have drifted out of sync - a corrupted system invariant, not a bad request -
	 * so fail loudly (HTTP 500) rather than attribute the wrong lineage to a column.
	 */
	private List<ColumnLineageEntry> alignToBoundSchema(IdAndVersion object, List<ComputedColumn> computed,
			List<ColumnModel> boundSchema) {
		if (computed.size() != boundSchema.size()) {
			throw new IllegalStateException("Expected " + boundSchema.size()
					+ " bound columns to match the defining SQL of " + object + " but computed " + computed.size());
		}
		List<ColumnLineageEntry> entries = new ArrayList<>(computed.size());
		for (int i = 0; i < computed.size(); i++) {
			ColumnModel boundColumn = boundSchema.get(i);
			String computedName = computed.get(i).outputName();
			if (!boundColumn.getName().equals(computedName)) {
				throw new IllegalStateException("The bound schema of " + object
						+ " is out of sync with its defining SQL at column " + i + ": bound column '"
						+ boundColumn.getName() + "' but the defining SQL produced '" + computedName + "'");
			}
			entries.add(computed.get(i).entry().setOutputColumnId(boundColumn.getId()));
		}
		return entries;
	}

	/**
	 * Flatten one output column's immediate inputs to leaf source columns by composing against the
	 * as-built lineage of the sources. An input naming a source's output column is replaced by that
	 * column's already-flattened leaf inputs (a base table or view resolves to itself); an input matching no
	 * source column is kept as-is. An
	 * identity column is exactly its single source column, so it inherits that column's derivation; any
	 * other kind dominates and only its inputs are flattened.
	 */
	private ColumnLineageEntry flatten(ColumnLineageEntry entry, Map<Long, List<ColumnLineageEntry>> childLineage) {
		boolean identity = DerivationKind.IDENTITY.equals(entry.getDerivationKind());
		DerivationKind resultKind = entry.getDerivationKind();
		String resultSetFunctionType = entry.getSetFunctionType();
		LinkedHashSet<SourceColumnReference> leafInputs = new LinkedHashSet<>();
		for (SourceColumnReference input : entry.getInputs()) {
			Optional<ColumnLineageEntry> childEntry = resolveChildEntry(input, childLineage);
			if (childEntry.isEmpty()) {
				leafInputs.add(input);
			} else {
				leafInputs.addAll(childEntry.get().getInputs());
				// An identity column is exactly its source column, so it inherits that column's derivation.
				// A UNION-merged identity can resolve several children; the first non-identity dominates
				// (matching the merge rule), so only adopt a child kind while still identity - once a child
				// has made this column non-identity, later children contribute only their leaf inputs.
				if (identity && DerivationKind.IDENTITY.equals(resultKind)) {
					resultKind = childEntry.get().getDerivationKind();
					resultSetFunctionType = childEntry.get().getSetFunctionType();
				}
			}
		}
		return new ColumnLineageEntry()
				.setOutputColumnId(entry.getOutputColumnId())
				.setDerivationKind(resultKind)
				.setSetFunctionType(resultSetFunctionType)
				.setInputs(new ArrayList<>(leafInputs));
	}

	/**
	 * Resolve an immediate input to the source column's own flattened lineage entry among the sources, or
	 * empty when no source lineage carries that column.
	 */
	private Optional<ColumnLineageEntry> resolveChildEntry(SourceColumnReference input,
			Map<Long, List<ColumnLineageEntry>> childLineage) {
		List<ColumnLineageEntry> entries = childLineage.get(IdAndVersion.parse(input.getSourceObjectId()).getId());
		if (entries == null) {
			return Optional.empty();
		}
		return entries.stream()
				.filter(childColumn -> input.getSourceColumnId().equals(childColumn.getOutputColumnId()))
				.findFirst();
	}

	/**
	 * Compute the ordered, immediate lineage of each output column of the defining SQL, 1:1 with
	 * {@link SQLTranslatorUtils#getSchemaOfSelect}. The {@code outputColumnId} of each entry is left
	 * null; it is assigned from the bound schema during flattening.
	 */
	List<ColumnLineageEntry> computeEntries(String definingSql) {
		return computeColumns(definingSql).stream().map(ComputedColumn::entry).collect(Collectors.toList());
	}

	/**
	 * Compute the ordered output columns of the defining SQL, each carrying both its lineage entry and
	 * the output name the SQL produces, 1:1 and in the same order as
	 * {@link SQLTranslatorUtils#getSchemaOfSelect}, so the caller can align them to the bound schema.
	 */
	List<ComputedColumn> computeColumns(String definingSql) {
		QueryExpression model;
		try {
			model = new TableQueryParser(definingSql).queryExpression();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
		model.setSqlContext(SqlContext.build);
		SQLTranslatorUtils.translateDefiningClause(model);

		// Each QuerySpecification is one part; a UNION contributes multiple parts of equal width.
		List<List<ComputedColumn>> perPart = model.stream(QuerySpecification.class)
				.map(this::computeColumnsForPart).collect(Collectors.toList());
		return mergeParts(perPart);
	}

	/**
	 * Compute the output columns of a single query part, resolving each output column's source-column
	 * inputs against the part's tables and capturing the name the SQL produces for it.
	 */
	private List<ComputedColumn> computeColumnsForPart(QuerySpecification part) {
		TableAndColumnMapper mapper = new TableAndColumnMapper(part, tableManagerSupport);
		// A 'select *' carries no explicit columns, so expand it into one column per source column -
		// exactly as QueryTranslator does - before deriving an entry per output column.
		if (Boolean.TRUE.equals(part.getSelectList().getAsterisk())) {
			part.replaceSelectList(mapper.buildSelectAllColumns(), null);
		}
		return part.getSelectList().getColumns().stream()
				.map(column -> new ComputedColumn(SQLTranslatorUtils.getSelectColumns(column, mapper).getName(),
						computeEntry(column, mapper)))
				.collect(Collectors.toList());
	}

	/**
	 * Classify a single output column and collect its immediate source-column inputs.
	 */
	static ColumnLineageEntry computeEntry(DerivedColumn derivedColumn, TableAndColumnMapper mapper) {
		ColumnLineageEntry entry = new ColumnLineageEntry();
		SetFunctionSpecification setFunction = derivedColumn.getFirstElementOfType(SetFunctionSpecification.class);
		List<ColumnReference> columnReferences = derivedColumn.stream(ColumnReference.class).collect(Collectors.toList());

		if (setFunction != null) {
			entry.setDerivationKind(DerivationKind.AGGREGATE);
			entry.setSetFunctionType(setFunction.getSetFunctionType().name());
			// An aggregate's inputs are only the columns whose values it reads, so 'count(*)' resolves to no
			// inputs: it is a function of row cardinality, not of any column value. This is not a lineage
			// hole for QID re-identification. The differencing risk of a count combined with a QID-narrowing
			// filter is governed at query time by the filter column's own lineage (which flattens to the QID
			// leaf) plus the transitive dependency closure (which records the QID-bearing source) - not by
			// the count's select-list lineage - so query-set-size restriction applies there, not here.
		} else if (columnReferences.isEmpty()) {
			entry.setDerivationKind(DerivationKind.LITERAL);
		} else if (isIdentity(derivedColumn, columnReferences)) {
			entry.setDerivationKind(DerivationKind.IDENTITY);
		} else {
			entry.setDerivationKind(DerivationKind.EXPRESSION);
		}

		entry.setInputs(resolveInputs(columnReferences, mapper));
		return entry;
	}

	/**
	 * An output column is an identity when its value is exactly one column reference, unchanged by any
	 * function, cast, or arithmetic. An 'as' alias does not affect this: the alias renames the output
	 * but the value expression is still just the reference.
	 */
	private static boolean isIdentity(DerivedColumn derivedColumn, List<ColumnReference> columnReferences) {
		if (columnReferences.size() != 1) {
			return false;
		}
		return derivedColumn.getValueExpression().toSql().equals(columnReferences.get(0).toSql());
	}

	/**
	 * Resolve each column reference to its source object and source ColumnModel id, de-duplicating and
	 * preserving first-seen order. References that do not resolve to a schema column (row metadata such
	 * as ROW_ID, or references the mapper cannot match) contribute no input.
	 */
	private static List<SourceColumnReference> resolveInputs(List<ColumnReference> columnReferences,
			TableAndColumnMapper mapper) {
		LinkedHashSet<SourceColumnReference> inputs = new LinkedHashSet<>();
		for (ColumnReference columnReference : columnReferences) {
			mapper.lookupColumnReferenceMatch(columnReference)
					.filter(match -> match.getColumnTranslationReference() instanceof SchemaColumnTranslationReference)
					.ifPresent(match -> inputs.add(new SourceColumnReference()
							.setSourceObjectId(KeyFactory.keyToString(match.getTableInfo().getTableIdAndVersion().getId()))
							.setSourceVersionNumber(match.getTableInfo().getTableIdAndVersion().getVersion().orElse(null))
							.setSourceColumnId(((SchemaColumnTranslationReference) match.getColumnTranslationReference()).getId())));
		}
		return new ArrayList<>(inputs);
	}

	/**
	 * Combine the per-part entries into one entry per output column. A single part is returned as-is. A
	 * UNION merges equal-width parts by output position: the inputs are unioned across every branch and,
	 * because any transformation on any branch changes the output, a non-identity kind on any branch
	 * dominates an identity. Parts that do not share the first part's width (for example the inner query
	 * of a common table expression) are ignored, matching {@link SQLTranslatorUtils#createSchemaOfSelect}.
	 */
	static List<ComputedColumn> mergeParts(List<List<ComputedColumn>> perPart) {
		List<ComputedColumn> first = perPart.get(0);
		if (perPart.size() < 2 || perPart.stream().skip(1).anyMatch(part -> part.size() != first.size())) {
			return first;
		}
		List<ComputedColumn> merged = new ArrayList<>(first.size());
		for (int column = 0; column < first.size(); column++) {
			ColumnLineageEntry result = new ColumnLineageEntry().setDerivationKind(DerivationKind.IDENTITY);
			LinkedHashSet<SourceColumnReference> inputs = new LinkedHashSet<>();
			for (List<ComputedColumn> part : perPart) {
				ColumnLineageEntry branch = part.get(column).entry();
				inputs.addAll(branch.getInputs());
				if (result.getDerivationKind() == DerivationKind.IDENTITY
						&& branch.getDerivationKind() != DerivationKind.IDENTITY) {
					result.setDerivationKind(branch.getDerivationKind());
					result.setSetFunctionType(branch.getSetFunctionType());
				}
			}
			// The output name of a UNION is the first branch's name, matching getSchemaOfSelect.
			merged.add(new ComputedColumn(first.get(column).outputName(), result.setInputs(new ArrayList<>(inputs))));
		}
		return merged;
	}

	/**
	 * One output column of the defining SQL: its lineage entry together with the name the SQL produces
	 * for it, so the caller can both align to and validate against the bound schema.
	 */
	record ComputedColumn(String outputName, ColumnLineageEntry entry) {
	}

}
