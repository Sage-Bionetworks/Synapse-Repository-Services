package org.sagebionetworks.web.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * See: 
 * @author jmhill
 *
 */
public class UrlTemplate extends ColumnInfo implements IsSerializable{
	
	private String urlTemplate;
	
	public UrlTemplate(){
		super();
	}
	
	public UrlTemplate(String template, String id, String type, String displayName,String description){
		super(id, type, displayName, description);
		this.urlTemplate = template;
	}

	public String getUrlTemplate() {
		return urlTemplate;
	}

	public void setUrlTemplate(String urlTemplate) {
		this.urlTemplate = urlTemplate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((urlTemplate == null) ? 0 : urlTemplate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		UrlTemplate other = (UrlTemplate) obj;
		if (urlTemplate == null) {
			if (other.urlTemplate != null)
				return false;
		} else if (!urlTemplate.equals(other.urlTemplate))
			return false;
		return true;
	}

}
