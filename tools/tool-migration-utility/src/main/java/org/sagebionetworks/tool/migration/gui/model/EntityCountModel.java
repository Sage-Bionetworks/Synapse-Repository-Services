package org.sagebionetworks.tool.migration.gui.model;

import java.util.HashMap;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import org.sagebionetworks.repo.model.EntityType;

/**
 * Table model for Entity Counts.
 * @author John
 *
 */
public class EntityCountModel extends AbstractTableModel {
	
	private static final long serialVersionUID = 1L;
	
	private static final String LONG_WITH_COMMA_FORMAT = "%1$,d";
	private static final String COUNT = "Count";
	private static final String ENTITY_TYPE = "Entity Type";
	private Map<String,String> map = new HashMap<String, String>();
	
	public EntityCountModel(){
		// Populate the values with zeros
		for(EntityType type: EntityType.values()){
			map.put(type.name(), "unknown");
		}
	}
	
	/**
	 * Set the count
	 * @param type
	 * @param count
	 */
	public void setValue(EntityType type, long count){
		map.put(type.name(), String.format(LONG_WITH_COMMA_FORMAT, count));
		int rowIndex = 0;
		for(int i=0; i< EntityType.values().length; i++){
			if(type == EntityType.values()[i]){
				rowIndex = i;
				break;
			}
		}
		// Fire the change.
		fireTableCellUpdated(rowIndex, 1);
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
		String key = EntityType.values()[rowIndex].name();
		if(columnIndex == 0) return key;
		else return map.get(key);
	}


	@Override
	public String getColumnName(int column) {
		if(column == 0) return ENTITY_TYPE;
		else return COUNT;
	}

	public static void main(String args[]){
		String format = LONG_WITH_COMMA_FORMAT;
		System.out.println(String.format(format, 123345));
	}
}
