package org.sagebionetworks.web.client.view;

import com.google.gwt.user.cellview.client.CellTable;

public interface DatasetCellTableResource extends CellTable.Resources {
	
	public interface CellTableStyle extends CellTable.Style {};

	 @Source({"DatasetCellTable.css"})
	 CellTableStyle cellTableStyle(); 
	
}
