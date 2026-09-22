package org.sagebionetworks.repo.manager.table;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dao.table.ColumnProvenanceDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnProvenance;
import org.sagebionetworks.repo.model.table.ColumnProvenanceEntry;
import org.sagebionetworks.repo.model.table.DerivationKind;
import org.sagebionetworks.repo.model.table.SourceColumnReference;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.table.cluster.SQLTranslatorUtils;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.cluster.columntranslation.SchemaColumnTranslationReference;
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
 * Provides the immediate column-level lineage of a defining-SQL object (MaterializedView,
 * VirtualTable, SearchIndex): for each output column, how it derives from the columns of the
 * object's immediate sources, by stable ColumnModel id rather than name. The document is derived
 * from the defining SQL and the object's bound schema, and cached in a non-migrated table that is
 * populated lazily on a read miss and cleared when the object's schema is re-bound.
 */
@Service
public class ColumnProvenanceManager {

	private final ColumnProvenanceDao columnProvenanceDao;
	private final NodeDAO nodeDao;
	private final ColumnModelManager columnModelManager;
	private final TableManagerSupport tableManagerSupport;

	public ColumnProvenanceManager(ColumnProvenanceDao columnProvenanceDao, NodeDAO nodeDao,
			ColumnModelManager columnModelManager, TableManagerSupport tableManagerSupport) {
		this.columnProvenanceDao = columnProvenanceDao;
		this.nodeDao = nodeDao;
		this.columnModelManager = columnModelManager;
		this.tableManagerSupport = tableManagerSupport;
	}

	/**
	 * Get the immediate column provenance for a defining-SQL object, computing and caching it on the
	 * first read.
	 *
	 * @param object the object version to describe.
	 * @return the provenance document, or empty when the object is not a defining-SQL object (a leaf
	 *         table or a view without defining SQL has no column-level lineage to record).
	 */
	public Optional<ColumnProvenance> getColumnProvenance(IdAndVersion object) {
		ValidateArgument.required(object, "object");
		Optional<ColumnProvenance> cached = columnProvenanceDao.getColumnProvenance(object);
		if (cached.isPresent()) {
			return cached;
		}
		Optional<String> definingSql = nodeDao.getDefiningSql(object);
		if (definingSql.isEmpty()) {
			return Optional.empty();
		}
		ColumnProvenance provenance = computeColumnProvenance(object, definingSql.get());
		columnProvenanceDao.saveColumnProvenance(object, provenance);
		return Optional.of(provenance);
	}

	/**
	 * Bind the given output schema to a defining-SQL object and discard any cached provenance in one
	 * step. This is the single entry point every defining-SQL entity (MaterializedView, VirtualTable,
	 * SearchIndex) uses to (re)bind its schema, so that a new entity type cannot bind a schema and then
	 * forget to invalidate the now-stale lineage.
	 *
	 * @param schemaIds the ordered output column ids to bind.
	 * @param object    the object version being (re)bound.
	 */
	@WriteTransaction
	public void bindSchemaAndInvalidate(List<String> schemaIds, IdAndVersion object) {
		columnModelManager.bindColumnsToVersionOfObject(schemaIds, object);
		invalidate(object);
	}

	/**
	 * Discard any cached provenance for the object so it is recomputed on the next read. Called when
	 * the object's schema is re-bound, since an in-place redefinition can change the lineage.
	 *
	 * @param object the object version to invalidate.
	 */
	public void invalidate(IdAndVersion object) {
		ValidateArgument.required(object, "object");
		columnProvenanceDao.clear(object);
	}

	/**
	 * Build the provenance document by pairing each computed entry with the object's bound output
	 * column id (both are in select-list order).
	 */
	ColumnProvenance computeColumnProvenance(IdAndVersion object, String definingSql) {
		List<ComputedColumn> computed = computeColumns(definingSql);
		List<ColumnModel> boundColumns = columnModelManager.getTableSchema(object);
		// Each computed column is paired with the object's bound output column at the same select-list
		// position: both the computed columns and the bound schema are in select-list order (bound
		// columns by ordinal, computed columns by walk order, with 'select *' expanded here exactly as
		// at bind time). Position - not name - is the pairing key, because output names need not be
		// unique (e.g. 'select foo, foo'). The bound schema was itself created from getSchemaOfSelect at
		// bind time, so as a defence-in-depth invariant we also confirm the bound column name still
		// equals the name the defining SQL produces at each position. A count or name divergence means
		// the bound schema and the defining SQL have drifted out of sync - a corrupted system invariant,
		// not a bad request - so fail loudly with an IllegalStateException (HTTP 500) rather than
		// attribute the wrong lineage to a column.
		if (computed.size() != boundColumns.size()) {
			throw new IllegalStateException("Expected " + boundColumns.size()
					+ " bound columns to match the defining SQL of " + object + " but computed " + computed.size());
		}
		List<ColumnProvenanceEntry> columns = new ArrayList<>(computed.size());
		for (int i = 0; i < computed.size(); i++) {
			ColumnModel boundColumn = boundColumns.get(i);
			String computedName = computed.get(i).outputName();
			if (!boundColumn.getName().equals(computedName)) {
				throw new IllegalStateException("The bound schema of " + object
						+ " is out of sync with its defining SQL at column " + i + ": bound column '"
						+ boundColumn.getName() + "' but the defining SQL produced '" + computedName + "'");
			}
			columns.add(computed.get(i).entry().setOutputColumnId(boundColumn.getId()));
		}
		return new ColumnProvenance()
				.setObjectId("syn" + object.getId())
				.setVersionNumber(object.getVersion().orElse(null))
				.setColumns(columns);
	}

	/**
	 * Compute the ordered, immediate provenance of each output column of the defining SQL, 1:1 with
	 * {@link SQLTranslatorUtils#getSchemaOfSelect}. The {@code outputColumnId} of each entry is left
	 * null; it is assigned by the caller from the bound schema.
	 */
	List<ColumnProvenanceEntry> computeEntries(String definingSql) {
		return computeColumns(definingSql).stream().map(ComputedColumn::entry).collect(Collectors.toList());
	}

	/**
	 * Compute the ordered output columns of the defining SQL, each carrying both its provenance entry
	 * and the output name the SQL produces (1:1 and in the same order as
	 * {@link SQLTranslatorUtils#getSchemaOfSelect}, so the caller can align them to the bound schema).
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
	static ColumnProvenanceEntry computeEntry(DerivedColumn derivedColumn, TableAndColumnMapper mapper) {
		ColumnProvenanceEntry entry = new ColumnProvenanceEntry();
		SetFunctionSpecification setFunction = derivedColumn.getFirstElementOfType(SetFunctionSpecification.class);
		List<ColumnReference> columnReferences = derivedColumn.stream(ColumnReference.class).collect(Collectors.toList());

		if (setFunction != null) {
			entry.setDerivationKind(DerivationKind.AGGREGATE);
			entry.setSetFunctionType(setFunction.getSetFunctionType().name());
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
							.setSourceObjectId("syn" + match.getTableInfo().getTableIdAndVersion().getId())
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
			ColumnProvenanceEntry result = new ColumnProvenanceEntry().setDerivationKind(DerivationKind.IDENTITY);
			LinkedHashSet<SourceColumnReference> inputs = new LinkedHashSet<>();
			for (List<ComputedColumn> part : perPart) {
				ColumnProvenanceEntry branch = part.get(column).entry();
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
	 * One output column of the defining SQL: its provenance entry together with the name the SQL
	 * produces for it, so the caller can both align to and validate against the bound schema.
	 */
	record ComputedColumn(String outputName, ColumnProvenanceEntry entry) {
	}

}
