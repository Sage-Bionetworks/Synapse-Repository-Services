package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.schema.ValidationResults;

public class RowValidation  {

	// the ID of the constant that contains the validation results.
	private LogicalTimestamp constantId;
	private ValidationResults validationResults;

	public LogicalTimestamp getConstantId() {
		return constantId;
	}

	public RowValidation setConstantId(LogicalTimestamp constantId) {
		this.constantId = constantId;
		return this;
	}

	public ValidationResults getValidationResults() {
		return validationResults;
	}

	public RowValidation setValidationResults(ValidationResults validationResults) {
		this.validationResults = validationResults;
		return this;
	}

	public List<LogicalTimestamp> getConstantIds() {
		return constantId == null ? Collections.emptyList() : List.of(constantId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(constantId, validationResults);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RowValidation other = (RowValidation) obj;
		return Objects.equals(constantId, other.constantId)
				&& Objects.equals(validationResults, other.validationResults);
	}

	@Override
	public String toString() {
		return "RowValidation [constantId=" + constantId + ", validationResults=" + validationResults + "]";
	}

}
