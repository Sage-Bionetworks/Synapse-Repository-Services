package org.sagebionetworks.util.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

import au.com.bytecode.opencsv.CSVWriter;
import au.com.bytecode.opencsv.Constants;

public class CSVWriterProviderImplTest {

    private final CSVWriterProviderImpl csvWriterProvider = new CSVWriterProviderImpl();


    @Test
    public void testCreateCSVWriterAllDefaults(){
        CsvTableDescriptor csvTableDescriptor = null;
        StringWriter reader = new StringWriter();
        // call under test
        CSVWriter csvWriter = csvWriterProvider.createWriter(reader, csvTableDescriptor);
        assertNotNull(csvWriter);
        assertEquals(Constants.DEFAULT_SEPARATOR, csvWriter.getSeparator());
        assertEquals(Constants.DEFAULT_ESCAPE_CHARACTER, csvWriter.getEscapechar());
        assertEquals(Constants.DEFAULT_QUOTE_CHARACTER, csvWriter.getQuotechar());
        assertEquals(Constants.DEFAULT_LINE_END, csvWriter.getLineEnd());
    }

    @Test
    public void testCreateCSVWriterTabSeperator(){
        CsvTableDescriptor csvTableDescriptor = new CsvTableDescriptor().setSeparator("\t");
        StringWriter reader = new StringWriter();
        // call under test
        CSVWriter csvWriter = csvWriterProvider.createWriter(reader, csvTableDescriptor);
        assertNotNull(csvWriter);
        assertEquals('\t', csvWriter.getSeparator());
        assertEquals(Constants.DEFAULT_ESCAPE_CHARACTER, csvWriter.getEscapechar());
        assertEquals(Constants.DEFAULT_QUOTE_CHARACTER, csvWriter.getQuotechar());
        assertEquals(Constants.DEFAULT_LINE_END, csvWriter.getLineEnd());
    }

    @Test
    public void testCreateCSVWriterEscapse(){
        CsvTableDescriptor csvTableDescriptor = new CsvTableDescriptor().setEscapeCharacter("|");
        StringWriter reader = new StringWriter();
        // call under test
        CSVWriter csvWriter = csvWriterProvider.createWriter(reader, csvTableDescriptor);
        assertNotNull(csvWriter);
        assertEquals(Constants.DEFAULT_SEPARATOR, csvWriter.getSeparator());
        assertEquals('|', csvWriter.getEscapechar());
        assertEquals(Constants.DEFAULT_QUOTE_CHARACTER, csvWriter.getQuotechar());
        assertEquals(Constants.DEFAULT_LINE_END, csvWriter.getLineEnd());
    }

    @Test
    public void testCreateCSVWriterQuote(){
        CsvTableDescriptor csvTableDescriptor = new CsvTableDescriptor().setQuoteCharacter("'");
        StringWriter reader = new StringWriter();
        // call under test
        CSVWriter csvWriter = csvWriterProvider.createWriter(reader, csvTableDescriptor);
        assertNotNull(csvWriter);
        assertEquals(Constants.DEFAULT_SEPARATOR, csvWriter.getSeparator());
        assertEquals(Constants.DEFAULT_ESCAPE_CHARACTER, csvWriter.getEscapechar());
        assertEquals('\'', csvWriter.getQuotechar());
        assertEquals(Constants.DEFAULT_LINE_END, csvWriter.getLineEnd());
    }

    @Test
    public void testCreateCSVWriterLineEnd(){
        CsvTableDescriptor csvTableDescriptor = new CsvTableDescriptor().setLineEnd("\t");
        StringWriter reader = new StringWriter();
        // call under test
        CSVWriter csvWriter = csvWriterProvider.createWriter(reader, csvTableDescriptor);
        assertNotNull(csvWriter);
        assertEquals(Constants.DEFAULT_SEPARATOR, csvWriter.getSeparator());
        assertEquals(Constants.DEFAULT_ESCAPE_CHARACTER, csvWriter.getEscapechar());
        assertEquals(Constants.DEFAULT_QUOTE_CHARACTER, csvWriter.getQuotechar());
        assertEquals("\t", csvWriter.getLineEnd());
    }

    @Test
    public void testCreateCSVWriterSeperatorOverLimit(){
        CsvTableDescriptor csvTableDescriptor = new CsvTableDescriptor().setSeparator("too long");
        StringWriter reader = new StringWriter();
        String message = assertThrows(IllegalArgumentException.class, ()->{
            // call under test
            csvWriterProvider.createWriter(reader, csvTableDescriptor);
        }).getMessage();
        assertEquals("CsvTableDescriptor.separator must be exactly one character.", message);
    }

    @Test
    public void testCreateCSVWriterEscapeOverLimit(){
        CsvTableDescriptor csvTableDescriptor = new CsvTableDescriptor().setEscapeCharacter("too long");
        StringWriter reader = new StringWriter();
        String message = assertThrows(IllegalArgumentException.class, ()->{
            // call under test
            csvWriterProvider.createWriter(reader, csvTableDescriptor);
        }).getMessage();
        assertEquals("CsvTableDescriptor.escapeCharacter must be exactly one character.", message);
    }

    @Test
    public void testCreateCSVWriterQuoteOverLimit(){
        CsvTableDescriptor csvTableDescriptor = new CsvTableDescriptor().setQuoteCharacter("too long");
        StringWriter reader = new StringWriter();
        String message = assertThrows(IllegalArgumentException.class, ()->{
            // call under test
            csvWriterProvider.createWriter(reader, csvTableDescriptor);
        }).getMessage();
        assertEquals("CsvTableDescriptor.quoteCharacter must be exactly one character.", message);
    }

    @Test
    public void testCreateCSVWriterWithExplicitEscapeUsesEscapeForQuote() throws IOException {
        // descriptor explicitly sets escapeCharacter — the writer must use it to escape embedded quote chars
        CsvTableDescriptor csvTableDescriptor = new CsvTableDescriptor()
                .setQuoteCharacter("'")
                .setEscapeCharacter("/");
        StringWriter sw = new StringWriter();
        // call under test
        try (CSVWriter csvWriter = csvWriterProvider.createWriter(sw, csvTableDescriptor)) {
            csvWriter.writeNext(new String[] { "it's_test.csv" });
        }
        assertEquals("'it/'s_test.csv'" + Constants.DEFAULT_LINE_END, sw.toString());
    }

    @Test
    public void testCreateCSVWriterWithoutExplicitEscapePreservesLegacyDoubling() throws IOException {
        // descriptor leaves escapeCharacter null — embedded quote must still be doubled (RFC 4180),
        // matching the byte-for-byte output every existing CSV consumer depends on
        CsvTableDescriptor csvTableDescriptor = new CsvTableDescriptor().setQuoteCharacter("'");
        StringWriter sw = new StringWriter();
        // call under test
        try (CSVWriter csvWriter = csvWriterProvider.createWriter(sw, csvTableDescriptor)) {
            csvWriter.writeNext(new String[] { "it's_test.csv" });
        }
        assertEquals("'it''s_test.csv'" + Constants.DEFAULT_LINE_END, sw.toString());
    }

    @Test
    public void testCreateCSVWriterWithNullDescriptorPreservesLegacyDoubling() throws IOException {
        // null descriptor — defaults across the board. Default quote char is '"', so an embedded
        // '"' must be doubled (RFC 4180). Built from a char constant rather than escaped string
        // literals so the assertion stays readable.
        //   input:    she said "hi"
        //   expected: "she said ""hi"""
        char q = Constants.DEFAULT_QUOTE_CHARACTER;
        String input = "she said " + q + "hi" + q;
        String expected = "" + q + "she said " + q + q + "hi" + q + q + q + Constants.DEFAULT_LINE_END;
        StringWriter sw = new StringWriter();
        // call under test
        try (CSVWriter csvWriter = csvWriterProvider.createWriter(sw, null)) {
            csvWriter.writeNext(new String[] { input });
        }
        assertEquals(expected, sw.toString());
    }
}