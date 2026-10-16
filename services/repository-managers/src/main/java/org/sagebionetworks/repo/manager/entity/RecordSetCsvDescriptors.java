package org.sagebionetworks.repo.manager.entity;

import java.util.Optional;

import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

/**
 * The CSV parsing rules a RecordSet revision is read with. A revision that
 * records no descriptor is parsed with the default, so every reader and every
 * comparison of two revisions must resolve a null descriptor the same way -
 * otherwise two revisions that parse identically can look different.
 */
public class RecordSetCsvDescriptors {

	public static final CsvTableDescriptor DEFAULT_RECORD_SET_CSV_DESCRIPTOR = new CsvTableDescriptor()
			.setIsFirstLineHeader(true);

	/**
	 * @return the descriptor the given RecordSet's CSV is parsed with, which is the
	 *         default when the RecordSet records none
	 */
	public static CsvTableDescriptor orDefault(RecordSet recordSet) {
		return orDefault(recordSet.getCsvDescriptor());
	}

	/**
	 * @return the given descriptor, or the default when it is null
	 */
	public static CsvTableDescriptor orDefault(CsvTableDescriptor csvDescriptor) {
		return Optional.ofNullable(csvDescriptor).orElse(DEFAULT_RECORD_SET_CSV_DESCRIPTOR);
	}
}
