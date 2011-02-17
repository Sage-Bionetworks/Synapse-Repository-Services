package org.sagebionetworks.web.client.presenter;

import java.util.Date;

import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.LayerLink;

public class DatasetRow {
	
	private Dataset wrapped = null;
	// Start with no layers
	private int layerMask = 0x0;
	
	public DatasetRow(Dataset toWrap){
		this.wrapped = toWrap;
		// Determine the layer mask
		layerMask = 0x00;
		if(this.wrapped.getLayerPreviews() != null){
			for(LayerLink layer: this.wrapped.getLayerPreviews()){
				layerMask = layerMask | layer.getType().getMask();
			}
		}
	}

	public String getName() {
		return wrapped.getName();
	}
	
	public int getLayersMask(){
		return layerMask;
	}

	public String getStatus() {
		return wrapped.getStatus();
	}

	public String getCreator() {
		return wrapped.getCreator();
	}

	public Date getCreatedOn() {
		return wrapped.getCreationDate();
		
	}
	public Date getModifiedColumn() {
		return wrapped.getReleaseDate();
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

	public String getDesc1ription() {
		return wrapped.getDescription();
	}

}
