package org.sagebionetworks.web.client.widget.editpanels;

import com.extjs.gxt.ui.client.data.BaseModelData;

public class EditableAnnotationModelData extends BaseModelData {

	/**
	 * Get the column id for the key. This is for creating a ColumnConfig
	 */
	public static final String KEY_COLUMN_ID = "name";
	
	/**
	 * Get the column id for the value. This is for creating a ColumnConfig
	 */
	public static final String VALUE_COLUMN_ID = "value";
	
	
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
