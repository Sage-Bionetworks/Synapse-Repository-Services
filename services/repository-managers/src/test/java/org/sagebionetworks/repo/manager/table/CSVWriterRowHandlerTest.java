package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.util.csv.CSVWriterStream;

import com.google.common.collect.Lists;

public class CSVWriterRowHandlerTest {
	
	LinkedList<String> writtenLines;
	
	CSVWriterStream writer;
	List<SelectColumn> selectColumns;
	boolean includeRowIdAndVersion;
	CSVWriterRowHandler handler;
	
	@Before
	public void before(){
		
		writtenLines = new LinkedList<String>();
		writer = new CSVWriterStream() {

			@Override
			public void writeNext(String[] nextLine) {
				StringBuilder builder = new StringBuilder();
				boolean first = true;
				for(String cell: nextLine){
					if(! first){
						builder.append(",");
					}
					builder.append(cell);
					first = false;
				}
				writtenLines.add(builder.toString());
			}
		};
		SelectColumn column = new SelectColumn();
		column.setColumnType(ColumnType.STRING);
		column.setName("foo");
		selectColumns = Lists.newArrayList(column);
		
		includeRowIdAndVersion = true;
		
		handler = new CSVWriterRowHandler(writer, selectColumns, includeRowIdAndVersion);
	}

	@Test
	public void testHandler(){
		// write the header.
		handler.writeHeader();
		//  add a row
		Row row = new Row();
		row.setRowId(1L);
		row.setVersionNumber(2L);
		row.setValues(Lists.newArrayList("one"));
		handler.nextRow(row);
		assertEquals(2, writtenLines.size());
		assertEquals("ROW_ID,ROW_VERSION,foo", writtenLines.get(0));
		assertEquals("1,2,one", writtenLines.get(1));
	}

}
