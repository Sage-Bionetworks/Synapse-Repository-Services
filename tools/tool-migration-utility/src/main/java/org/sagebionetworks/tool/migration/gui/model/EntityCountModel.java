package org.sagebionetworks.tool.migration.gui.model;

import java.util.HashMap;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.MigratableObjectType;

/**
 * Table model for Entity Counts.
 * @author John
 *
 */
public class EntityCountModel extends AbstractTableModel {
	
	private static final long serialVersionUID = 1L;
	
	private static final String LONG_WITH_COMMA_FORMAT = "%1$,d";
	private static final String COUNT = "Count";
	private static final String OBJECT_TYPE = "Object Type";
	private Map<Integer,String> map = new HashMap<Integer, String>();
	private Map<String,Integer> rowIndex = new HashMap<String, Integer>();
	private Map<Integer,String> reverseRowIndex = new HashMap<Integer,String>();
	
	public EntityCountModel(){
		int i=0;
		for (EntityType entityType : EntityType.values()) {
			rowIndex.put(entityType.name(), i);
			reverseRowIndex.put(i, entityType.name());
			map.put(i, "unknown");
			i++;
		}
		for (MigratableObjectType objType : MigratableObjectType.values()) {
			if (objType.equals(MigratableObjectType.ENTITY)) {
				// skip it
			} else {
				rowIndex.put(objType.name(), i);
				reverseRowIndex.put(i, objType.name());
				map.put(i, "unknown");
				i++;
			}
		}
	}
	
	/**
	 * Set the count
	 * @param objType ENTITY, PRINCIPAL, etc.
	 * @param entityType if objType==ENTITY, this is the sub-type, else null
	 * @param count
	 */
	public void setValue(MigratableObjectType objType, EntityType entityType, long count){
		String typeName = objType.name();
		if (objType.equals(MigratableObjectType.ENTITY)) typeName=entityType.name();
		int r = rowIndex.get(typeName);
		map.put(r, String.format(LONG_WITH_COMMA_FORMAT, count));
		// Fire the change.
		fireTableCellUpdated(r, 1);
	}


	@Override
	public int getRowCount() {
		return map.size();
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if(columnIndex == 0) return reverseRowIndex.get(rowIndex);
		else return map.get(rowIndex);
	}


	@Override
	public String getColumnName(int column) {
		if(column == 0) return OBJECT_TYPE;
		else return COUNT;
	}

	public static void main(String args[]){
		String format = LONG_WITH_COMMA_FORMAT;
		System.out.println(String.format(format, 123345));
	}
}
