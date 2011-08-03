package org.sagebionetworks.web.client.widget.editpanels;

import java.util.List;

import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;

public class EditableColumnModel extends ColumnModel {

	private boolean editable;
	
	public EditableColumnModel(List<ColumnConfig> columns) {
		super(columns);
	}
	
	public void setCellsEditable(boolean editable) {
		this.editable = editable;
	}
	
	@Override
	public boolean isCellEditable(int colIndex) {
		return editable;
	}

}
