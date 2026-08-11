package org.sagebionetworks.repo.manager.grid.internal.replica.validation;

import java.io.IOException;
import java.util.Date;

import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Accumulates the per-row validation outcome of a grid export/push into both the
 * user-visible validation-details CSV and the aggregate
 * {@link ValidationSummaryStatistics}.
 */
public class ValidationSummaryAccumulator {

	public static final String[] VALIDATION_CSV_HEADERS = new String[] {
		"row_index", "is_valid", "validation_error_message", "all_validation_messages"
	};

	private final CSVWriter validationCsvWriter;

	private int totalCount = 0;
	private int validCount = 0;
	private int invalidCount = 0;
	private int unknownCount = 0;

	/**
	 * @param validationCsvWriter writer for the validation-details CSV; the header
	 *                            row is written here on construction
	 */
	public ValidationSummaryAccumulator(CSVWriter validationCsvWriter) throws IOException {
		this.validationCsvWriter = validationCsvWriter;
		this.validationCsvWriter.writeNext(VALIDATION_CSV_HEADERS);
	}

	/**
	 * Record one row's validation outcome: increment the appropriate counter and
	 * write the row's details. A {@code null} result counts as "unknown" (no bound
	 * schema, so the row could not be validated).
	 *
	 * @param result the row's validation result, or null if unknown
	 */
	public void record(ValidationResults result) {
		// row_index, is_valid, validation_error_message, all_validation_messages
		String[] details = new String[] { String.valueOf(totalCount), null, null, null };
		totalCount++;
		if (result == null) {
			unknownCount++;
		} else if (Boolean.TRUE.equals(result.getIsValid())) {
			validCount++;
			details[1] = "true";
		} else {
			invalidCount++;
			details[1] = "false";
			details[2] = result.getValidationErrorMessage();
			details[3] = JDOSecondaryPropertyUtils.writeStringListToJson(result.getAllValidationMessages());
		}
		try {
			validationCsvWriter.writeNext(details);
		} catch (IOException e) {
			throw new IllegalStateException("Could not write validation details to CSV file.", e);
		}
	}

	/**
	 * @param containerId the id to set as the summary's container (the RecordSet id)
	 * @return the accumulated validation summary
	 */
	public ValidationSummaryStatistics getValidationSummary(String containerId) {
		return new ValidationSummaryStatistics()
			.setContainerId(containerId)
			.setTotalNumberOfChildren(Long.valueOf(totalCount))
			.setNumberOfValidChildren(Long.valueOf(validCount))
			.setNumberOfInvalidChildren(Long.valueOf(invalidCount))
			.setNumberOfUnknownChildren(Long.valueOf(unknownCount))
			.setGeneratedOn(new Date());
	}
}
