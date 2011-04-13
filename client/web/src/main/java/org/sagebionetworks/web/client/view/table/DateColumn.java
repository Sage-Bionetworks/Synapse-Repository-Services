package org.sagebionetworks.web.client.view.table;

import java.util.Date;
import java.util.Map;

import org.sagebionetworks.web.shared.DateColumnInfo;

import com.google.gwt.cell.client.DateCell;
import com.google.gwt.user.cellview.client.Column;

/**
 * A simple column that converts a base column from a long to a date.
 * @author jmhill
 *
 */
public class DateColumn extends Column<Map<String, Object>, Date>{
	
	private DateColumnInfo meta;
	
	public void setDateColumnInfo(DateColumnInfo meta){
		this.meta = meta;
	}

	public DateColumn() {
		super(new DateCell());
	}

	@Override
	public Date getValue(Map<String, Object> map) {
		// Get the long from the map
		Long value = (Long) map.get(meta.getBaseColumn().getId());
		if(value != null){
			// Us the long to create a date
			return new Date(value.longValue());
		}
		return null;
	}

}
