package org.sagebionetworks.web.client.view.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.view.table.column.provider.BooleanColumnProvider;
import org.sagebionetworks.web.client.view.table.column.provider.ColumnProvider;
import org.sagebionetworks.web.client.view.table.column.provider.DoubleColumnProvider;
import org.sagebionetworks.web.client.view.table.column.provider.IntegerColumnProvider;
import org.sagebionetworks.web.client.view.table.column.provider.LongColumnProvider;
import org.sagebionetworks.web.client.view.table.column.provider.StringColumnProvider;
import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.DateColumnInfo;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.LayerColumnInfo;
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
	
	private List<ColumnProvider> providerList = new ArrayList<ColumnProvider>();
	
	/**
	 * IoC via GIN
	 * @param injector
	 */
	@Inject
	public ColumnFactoryImpl(ColumnFactoryGinInjector injector){
		this.injector = injector;
		// Fill in the the provider list with known providers
		providerList.add(new StringColumnProvider());
		providerList.add(new BooleanColumnProvider());
		providerList.add(new LongColumnProvider());
		providerList.add(new DoubleColumnProvider());
		providerList.add(new IntegerColumnProvider());
	}
	
	/**
	 * Create columns from metadata.
	 * @param meta
	 * @return
	 */
	public Column<Map<String, Object>, ?> createColumn(HeaderData meta){
		if(meta == null) throw new IllegalArgumentException("HeaderData cannot be null");
		// First determine the type
		if(meta instanceof LinkColumnInfo){
			// Link
			LinkColumn link = injector.getLinkColumn();
			link.setLinkColumnInfo((LinkColumnInfo) meta);
			return link;
		}else if(meta instanceof LayerColumnInfo){
			// Layer
			LayerColumn layer = injector.getLayerColumn();
			layer.setLayerColumnInfo((LayerColumnInfo) meta);
			return layer;
		}else if(meta instanceof DateColumnInfo){
			// Date
			DateColumn date = injector.getDateColumn();
			date.setDateColumnInfo((DateColumnInfo) meta);
			return date;
		}else if(meta instanceof ColumnInfo){
			// Find a provider that 
			ColumnInfo colInfo = (ColumnInfo) meta;
			return createColumn(colInfo);
		}else{
			throw new IllegalArgumentException("Unknown HeaderData: "+meta.getClass().getName());
		}
	}
	
	/**
	 * This flavor will use the list of providers to create a column.
	 * @param info
	 * @return
	 */
	public Column<Map<String, Object>, ?> createColumn(ColumnInfo info){
		if(info == null) throw new IllegalArgumentException("ColumnInfo cannot be null");
		if(info.getType() == null) throw new IllegalArgumentException("ColumnInfo.getType() returned null");
		if(info.getId() == null) throw new IllegalArgumentException("ColumnInfo.getId() returned null");
		// Go through the list
		for(ColumnProvider provider: providerList){
			if(provider.isCompatible(info)){
				return provider.createColumn(info);
			}
		}
		throw new IllegalArgumentException("Unkown column type: "+info.getType());
	}

}
