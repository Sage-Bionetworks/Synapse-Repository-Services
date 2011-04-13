package org.sagebionetworks.web.client.view.table;

import java.util.Map;

import org.sagebionetworks.web.client.view.TrustedHtmlRenderer;
import org.sagebionetworks.web.shared.LinkColumnInfo;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.inject.Inject;

/**
 * A simple Column for rendering a single column as a HyperLink.
 * 
 * @author jmhill
 *
 */
public class LinkColumn extends Column<Map<String, Object>, String> {
	
	private Hyperlink link = null;
	LinkColumnInfo metadata = null;
	

	@Inject
	public LinkColumn(Hyperlink link){
		super(new ClickableTextCell( new TrustedHtmlRenderer()));
		this.link = link;
	}
	
	public void setLinkColumnInfo(LinkColumnInfo meta) {
		this.metadata = meta;
	}


	/**
	 * Render the cell as a link
	 */
	@Override
	public String getValue(Map<String, Object> row) {
		// Since the renderer will not escape html, we must do it here
		String displayId = metadata.getDisplay().getId();
		String displayValue;		
		if(displayId.matches("^staticdisplay.+")) {
			// display description for link text with staticdisplays
			displayValue = metadata.getDisplay().getDescription();
		} else {
			displayValue = (String) row.get(metadata.getDisplay().getId());
		}
		String urlValue = (String) row.get(metadata.getUrl().getId());
		if(displayValue != null && urlValue != null){
			String safeName = SafeHtmlUtils.htmlEscape(displayValue);
			String safeLink = SafeHtmlUtils.htmlEscape(urlValue);
			link.setText(safeName);
			link.setTargetHistoryToken(safeLink);
			return link.toString();
		}else{
			// Return an empty string for null values
			return "";
		}
	}

}
