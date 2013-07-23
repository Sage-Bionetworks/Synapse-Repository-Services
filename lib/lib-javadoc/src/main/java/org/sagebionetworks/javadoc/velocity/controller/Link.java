package org.sagebionetworks.javadoc.velocity.controller;

/**
 * Represents a link to another class.
 * 
 * @author John
 *
 */
public class Link {

	String display;
	String href;
	public Link(){}
	public Link(String href, String display) {
		super();
		this.href = href;
		this.display = display;
	}
	public String getDisplay() {
		return display;
	}
	public void setDisplay(String display) {
		this.display = display;
	}
	public String getHref() {
		return href;
	}
	public void setHref(String href) {
		this.href = href;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((display == null) ? 0 : display.hashCode());
		result = prime * result + ((href == null) ? 0 : href.hashCode());
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
		Link other = (Link) obj;
		if (display == null) {
			if (other.display != null)
				return false;
		} else if (!display.equals(other.display))
			return false;
		if (href == null) {
			if (other.href != null)
				return false;
		} else if (!href.equals(other.href))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "Link [display=" + display + ", href=" + href + "]";
	}
	
}
