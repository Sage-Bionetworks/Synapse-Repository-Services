package org.sagebionetworks.repo.model.table;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.manager.table.TableModelUtils;

public class TableModelUtilsTest {

	@Test (expected=IllegalArgumentException.class)
	public void testValidateNewTableModelNull(){
		TableModelUtils.validateNewTableModel(null);
	}
	
	@Test
	public void testValidateNewTableModelEmpty(){
		TableModelUtils.validateNewTableModel(new TableEntity());
	}
	
	@Test
	public void testValidateNewTableModel(){
		TableEntity table = new TableEntity();
		List<ColumnModel> columns = new LinkedList<ColumnModel>();
		table.setColumns(columns);
		
		TableModelUtils.validateNewTableModel(table);
	}
}
