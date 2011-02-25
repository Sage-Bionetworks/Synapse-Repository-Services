package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.ImagePrototypeSingleton;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Header;

/**
 * A CellTable header that shows sorting.
 * 
 * from: http://code.google.com/p/google-web-toolkit/source/browse/branches/2.1/bikeshed/src/com/google/gwt/sample/expenses/gwt/client/SortableHeader.java
 * 
 */
public class SortableHeader extends Header<String> {
	
	private static String UP_ARROW = null;
	private static String DOWN_ARROW = null;
	
	private String columnKey = null;
	private String displayValue;
	private boolean isSorting = false;
	private boolean sortAscending = true;
	
	SortableHeader(String displayValue, ImagePrototypeSingleton prototype, String key) {
		super(new ClickableTextCell());
		this.displayValue = displayValue;
		this.columnKey = key;
		initImages(prototype);
	}

	/**
	 * If the static image strings have not been created then create them.
	 * @param prototype
	 */
	private static void initImages(ImagePrototypeSingleton prototype) {
		if(UP_ARROW == null || DOWN_ARROW == null){
			UP_ARROW = makeImage(prototype.getUpArrowIconHtml());
			DOWN_ARROW = makeImage(prototype.getDownArrowImageHtml());
		}
	}

	/**
	 * Create the images
	 * @param html
	 * @return
	 */
	private static String makeImage(String html) {
		if(html == null) return null;
		return html.replace("style='",	"style='position:absolute;right:0px;top:0px;");
	}

	public String getColumnKey() {
		return columnKey;
	}

	@Override
	public String getValue() {
		return displayValue;
	}

	public boolean isSorting() {
		return isSorting;
	}

	public void setSorting(boolean isSorting) {
		this.isSorting = isSorting;
	}

	public void toggle(){
		this.sortAscending = !this.sortAscending;
	}

	public boolean isSortAscending() {
		return sortAscending;
	}

	public void setSortAscending(boolean sortAscending) {
		this.sortAscending = sortAscending;
	}

	@Override
	public void render(Context context, SafeHtmlBuilder sb) {
		StringBuilder builder = new StringBuilder();
		builder.append("<div style='position:relative;cursor:hand;cursor:pointer;");
		builder.append("padding-right:16px;'>");
		if (isSorting) {
			if (sortAscending) {
				builder.append(UP_ARROW);
			} else {
				builder.append(DOWN_ARROW);
			}
		} else {
			builder.append("<div style='position:absolute;display:none;'></div>");
		}
		builder.append("<div>");
		builder.append(displayValue);
		builder.append("</div></div>");
		SafeHtml html = SafeHtmlUtils.fromTrustedString(builder.toString());
		sb.append(html);
	}

}
