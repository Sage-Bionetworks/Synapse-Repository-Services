package org.sagebionetworks.web.client.view;

import java.util.List;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

/**
 * Renders a list of images as a table row..
 * @author jmhill
 *
 */
public class ImageResourceListCell extends AbstractCell<List<ImageResource>> {

	@Override
	public void render(com.google.gwt.cell.client.Cell.Context context,
			List<ImageResource> list, SafeHtmlBuilder sb) {
		if (list != null) {
			// Render each image in a row.
			// Put the images in a row
			StringBuilder builder = new StringBuilder();
			builder.append("<table cellpadding=\"1\">");
			builder.append("<tr>");
			// Add each image as a cell
			for (ImageResource image : list) {
				builder.append("<td>");
				builder.append(AbstractImagePrototype.create(image).getHTML());
				builder.append("</td>");
			}
			builder.append("</tr></table>");
			SafeHtml html = SafeHtmlUtils.fromTrustedString(builder.toString());
			sb.append(html);
		}
	}

}
