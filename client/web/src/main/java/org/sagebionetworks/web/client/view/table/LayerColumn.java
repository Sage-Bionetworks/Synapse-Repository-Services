package org.sagebionetworks.web.client.view.table;

import java.util.Map;

import org.sagebionetworks.web.client.ImagePrototypeSingleton;
import org.sagebionetworks.web.client.view.TrustedHtmlRenderer;
import org.sagebionetworks.web.shared.LayerColumnInfo;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.user.cellview.client.Column;
import com.google.inject.Inject;
/**
 * Renders a single column containing icons for each layer type supported by a datasets.
 * 
 * @author jmhill
 *
 */
public class LayerColumn extends Column<Map<String, Object>, String>{
	
	/**
	 * This provides the images.
	 */
	ImagePrototypeSingleton imagePrototype;
	LayerColumnInfo meta;
	
	@Inject
	public LayerColumn(ImagePrototypeSingleton imageProto) {
		super(new ClickableTextCell( new TrustedHtmlRenderer()));
		this.imagePrototype = imageProto;
	}

	@Override
	public String getValue(Map<String, Object> row) {
		// We need three columns to render this
		Boolean hasExpression = (Boolean) row.get(meta.getHasExpression().getId());
		Boolean hasGenetic = (Boolean) row.get(meta.getHasGenetic().getId());
		Boolean hasPheontype = (Boolean) row.get(meta.getHasClinical().getId());
		if(hasExpression != null && hasGenetic != null && hasPheontype != null){
			StringBuilder builder = new StringBuilder();
			// Expression
			if(hasExpression.booleanValue()){
				// show icon
				builder.append(imagePrototype.getIconGeneExpression16());
			}else{
				// show transparent icon
				builder.append(imagePrototype.getIconTransparent16Html());				
			}
			builder.append(" ");
			// genetic
			if(hasGenetic.booleanValue()){
				// show icon
				builder.append(imagePrototype.getIconGenotype16());
			}else{
				// show transparent icon
				builder.append(imagePrototype.getIconTransparent16Html());				
			}
			builder.append(" ");
			//phenotype
			if(hasPheontype.booleanValue()){
				// show icon
				builder.append(imagePrototype.getIconPhenotypes16());
			}else{
				// show transparent icon
				builder.append(imagePrototype.getIconTransparent16Html());				
			}
			return builder.toString();
		}
		return null;
	}

	public void setLayerColumnInfo(LayerColumnInfo meta) {
		this.meta = meta;
	}

}
