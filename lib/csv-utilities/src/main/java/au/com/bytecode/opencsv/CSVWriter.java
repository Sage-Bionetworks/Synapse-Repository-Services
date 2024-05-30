package au.com.bytecode.opencsv;

/**
 Copyright 2005 Bytecode Pty Ltd.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * A very simple CSV writer released under a commercial-friendly license.
 *
 * @author Glen Smith
 *
 */
public class CSVWriter implements Closeable {
    
    public static final int INITIAL_STRING_SIZE = 128;

	private Writer rawWriter;

    private char separator;

    private char quotechar;
    
    private char escapechar;
    
    private String lineEnd;

    private ResultSetHelper resultService = new ResultSetHelperService();
    
    /**
     * Constructs CSVWriter using a comma for the separator.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     */
    public CSVWriter(Writer writer) {
        this(writer, Constants.DEFAULT_SEPARATOR);
    }

    /**
     * Constructs CSVWriter with supplied separator.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries.
     */
    public CSVWriter(Writer writer, char separator) {
        this(writer, separator, Constants.DEFAULT_QUOTE_CHARACTER);
    }

    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     */
    public CSVWriter(Writer writer, char separator, char quotechar) {
    	this(writer, separator, quotechar, Constants.DEFAULT_ESCAPE_CHARACTER);
    }

    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     * @param escapechar
     *            the character to use for escaping quotechars or escapechars
     */
    public CSVWriter(Writer writer, char separator, char quotechar, char escapechar) {
        this(writer, separator, quotechar, escapechar, Constants.DEFAULT_LINE_END);
    }
    
    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     * @param lineEnd
     * 			  the line feed terminator to use
     */
    public CSVWriter(Writer writer, char separator, char quotechar, String lineEnd) {
        this(writer, separator, quotechar, Constants.DEFAULT_ESCAPE_CHARACTER, lineEnd);
    }
    
    /**
     * Constructs CSVWriter with supplied separator, quote char, escape char and line ending.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     * @param escapechar
     *            the character to use for escaping quotechars or escapechars
     * @param lineEnd
     * 			  the line feed terminator to use
     */
    public CSVWriter(Writer writer, char separator, char quotechar, char escapechar, String lineEnd) {
        this.rawWriter = writer;
        this.separator = separator;
        this.quotechar = quotechar;
        this.escapechar = escapechar;
        this.lineEnd = lineEnd;
    }
    
    /**
     * Writes the entire list to a CSV file. The list is assumed to be a
     * String[]
     *
     * @param allLines
     *            a List of String[], with each String[] representing a line of
     *            the file.
     * @throws IOException 
     */
    public void writeAll(List<String[]> allLines) throws IOException  {
    	for (String[] line : allLines) {
			writeNext(line);
		}
    }

    protected void writeColumnNames(ResultSet rs)
    	throws SQLException, IOException {

    	writeNext(resultService.getColumnNames(rs));
    }
    
    /**
     * Writes the entire ResultSet to a CSV file.
     *
     * The caller is responsible for closing the ResultSet.
     *
     * @param rs the recordset to write
     * @param includeColumnNames true if you want column names in the output, false otherwise
     *
     * @throws java.io.IOException thrown by getColumnValue
     * @throws java.sql.SQLException thrown by getColumnValue
     */
    public void writeAll(java.sql.ResultSet rs, boolean includeColumnNames)  throws SQLException, IOException {
    	
    	
    	if (includeColumnNames) {
			writeColumnNames(rs);
		}
    	
    	while (rs.next())
    	{
    		writeNext(resultService.getColumnValues(rs));
    	}
    }

    
    /**
     * Writes the next line to the file.
     *
     * @param nextLine
     *            a string array with each comma-separated element as a separate
     *            entry.
     * @throws IOException 
     */
    public void writeNext(String... nextLine) throws IOException {
    	
    	if (nextLine == null)
    		return;
    	
        StringBuilder sb = new StringBuilder(INITIAL_STRING_SIZE);
        for (int i = 0; i < nextLine.length; i++) {

            if (i != 0) {
                sb.append(separator);
            }

            String nextElement = nextLine[i];
            if (nextElement == null)
                continue;
            if (quotechar !=  Constants.NO_QUOTE_CHARACTER)
            	sb.append(quotechar);
            
            sb.append(stringContainsSpecialCharacters(nextElement) ? processLine(nextElement) : nextElement);

            if (quotechar != Constants.NO_QUOTE_CHARACTER)
            	sb.append(quotechar);
        }
        
        sb.append(lineEnd);
        rawWriter.write(sb.toString());
    }

	private boolean stringContainsSpecialCharacters(String line) {
	    return line.indexOf(quotechar) != -1 || line.indexOf(escapechar) != -1;
    }

	protected StringBuilder processLine(String nextElement)
    {
		StringBuilder sb = new StringBuilder(INITIAL_STRING_SIZE);
	    for (int j = 0; j < nextElement.length(); j++) {
	        char nextChar = nextElement.charAt(j);
	        if (nextChar == quotechar) {
	        	sb.append(quotechar).append(quotechar);
	        } else if (escapechar != Constants.NO_ESCAPE_CHARACTER && nextChar == escapechar) {
	        	sb.append(escapechar).append(escapechar);
	        } else {
	            sb.append(nextChar);
	        }
	    }
	    
	    return sb;
    }

    /**
     * Flush underlying stream to writer.
     * 
     * @throws IOException if bad things happen
     */
    public void flush() throws IOException {
        rawWriter.flush();
    } 

    /**
     * Close the underlying stream writer flushing any buffered content.
     *
     * @throws IOException if bad things happen
     *
     */
    public void close() throws IOException {
        flush();
        rawWriter.close();
    }

    public void setResultService(ResultSetHelper resultService) {
        this.resultService = resultService;
    }
    
	public void write(CSVWriteProc proc) {
		proc.process(this);
	}

	/**
	 * @return the separator
	 */
	public char getSeparator() {
		return separator;
	}

	/**
	 * @param separator the separator to set
	 */
	public void setSeparator(char separator) {
		this.separator = separator;
	}

	/**
	 * @return the quotechar
	 */
	public char getQuotechar() {
		return quotechar;
	}

	/**
	 * @param quotechar the quotechar to set
	 */
	public void setQuotechar(char quotechar) {
		this.quotechar = quotechar;
	}

	/**
	 * @return the escapechar
	 */
	public char getEscapechar() {
		return escapechar;
	}

	/**
	 * @param escapechar the escapechar to set
	 */
	public void setEscapechar(char escapechar) {
		this.escapechar = escapechar;
	}

	/**
	 * @return the lineEnd
	 */
	public String getLineEnd() {
		return lineEnd;
	}

	/**
	 * @param lineEnd the lineEnd to set
	 */
	public void setLineEnd(String lineEnd) {
		this.lineEnd = lineEnd;
	}
	
}
