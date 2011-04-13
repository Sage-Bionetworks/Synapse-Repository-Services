package org.sagebionetworks.web.client.view;

import java.util.Map;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.inject.Inject;

/**
 * The runtime version of the CellTableProvider;
 * @author jmhill
 *
 */
public class CellTableProviderImpl implements CellTableProvider {
	
	private DatasetCellTableResource cellTableResource;
	
	@Inject
	public CellTableProviderImpl(DatasetCellTableResource cellTableResource){
		this.cellTableResource = cellTableResource;
	}

	@Override
	public CellTable<Map<String, Object>> createNewTable() {
		return new CellTable<Map<String,Object>>(10, this.cellTableResource);
	}

	@Override
	public SimplePager createPager() {
		return new SimplePager(TextLocation.CENTER);
	}

}
