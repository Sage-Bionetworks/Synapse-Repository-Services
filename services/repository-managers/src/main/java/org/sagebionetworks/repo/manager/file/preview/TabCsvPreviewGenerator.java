package org.sagebionetworks.repo.manager.file.preview;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

/**
 * Generates previews for text content types.
 * 
 * @author Jay
 *
 */
public class TabCsvPreviewGenerator implements PreviewGenerator {
	
	public static final String TEXT_TAB_SEPARATED_VALUES = "text/tab-separated-values";
	public static final String TEXT_CSV_SEPARATED_VALUES = "text/csv";
	public static final Character TAB = '\t';
	public static final Character COMMA = ',';
	public static final Character DOUBLE_QUOTE = '\"';
	public static final Character NEWLINE = '\n';
	public static final Character CR = '\r';
	public static final Character EOF = '\u001a';
	public static final String HTML_ELLIPSIS = "&hellip;";
	public static final String HTML_COMMA = "&#44;";
	public static final int MAX_ROW_COUNT = 30;
	public static final int MAX_COLUMN_COUNT = 20;
	public static final int MAX_CELL_CHARACTER_COUNT = 40;
	public Character delimiter;
	
	public static Character getComma() {
		return COMMA;
	}
	public static Character getTab() {
		return TAB;
	}
	
	public TabCsvPreviewGenerator(Character delimiter) {
		this.delimiter = delimiter;
	}
	
	public PreviewOutputMetadata generatePreview(InputStream from, OutputStream to) throws IOException {
		String output = read(from);
		IOUtils.write(output, to, "UTF-8");
		//always generates a csv preview
		return new PreviewOutputMetadata(TEXT_CSV_SEPARATED_VALUES, ".csv");
	}
	
	protected void setDelimiter(Character newDelimiter) {
		this.delimiter = newDelimiter;
	}
	
	public String read(InputStream from) throws IOException{
		StringBuilder buffer = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(from, "UTF-8");
        BufferedReader in = new BufferedReader(isr);
        int currentLine = 0;
        boolean isEndOfFile = false;
        String lastRow = null;
        while(currentLine < MAX_ROW_COUNT && !isEndOfFile) {
        	lastRow = getNextRow(in);
        	//if a line consists only of whitespace, then do not include it in the preview output
        	if (lastRow.trim().length() > 0)
        		buffer.append(lastRow);
			currentLine++;
			if (lastRow.length() > 0)
				isEndOfFile = lastRow.charAt(lastRow.length()-1) == EOF;
		}
		if (currentLine >= MAX_ROW_COUNT) {
			//indicate we are truncating rows.
			if (lastRow != null && lastRow.length() > 0) {
				//output the correct number of columns
				int columnCount = lastRow.split(",").length;
				for (int i = 0; i < columnCount; i++) {
					buffer.append(HTML_ELLIPSIS);
					if (i != columnCount-1)
						buffer.append(COMMA);
				}
			}
		}
		in.close(); 
		
		if (isEndOfFile) {
			buffer.deleteCharAt(buffer.length()-1);
		}
			
		return buffer.toString();
	}
	
	public String getNextRow(BufferedReader in) throws IOException {
		int currentColumn = 0;
		StringBuilder buffer = new StringBuilder();
		boolean isEndOfLine = false;
		boolean isEndOfFile = false;
		while(currentColumn < MAX_COLUMN_COUNT && !isEndOfLine && !isEndOfFile) {
        	String cellText = getNextCell(in);
        	buffer.append(cellText);
			currentColumn++;
			isEndOfLine = cellText.endsWith("\n") || cellText.endsWith("\r");
			if (cellText.length() > 0)
				isEndOfFile = cellText.charAt(cellText.length()-1) == EOF;
		}
        if (currentColumn >= MAX_COLUMN_COUNT) {
			//indicate we are truncating columns.
        	buffer.append(HTML_ELLIPSIS);
        	//and read until the end of line
        	in.readLine();
        }
        if (!isEndOfLine && !isEndOfFile) {
        	buffer.append("\n");	
        }
        return buffer.toString();
	}
	
	public String getNextCell(BufferedReader in) throws IOException {
		StringBuilder buffer = new StringBuilder();
		int ch;
		int count = 0;
		boolean isInQuote = false;
		//extract the cell text (or as much as we are allowed)
		while ((ch = in.read()) > -1 && count < MAX_CELL_CHARACTER_COUNT && isInCell(ch, isInQuote)) {
			if (ch == DOUBLE_QUOTE) {
				isInQuote = !isInQuote;
			} else {
				if (ch == COMMA) {
					//convert commas that occur inside of a cell to HTML_COMMA
					buffer.append(HTML_COMMA);
				} else if (ch == NEWLINE || ch == CR) {
					//eat newlines inside of cells (convert to spaces)
					buffer.append(" ");
				} else {
					buffer.append((char) ch);	
				}
				count++;
			}
		}
		// now scan forward to the next newline if we didn't find one above
		if (isInCell(ch, isInQuote)) {
			buffer.append(HTML_ELLIPSIS);
			while ((ch = in.read()) > -1) {
				if (ch == DOUBLE_QUOTE) {
					isInQuote = !isInQuote;
				} else if (!isInCell(ch, isInQuote)) {
					break;
				}
			}
		}
		
		//always sending csv to output.  and if this is the end of the stream, output an EOF character so that callers recognize the state
		if (ch == delimiter)
			ch = COMMA;
		else if (ch == -1)
			ch = EOF;
		buffer.append((char) ch);
		return buffer.toString();
	}
	
	public boolean isInCell(int ch, boolean isInQuote) {
		//if in quote, then assume we're in the same cell
		if (ch == -1)
			return false;
		if (isInQuote)
			return true;
		return ch != NEWLINE && ch != CR && ch != delimiter;
	}
	
	public float getMemoryMultiplierForContentType(String contentType) {
		return 1;
	}
	
	@Override
	public boolean supportsContentType(String contentType) {
		if (delimiter == COMMA)
			return contentType.equals(TEXT_CSV_SEPARATED_VALUES);
		else if (delimiter == TAB)
			return contentType.equals(TEXT_TAB_SEPARATED_VALUES);
		else return false;
	}
}
