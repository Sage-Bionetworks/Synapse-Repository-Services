package org.sagebionetworks.table.cluster.resolution;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.sagebionetworks.repo.model.table.DerivationKind;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.SetFunctionType;
import org.sagebionetworks.util.ValidateArgument;

/**
 * A select column of a query together with how its value is derived and the columns it reads.
 */
public final class ResolvedColumn {

	private final DerivedColumn element;
	private final String outputName;
	private final DerivationKind kind;
	private final SetFunctionType setFunctionType;
	private final boolean wholeCount;
	private final List<ColumnSource> inputs;

	/**
	 * @param element         the select column in the parsed query.
	 * @param outputName      the name the query produces for the column.
	 * @param kind            how the value is derived from the columns it reads.
	 * @param setFunctionType the aggregate that computes the value. May be null when the kind is not
	 *                        {@link DerivationKind#AGGREGATE}.
	 * @param wholeCount      true when the entire value of the column is a single {@code COUNT}.
	 * @param inputs          each schema column the value reads, in first-seen order.
	 */
	public ResolvedColumn(DerivedColumn element, String outputName, DerivationKind kind,
			SetFunctionType setFunctionType, boolean wholeCount, List<ColumnSource> inputs) {
		ValidateArgument.required(element, "element");
		ValidateArgument.required(outputName, "outputName");
		ValidateArgument.required(kind, "kind");
		ValidateArgument.required(inputs, "inputs");
		this.element = element;
		this.outputName = outputName;
		this.kind = kind;
		this.setFunctionType = setFunctionType;
		this.wholeCount = wholeCount;
		this.inputs = List.copyOf(inputs);
	}

	/**
	 * @return the select column in the parsed query. This is the same object as in the query, so it
	 *         identifies the column without relying on its position in the select list.
	 */
	public DerivedColumn getElement() {
		return element;
	}

	/**
	 * @return the name the query produces for the column. When the query is the defining SQL of an object,
	 *         this is the name of the object's bound column.
	 */
	public String getOutputName() {
		return outputName;
	}

	/**
	 * @return how the value is derived from the columns it reads.
	 */
	public DerivationKind getKind() {
		return kind;
	}

	/**
	 * @return the aggregate that computes the value, or empty when the kind is not
	 *         {@link DerivationKind#AGGREGATE}.
	 */
	public Optional<SetFunctionType> getSetFunctionType() {
		return Optional.ofNullable(setFunctionType);
	}

	/**
	 * @return true when the entire value of the column is a single {@code COUNT}, so it can be suppressed as
	 *         a count.
	 */
	public boolean isWholeCount() {
		return wholeCount;
	}

	/**
	 * @return each schema column the value reads, de-duplicated in first-seen order. Row metadata columns
	 *         are excluded.
	 */
	public List<ColumnSource> getInputs() {
		return inputs;
	}

	@Override
	public int hashCode() {
		return Objects.hash(element, outputName, kind, setFunctionType, wholeCount, inputs);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ResolvedColumn)) {
			return false;
		}
		ResolvedColumn other = (ResolvedColumn) obj;
		return Objects.equals(element, other.element) && Objects.equals(outputName, other.outputName)
				&& kind == other.kind && setFunctionType == other.setFunctionType && wholeCount == other.wholeCount
				&& Objects.equals(inputs, other.inputs);
	}

	@Override
	public String toString() {
		return "ResolvedColumn [element=" + element.toSql() + ", outputName=" + outputName + ", kind=" + kind
				+ ", setFunctionType=" + setFunctionType + ", wholeCount=" + wholeCount + ", inputs=" + inputs + "]";
	}

}
