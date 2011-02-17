package org.sagebionetworks.web.client.view.table;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.shared.ColumnMetadata;
import org.sagebionetworks.web.shared.ColumnMetadata.RenderType;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.LinkColumnInfo;

import com.google.gwt.user.cellview.client.Column;
import com.google.inject.Inject;

/**
 * Factory for translating ColumnMetadata into GWT CellTable Columns.
 * 
 * @author jmhill
 *
 */
public class ColumnFactoryImpl implements ColumnFactory {
	
	private ColumnFactoryGinInjector injector;

	/**
	 * IoC via GIN
	 * @param injector
	 */
	@Inject
	public ColumnFactoryImpl(ColumnFactoryGinInjector injector){
		this.injector = injector;
	}
	
	/**
	 * Create columns from metadata.
	 * @param meta
	 * @return
	 */
	public Column<Map<String, Object>, ?> createColumn(HeaderData meta){
		if(meta == null) throw new IllegalArgumentException("HeaderData cannot be null");
//		// For this type we need two keys
//		List<String> valueKeys = meta.getValueKeys();
//		if(valueKeys == null) throw new IllegalArgumentException("The valueKeys list cannot be null");
//		// Validate that the required number of keys are present
//		RenderType type = meta.getType();
//		if(type == null) throw new IllegalArgumentException("RenderType cannot be null");
//		// Validate the the expected number of keys are present.
//		if(type.getKeyCount() != valueKeys.size()) throw new IllegalArgumentException("RenderType: "+type.name()+" requires: "+type.getKeyCount()+" keys not: "+valueKeys.size());
		// First determine the type
		if(meta instanceof LinkColumnInfo){
			// There must be two keys for this type
			LinkColumn link = injector.getLinkColumn();
			link.setLinkColumnInfo((LinkColumnInfo) meta);
			return link;
		}else{
			throw new IllegalArgumentException("Unknown HeaderData: "+meta.getClass().getName());
		}
	}

}
