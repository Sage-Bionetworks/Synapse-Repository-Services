package org.sagebionetworks.web.shared;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ColumnMetadata implements IsSerializable {
	
	public enum RenderType {
		LINK(2),
		IMAGE_LIST(1),
		IMAGE(1),
		STRING(1),
		DATE(1),
		INTEGER(1);
		
		private int numberKeys;
		
		RenderType(int numberKeys){
			this.numberKeys = numberKeys;
		}
		
		/**
		 * How many keys does the renderer require?
		 * @return
		 */
		public int getKeyCount(){
			return numberKeys;
		}
	}
	
	private boolean sortable = false;
	private String sortKey = null;
	private String headerValue = null;
	private RenderType type = null;
	private List<String> valueKeys = null;

	public RenderType getType() {
		return type;
	}

	public void setType(RenderType type) {
		this.type = type;
	}

	public List<String> getValueKeys() {
		return valueKeys;
	}

	public void setValueKeys(List<String> valueKeys) {
		this.valueKeys = valueKeys;
	}

	public String getHeaderValue() {
		return headerValue;
	}

	public void setHeaderValue(String headerValue) {
		this.headerValue = headerValue;
	}

	public String getSortKey() {
		return sortKey;
	}

	public void setSortKey(String sortKey) {
		this.sortKey = sortKey;
	}

	public boolean isSortable() {
		return sortable;
	}

	public void setSortable(boolean sortable) {
		this.sortable = sortable;
	}

}
