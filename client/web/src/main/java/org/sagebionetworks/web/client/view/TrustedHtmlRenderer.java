package org.sagebionetworks.web.client.view;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SafeHtmlRenderer;

/**
 * Allows pure HTML to be rendered.
 * @author jmhill
 *
 */
public class TrustedHtmlRenderer implements SafeHtmlRenderer<String> {

	@Override
	public SafeHtml render(final String value) {
		return new SafeHtml() {
			private static final long serialVersionUID = 1L;
			@Override
			public String asString() {
				return value;
			}
		};
	}

	@Override
	public void render(String object, SafeHtmlBuilder builder) {
		builder.append(render(object));
	}

}
