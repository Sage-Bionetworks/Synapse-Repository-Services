package org.sagebionetworks.bccsetup;

import java.util.HashMap;
import java.util.Map;

import com.google.gdata.data.spreadsheet.Cell;

/**
 * The Google interface for spreadsheets gives a sequential 'feed' of cells, 
 * it does not provide for 'random access'.  This class organizes the 
 * cells for access by <row, col>
 * 
 * @author brucehoff
 *
 */
public class WorksheetModel {
	private Map<Integer, Map<Integer,Cell>> tableMap = new HashMap<Integer, Map<Integer,Cell>>();
	private int rows = 0;
	private int cols = 0;
	
	/*
	 * Put the given cell at the given <row,col>
	 * @param row the zero-based row index
	 * @param col the zero-based col index
	 * @param cell
	 * 
	 */
	public void add(int row, int col, Cell cell) {
		Map<Integer,Cell> rowMap = tableMap.get(row);
		if (rowMap==null) {
			rowMap = new HashMap<Integer,Cell>();
			tableMap.put(row, rowMap);
			if (rows<=row) rows=row+1;
		}
		rowMap.put(col, cell);
		if (cols<=col) cols=col+1;
	}
	
	/**
	 * Get the cell at the give <row,col>
	 * @param row the zero-based row index
	 * @param col the zero-based col index
	 * @return the Cell stored at the given <row,col> or null if no cell
	 */
	public Cell get(int row, int col) {
		Map<Integer,Cell> rowMap = tableMap.get(row);
		if (rowMap==null) return null;
		return rowMap.get(col);
	}
	
	/**
	 * 
	 * @return the number of rows in the table.  Note, the table may be sparse, so not all rows are occupied.
	 */
	public int getRows() {return rows;}
	
	/**
	 * 
	 * @return the number of columns in the table.  Note, the table may be sparse, so not all columns are occupied.
	 */
	public int getCols() {return cols;}

}
