package org.sagebionetworks.util.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}