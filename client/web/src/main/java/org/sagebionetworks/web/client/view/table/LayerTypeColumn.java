package org.sagebionetworks.web.client.view.table;

import java.util.Map;

import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.ImagePrototypeSingleton;
import org.sagebionetworks.web.client.view.TrustedHtmlRenderer;
import org.sagebionetworks.web.shared.LayerTypeIconColumnInfo;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.inject.Inject;

/**
 * A simple column that converts a base column from a long to a date.
 * 
 * @author jmhill
 * 
 */
public class LayerTypeColumn extends Column<Map<String, Object>, String> {

	private LayerTypeIconColumnInfo meta;

	
	ImagePrototypeSingleton imagePrototype;
	IconsImageBundle iconsImageBundle;
	
	public void setLayerColumnInfo(LayerTypeIconColumnInfo meta) {
		this.meta = meta;
	}

	@Inject
	public LayerTypeColumn(ImagePrototypeSingleton imagePrototype, IconsImageBundle iconsImageBundle) {
		super(new ClickableTextCell(new TrustedHtmlRenderer()));
		this.imagePrototype = imagePrototype;
		this.iconsImageBundle = iconsImageBundle;
	}

	@Override
	public String getValue(Map<String, Object> row) {
		String cellImageType = (String) row.get(meta.getBaseColumn().getId());
		if ("E".equals(cellImageType)) {
			return imagePrototype.getIconGeneExpression16();
		} else if ("C".equals(cellImageType)) {
			return imagePrototype.getIconPhenotypes16();
		} else if ("G".equals(cellImageType)) {
			return imagePrototype.getIconGenotype16();
		} else if("M".equals(cellImageType)) {
			return ""; // don't display Media columns in table
			//return AbstractImagePrototype.create(iconsImageBundle.documentText16()).getHTML();
		}
		
		throw new IllegalArgumentException("Unknown Layer type: "
				+ cellImageType);

	}

}
