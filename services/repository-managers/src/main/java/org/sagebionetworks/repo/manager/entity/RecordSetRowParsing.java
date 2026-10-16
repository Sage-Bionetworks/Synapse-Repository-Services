package org.sagebionetworks.repo.manager.entity;

import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

/**
 * The per-revision fields of a RecordSet that govern how the revision's CSV is
 * parsed into rows and how those rows are keyed. Everything that compares two
 * revisions - auto-versioning, grid synchronization and the queryable index -
 * reads them off the revision, so any new per-revision RecordSet field that
 * governs parsing or keying belongs here.
 *
 * @param upsertKey     the columns that key the revision's rows
 * @param csvDescriptor the descriptor the revision records, or null if it
 *                      records none
 */
public record RecordSetRowParsing(List<String> upsertKey, CsvTableDescriptor csvDescriptor) {

	public static RecordSetRowParsing of(RecordSet recordSet) {
		return new RecordSetRowParsing(recordSet.getUpsertKey(), recordSet.getCsvDescriptor());
	}

	public static RecordSetRowParsing of(Node node) {
		return new RecordSetRowParsing(node.getUpsertKey(), node.getCsvDescriptor());
	}

	/**
	 * @return true if rows would be matched by a different key than in the given
	 *         parsing
	 */
	public boolean isUpsertKeyChangedFrom(RecordSetRowParsing other) {
		return !Objects.equals(upsertKey, other.upsertKey);
	}

	/**
	 * Descriptors are compared with their defaults filled in, so that spelling out
	 * a default is not mistaken for a change to how the CSV is read.
	 *
	 * @return true if the same bytes would be read differently than with the given
	 *         parsing
	 */
	public boolean isCsvDescriptorChangedFrom(RecordSetRowParsing other) {
		return !RecordSetCsvDescriptors.withDefaults(csvDescriptor)
				.equals(RecordSetCsvDescriptors.withDefaults(other.csvDescriptor));
	}

	/**
	 * @return true if the same bytes would yield different keyed rows than with
	 *         the given parsing
	 */
	public boolean isChangedFrom(RecordSetRowParsing other) {
		return isUpsertKeyChangedFrom(other) || isCsvDescriptorChangedFrom(other);
	}
}
