package org.sagebionetworks.web.client.widget.editpanels;

import com.extjs.gxt.ui.client.data.BaseModelData;

public class EditableAnnotationModelData extends BaseModelData {

	public static final String KEY_COLUMN_ID = "name";	
	public static final String VALUE_COLUMN_ID = "value";
	public static final String VALUE_DISPLAY_COLUMN_ID = "valueDisplay";
	
	
	private static final long serialVersionUID = 1L;
	
	private Boolean changed;	
	private ColumnEditType columnEditType; 
 
	public EditableAnnotationModelData() {
		super();
	}	
	
	/*
	 * Getter & Setter for Column Edit Type (generated)
	 */
	public ColumnEditType getColumnEditType() {
		return columnEditType;
	}

	public void setColumnEditType(ColumnEditType columnEditType) {
		this.columnEditType = columnEditType;
	}

	
	/*
	 * Getter & Setter for the key
	 */
	public String getKey() {
		return super.get(KEY_COLUMN_ID);
	}
	
	public void setKey(String key) {
		this.set(KEY_COLUMN_ID, key);
	};	
	
	/*
	 * Getter & Setter for the value
	 */
	@SuppressWarnings("unchecked")
	public <X extends Object> X getValue() {
		return (X) super.get(VALUE_COLUMN_ID);
	}
	
	public <X extends Object> void setValue(X value) {
		this.set(VALUE_COLUMN_ID, value);
	}

	/*
	 * Getter & Setter for the value display 
	 */
	@SuppressWarnings("unchecked")
	public <X extends Object> X getValueDisplay() {		
		// if not display is set, just return the value
		if(super.get(VALUE_DISPLAY_COLUMN_ID) == null) {
			return (X) super.get(VALUE_COLUMN_ID);
		} 

		return (X) super.get(VALUE_DISPLAY_COLUMN_ID);
	}
	
	public <X extends Object> void setValueDisplay(X valueDisplay) {
		this.set(VALUE_DISPLAY_COLUMN_ID, valueDisplay);
	}

	
	@Override
	public <X extends Object> X set(String property, X value) {
		// if value is being set, flip dirty bit
		if(VALUE_COLUMN_ID.equals(property)) {
			changed = changed == null ? false : true; // set to false on first assign
		}
		return (X) super.set(property, value);
	};
	
	public boolean isDirty() {
		if(changed == null) return false;
		return changed;
	};	
}
