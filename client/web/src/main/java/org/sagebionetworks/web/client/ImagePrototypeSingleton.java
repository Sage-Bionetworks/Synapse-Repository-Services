package org.sagebionetworks.web.client;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.inject.Inject;

/**
 * A singleton provider of Image prototypes
 * 
 * @author jmhill
 *
 */
public class ImagePrototypeSingleton {
	
	
	private String upArrowIconHtml;
	private String downArrowIconHtml;
	
	@Inject
	ImagePrototypeSingleton(SageImageBundle bundle){
		// Fill in all of the fields
		upArrowIconHtml = AbstractImagePrototype.create(bundle.iconUpArrow()).getHTML();
		downArrowIconHtml = AbstractImagePrototype.create(bundle.iconDownArrow()).getHTML();
	}

	public String getUpArrowIconHtml() {
		return upArrowIconHtml;
	}

	public String getDownArrowImageHtml() {
		return downArrowIconHtml;
	}

}
