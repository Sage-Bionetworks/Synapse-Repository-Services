package org.sagebionetworks.util.csv;

import java.io.Writer;

import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

import au.com.bytecode.opencsv.CSVWriter;

public interface CSVWriterProvider {

	/**
	 * Abstraction to create a new {@link CSVWriter}
	 * @param fileWriter
	 * @param csvTableDescriptor
	 * @return
	 */
	CSVWriter createWriter(Writer fileWriter, CsvTableDescriptor csvTableDescriptor);

}