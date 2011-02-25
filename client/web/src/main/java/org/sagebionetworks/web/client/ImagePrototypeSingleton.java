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
	private String iconTransparent16Html;
	private String iconGeneExpression16;
	private String iconPhenotypes16;
	private String iconGenotype16;
	
	@Inject
	ImagePrototypeSingleton(SageImageBundle bundle){
		// Fill in all of the fields
		upArrowIconHtml = AbstractImagePrototype.create(bundle.iconUpArrow()).getHTML();
		downArrowIconHtml = AbstractImagePrototype.create(bundle.iconDownArrow()).getHTML();
		iconTransparent16Html = AbstractImagePrototype.create(bundle.iconTransparent16()).getHTML();
		iconGeneExpression16 = AbstractImagePrototype.create(bundle.iconGeneExpression16()).getHTML();
		iconPhenotypes16 = AbstractImagePrototype.create(bundle.iconPhenotypes16()).getHTML();
		iconGenotype16 = AbstractImagePrototype.create(bundle.iconGenotype16()).getHTML();
	}

	public String getUpArrowIconHtml() {
		return upArrowIconHtml;
	}

	public String getDownArrowImageHtml() {
		return downArrowIconHtml;
	}

	public String getIconTransparent16Html() {
		return iconTransparent16Html;
	}

	public String getIconGeneExpression16() {
		return iconGeneExpression16;
	}

	public String getIconPhenotypes16() {
		return iconPhenotypes16;
	}

	public String getIconGenotype16() {
		return iconGenotype16;
	}

}
