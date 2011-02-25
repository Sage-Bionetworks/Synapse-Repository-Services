package org.sagebionetworks.web.shared;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Renders layer types as a single column of icons
 * 
 * @author jmhill
 *
 */
public class LayerColumnInfo implements HeaderData, IsSerializable, CompositeColumn{
	
	private String id;
	private String displayName;
	private String description;
	ColumnInfo hasExpression;
	ColumnInfo hasGenetic;
	ColumnInfo hasClinical;
	
	
	public ColumnInfo getHasExpression() {
		return hasExpression;
	}

	public void setHasExpression(ColumnInfo hasExpression) {
		this.hasExpression = hasExpression;
	}

	public ColumnInfo getHasGenetic() {
		return hasGenetic;
	}

	public void setHasGenetic(ColumnInfo hasGenetic) {
		this.hasGenetic = hasGenetic;
	}

	public ColumnInfo getHasClinical() {
		return hasClinical;
	}

	public void setHasClinical(ColumnInfo hasClinical) {
		this.hasClinical = hasClinical;
	}

	@Override
	public List<String> getBaseDependencyIds() {
		List<String> dependencies = new ArrayList<String>();
		// Depends on the display name and url
		dependencies.add(hasExpression.getId());
		dependencies.add(hasGenetic.getId());
		dependencies.add(hasClinical.getId());
		return dependencies;
	}
	
	public void setId(String id){
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getSortId() {
		// Return null since this is not well defined for this column.
		return null;
	}

}
