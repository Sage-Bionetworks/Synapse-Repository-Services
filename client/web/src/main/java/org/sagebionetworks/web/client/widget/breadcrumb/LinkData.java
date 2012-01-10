package org.sagebionetworks.web.client.widget.breadcrumb;

import com.google.gwt.place.shared.Place;

public class LinkData {

	private String text;
	private Place place;
	public LinkData(String text, Place place) {
		super();
		this.text = text;
		this.place = place;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public Place getPlace() {
		return place;
	}
	public void setPlace(Place place) {
		this.place = place;
	}
	
}
