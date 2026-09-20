package org.sagebionetworks.repo.manager.table;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dao.table.ColumnProvenanceDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnProvenance;
import org.sagebionetworks.repo.model.table.ColumnProvenanceEntry;
import org.sagebionetworks.repo.model.table.DerivationKind;
import org.sagebionetworks.repo.model.table.SourceColumnReference;
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
		List<ColumnProvenanceEntry> entries = computeEntries(definingSql);
		List<String> outputColumnIds = columnModelManager.getColumnIdsForTable(object);
		int columnCount = Math.min(entries.size(), outputColumnIds.size());
		List<ColumnProvenanceEntry> columns = new ArrayList<>(columnCount);
		for (int i = 0; i < columnCount; i++) {
			columns.add(entries.get(i).setOutputColumnId(outputColumnIds.get(i)));
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
		QueryExpression model;
		try {
			model = new TableQueryParser(definingSql).queryExpression();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
		model.setSqlContext(SqlContext.build);
		SQLTranslatorUtils.translateDefiningClause(model);

		// Each QuerySpecification is one part; a UNION contributes multiple parts of equal width.
		List<List<ColumnProvenanceEntry>> perPart = model.stream(QuerySpecification.class)
				.map(this::computeEntriesForPart).collect(Collectors.toList());
		return mergeParts(perPart);
	}

	/**
	 * Compute the provenance of a single query part, resolving each output column's source-column
	 * inputs against the part's tables.
	 */
	private List<ColumnProvenanceEntry> computeEntriesForPart(QuerySpecification part) {
		TableAndColumnMapper mapper = new TableAndColumnMapper(part, tableManagerSupport);
		// A 'select *' carries no explicit columns, so expand it into one column per source column -
		// exactly as QueryTranslator does - before deriving an entry per output column.
		if (Boolean.TRUE.equals(part.getSelectList().getAsterisk())) {
			part.replaceSelectList(mapper.buildSelectAllColumns(), null);
		}
		return part.getSelectList().getColumns().stream().map(column -> computeEntry(column, mapper))
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
	static List<ColumnProvenanceEntry> mergeParts(List<List<ColumnProvenanceEntry>> perPart) {
		List<ColumnProvenanceEntry> first = perPart.get(0);
		if (perPart.size() < 2 || perPart.stream().skip(1).anyMatch(part -> part.size() != first.size())) {
			return first;
		}
		List<ColumnProvenanceEntry> merged = new ArrayList<>(first.size());
		for (int column = 0; column < first.size(); column++) {
			ColumnProvenanceEntry result = new ColumnProvenanceEntry().setDerivationKind(DerivationKind.IDENTITY);
			LinkedHashSet<SourceColumnReference> inputs = new LinkedHashSet<>();
			for (List<ColumnProvenanceEntry> part : perPart) {
				ColumnProvenanceEntry branch = part.get(column);
				inputs.addAll(branch.getInputs());
				if (result.getDerivationKind() == DerivationKind.IDENTITY
						&& branch.getDerivationKind() != DerivationKind.IDENTITY) {
					result.setDerivationKind(branch.getDerivationKind());
					result.setSetFunctionType(branch.getSetFunctionType());
				}
			}
			merged.add(result.setInputs(new ArrayList<>(inputs)));
		}
		return merged;
	}

}
