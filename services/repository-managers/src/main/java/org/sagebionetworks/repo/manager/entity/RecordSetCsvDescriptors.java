package org.sagebionetworks.repo.manager.entity;

import java.util.Optional;

import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

import au.com.bytecode.opencsv.Constants;

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

	/**
	 * Two descriptors that differ only in whether a default is spelled out read
	 * and write a CSV identically, so they must compare equal once each is passed
	 * through this.
	 *
	 * @return a new descriptor equal to the given one (or the default when it is
	 *         null) with every property that is not set filled in with the value
	 *         the CSV is read and written with
	 */
	public static CsvTableDescriptor withDefaults(CsvTableDescriptor csvDescriptor) {
		CsvTableDescriptor descriptor = orDefault(csvDescriptor);
		return new CsvTableDescriptor()
				.setSeparator(Optional.ofNullable(descriptor.getSeparator())
						.orElse(String.valueOf(Constants.DEFAULT_SEPARATOR)))
				.setQuoteCharacter(Optional.ofNullable(descriptor.getQuoteCharacter())
						.orElse(String.valueOf(Constants.DEFAULT_QUOTE_CHARACTER)))
				.setEscapeCharacter(Optional.ofNullable(descriptor.getEscapeCharacter())
						.orElse(String.valueOf(Constants.DEFAULT_ESCAPE_CHARACTER)))
				.setLineEnd(Optional.ofNullable(descriptor.getLineEnd()).orElse(Constants.DEFAULT_LINE_END))
				.setIsFirstLineHeader(Optional.ofNullable(descriptor.getIsFirstLineHeader()).orElse(true));
	}
}
