package org.sagebionetworks.web.shared;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A very basic wrapper of a long 
 * @author jmhill
 *
 */
public class DateColumnInfo implements HeaderData, IsSerializable, CompositeColumn {
	
	private String id;
	ColumnInfo baseColumn;

	public ColumnInfo getBaseColumn() {
		return baseColumn;
	}

	public void setBaseColumn(ColumnInfo baseColumn) {
		if(baseColumn == null) throw new IllegalArgumentException("The base column cannot be null");
		// The base type must be long
		if(baseColumn.fetchType() == null) throw new IllegalArgumentException("The base column type is null");
		if(ColumnInfo.Type.Long != baseColumn.fetchType()) throw new IllegalArgumentException("The base column type must be: Long, not: "+baseColumn.getType());
		this.baseColumn = baseColumn;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public List<String> getBaseDependencyIds() {
		List<String> list = new ArrayList<String>();
		list.add(baseColumn.getId());
		return list;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getDisplayName() {
		return baseColumn.getDisplayName();
	}

	@Override
	public String getDescription() {
		return baseColumn.getDescription();
	}

	@Override
	public String getSortId() {
		return baseColumn.getSortId();
	}

}
