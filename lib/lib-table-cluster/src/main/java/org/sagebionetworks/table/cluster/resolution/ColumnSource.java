package org.sagebionetworks.table.cluster.resolution;

import java.util.Objects;
import java.util.Optional;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReference;
import org.sagebionetworks.util.ValidateArgument;

/**
 * The column that a {@link org.sagebionetworks.table.query.model.ColumnReference} reads.
 */
public final class ColumnSource {

	private final IdAndVersion table;
	private final ColumnTranslationReference reference;
	private final ResolvedColumn definition;

	/**
	 * @param table      the object whose column is read, as named in the query's FROM clause.
	 * @param reference  the column of that object's schema (or the row metadata column) that is read.
	 * @param definition the select column of an inlined defining SQL that produces the column. May be
	 *                   null when the table's defining SQL is not inlined in the query.
	 */
	public ColumnSource(IdAndVersion table, ColumnTranslationReference reference, ResolvedColumn definition) {
		ValidateArgument.required(table, "table");
		ValidateArgument.required(reference, "reference");
		this.table = table;
		this.reference = reference;
		this.definition = definition;
	}

	/**
	 * @return the object whose column is read, as named in the query's FROM clause.
	 */
	public IdAndVersion getTable() {
		return table;
	}

	/**
	 * @return the column of the table's schema (or the row metadata column) that is read.
	 */
	public ColumnTranslationReference getReference() {
		return reference;
	}

	/**
	 * @return the id of the ColumnModel read, or empty for a row metadata column.
	 */
	public Optional<String> getColumnId() {
		return reference.getColumnId();
	}

	/**
	 * @return the select column of the inlined defining SQL that produces this column, or empty when the
	 *         table's defining SQL is not inlined in the query.
	 */
	public Optional<ResolvedColumn> getDefinition() {
		return Optional.ofNullable(definition);
	}

	@Override
	public int hashCode() {
		return Objects.hash(table, reference.getUserQueryColumnName(), reference.getColumnId(), definition);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ColumnSource)) {
			return false;
		}
		ColumnSource other = (ColumnSource) obj;
		return Objects.equals(table, other.table)
				&& Objects.equals(reference.getUserQueryColumnName(), other.reference.getUserQueryColumnName())
				&& Objects.equals(reference.getColumnId(), other.reference.getColumnId())
				&& Objects.equals(definition, other.definition);
	}

	@Override
	public String toString() {
		return "ColumnSource [table=" + table + ", column=" + reference.getUserQueryColumnName() + ", columnId="
				+ reference.getColumnId().orElse(null) + ", definition=" + definition + "]";
	}

}
