package org.sagebionetworks.util.csv;

import java.io.Writer;

import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

import au.com.bytecode.opencsv.CSVWriter;
import au.com.bytecode.opencsv.Constants;

public class CSVWriterProviderImpl implements CSVWriterProvider {

	@Override
	public CSVWriter createWriter(Writer writer, CsvTableDescriptor csvTableDescriptor) {
        char separator = Constants.DEFAULT_SEPARATOR;
        char quotechar = Constants.DEFAULT_QUOTE_CHARACTER;
        char escape = Constants.DEFAULT_ESCAPE_CHARACTER;
        String lineEnd = Constants.DEFAULT_LINE_END;
        if (csvTableDescriptor != null) {
            if (csvTableDescriptor.getSeparator() != null) {
                if (csvTableDescriptor.getSeparator().length() != 1) {
                    throw new IllegalArgumentException("CsvTableDescriptor.separator must be exactly one character.");
                }
                separator = csvTableDescriptor.getSeparator().charAt(0);
            }
            if (csvTableDescriptor.getQuoteCharacter() != null) {
                if (csvTableDescriptor.getQuoteCharacter().length() != 1) {
                    throw new IllegalArgumentException("CsvTableDescriptor.quoteCharacter must be exactly one character.");
                }
                quotechar = csvTableDescriptor.getQuoteCharacter().charAt(0);
            }
            if (csvTableDescriptor.getEscapeCharacter() != null) {
                if (csvTableDescriptor.getEscapeCharacter().length() != 1) {
                    throw new IllegalArgumentException("CsvTableDescriptor.escapeCharacter must be exactly one character.");
                }
                escape = csvTableDescriptor.getEscapeCharacter().charAt(0);
            }
            if (csvTableDescriptor.getLineEnd() != null) {
                lineEnd = csvTableDescriptor.getLineEnd();
            }
        }
        // Create the reader.
        return new CSVWriter(writer, separator, quotechar, escape, lineEnd);
    }

}