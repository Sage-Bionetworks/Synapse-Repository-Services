package org.sagebionetworks.web.client.view;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;
/**
 * A simple render that will wrap the values in 
 * a <div class="${styeName}"> with the provided style name.
 * If the value contains any HTML it will be escaped.
 * @author jmhill
 *
 */
public class StylizedSafeHtmlRenderer implements
		SafeHtmlRenderer<String> {
	private final String styleName;

	StylizedSafeHtmlRenderer(String styleName) {
		this.styleName = styleName;
	}

	@SuppressWarnings("serial")
	@Override
	public SafeHtml render(String object) {
		final StringBuilder builder = new StringBuilder();
		builder.append("<div class=\"");
		builder.append(styleName);
		builder.append("\" >");
		// Keep it safe.
		builder.append(SafeHtmlUtils.htmlEscape(object));
		builder.append("</div>");
		return new SafeHtml() {
			@Override
			public String asString() {
				AllDatasetsViewImpl.logger.info(builder.toString());
				return builder.toString();
			}
		};
	}

	@Override
	public void render(String object, SafeHtmlBuilder builder) {
		builder.append(render(object));
	}
}