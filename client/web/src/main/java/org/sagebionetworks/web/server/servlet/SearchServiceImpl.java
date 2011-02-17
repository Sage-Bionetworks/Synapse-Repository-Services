package org.sagebionetworks.web.server.servlet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.sagebionetworks.web.client.SearchService;
import org.sagebionetworks.web.server.ColumnConfigProvider;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.server.ServerConstants;
import org.sagebionetworks.web.shared.ColumnMetadata;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.TableResults;
import org.sagebionetworks.web.shared.ColumnMetadata.RenderType;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SearchServiceImpl extends RemoteServiceServlet implements
		SearchService {
	
	private static Logger logger = Logger.getLogger(SearchServiceImpl.class
			.getName());
	
	/**
	 * The template is injected with Gin
	 */
	private RestTemplateProvider templateProvider;

	/**
	 * Injected with Gin
	 */
	private ColumnConfigProvider columnConfig;
	
	private List<String> defaultDatasetColumns;
	
	/**
	 * Injected via Gin.
	 * 
	 * @param template
	 */
	@Inject
	public void setRestTemplate(RestTemplateProvider template) {
		this.templateProvider = template;
	}
	
	/**
	 * Injected via Gin
	 * @param columnConfig
	 */
	@Inject
	public void setColunConfigProvider(ColumnConfigProvider columnConfig){
		this.columnConfig = columnConfig;
	}
	
	@Inject
	public void setDefaultDatasetColumns(@Named("org.sagebionetworks.all.datasets.default.columns") String defaults){
		// convert from a string to a list
		String[] split = defaults.split(",");
		this.defaultDatasetColumns = new LinkedList<String>();
		for(int i=0; i<split.length; i++){
			defaultDatasetColumns.add(split[i].trim());
		}
	}

	@Override
	public TableResults executeSearch(SearchParameters params) {
		TableResults results = new TableResults();
		results.setTotalNumberResults(100);
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		results.setRows(rows);
		
		// Fill in the rows
		Map<String, Object> row = new LinkedHashMap<String, Object>();
		row.put("name", "rowZeroName");
		row.put("datasetUrl", "0");
		rows.add(row);
		
		// next row
		row = new LinkedHashMap<String, Object>();
		row.put("name", "rowOneName");
		row.put("datasetUrl", "1");
		rows.add(row);
		return results;
	}

	@Override
	public List<ColumnMetadata> getColumnMetadata() {
		List<ColumnMetadata> results = new ArrayList<ColumnMetadata>();
		
		// The datasets name
		ColumnMetadata name = new ColumnMetadata();
		name.setHeaderValue("Dataset Name");
		name.setSortable(true);
		name.setSortKey("name");
		name.setType(RenderType.LINK);
		List<String> valueKeys = new ArrayList<String>();
		valueKeys.add("name");
		valueKeys.add("datasetUrl");
		name.setValueKeys(valueKeys);
		
		results.add(name);
		
		return results;
	}

	@Override
	public List<String> getDefaultDatasetColumnIds() {
		return defaultDatasetColumns;
	}

}
