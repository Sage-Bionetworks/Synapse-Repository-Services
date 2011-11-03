package org.sagebionetworks.web.client.presenter;

import java.util.Date;

import org.sagebionetworks.repo.model.Layer;


public class LayerRow {
	
	private Layer wrapped = null;
	
	public LayerRow(Layer toWrap){
		this.wrapped = toWrap;
	}

	public String getName() {
		return wrapped.getName();
	}
	
	public String getLayersType(){
		return wrapped.getType().name();
	}

	public Date getCreatedOn() {
		return wrapped.getCreatedOn();
		
	}

	/**
	 * Build a relative link to this dataset.
	 * @return
	 */
	public String getLinkString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Dataset:");
		builder.append(wrapped.getId());
		return builder.toString();
	}

	public String getId() {
		return wrapped.getId();
	}

	public String getDescription() {
		return wrapped.getDescription();
	}

}
