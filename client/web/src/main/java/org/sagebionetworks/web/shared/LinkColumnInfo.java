package org.sagebionetworks.web.shared;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A link column is a composite of two other columns, 
 * one for the display name and the other for the url.
 * 
 * @author jmhill
 *
 */
public class LinkColumnInfo implements HeaderData, IsSerializable, CompositeColumn {
	
	private String id;
	private ColumnInfo display;
	private UrlTemplate url;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ColumnInfo getDisplay() {
		return display;
	}

	public void setDisplay(ColumnInfo display) {
		this.display = display;
	}

	public UrlTemplate getUrl() {
		return url;
	}

	public void setUrl(UrlTemplate url) {
		this.url = url;
	}

	@Override
	public List<String> getBaseDependencyIds() {
		List<String> dependencies = new ArrayList<String>();
		// Depends on the display name and url
		dependencies.add(display.getId());
		dependencies.add(url.getId());
		return dependencies;
	}
	
	@Override
	public String getDisplayName() {
		return display.getDisplayName();
	}

	@Override
	public String getDescription() {
		return display.getDescription();
	}
	
	@Override
	public String getSortId() {
		// Use the display column for sorting
		return display.getId();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((display == null) ? 0 : display.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LinkColumnInfo other = (LinkColumnInfo) obj;
		if (display == null) {
			if (other.display != null)
				return false;
		} else if (!display.equals(other.display))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

}
