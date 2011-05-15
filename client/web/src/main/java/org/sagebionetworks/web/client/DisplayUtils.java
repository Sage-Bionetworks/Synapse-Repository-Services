package org.sagebionetworks.web.client;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class DisplayUtils {

	public static String getIconHtml(ImageResource icon) {
		return "<span class=\"iconSpan\">" + AbstractImagePrototype.create(icon).getHTML() + "</span>";
	}
	
}
