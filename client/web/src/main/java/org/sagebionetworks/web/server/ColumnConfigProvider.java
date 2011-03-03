package org.sagebionetworks.web.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.sagebionetworks.web.shared.CompositeColumn;
import org.sagebionetworks.web.shared.ColumnsForType;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.SearchParameters.FromType;
import org.sagebionetworks.web.shared.UrlTemplate;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Loads the column configuration from the classpath.
 * @author jmhill
 *
 */
public class ColumnConfigProvider {
	
	private static Logger logger = Logger.getLogger(ColumnConfigProvider.class.getName());
	
	private ColumnConfig config = null;
	private LinkedHashMap<String, HeaderData> map = null;
	private Map<String, List<String>> defaultColumns = new TreeMap<String, List<String>>();
	
	/**
	 * Maps of additional columns for each type.
	 */
	private Map<String, List<String>> additoinalColumns = new TreeMap<String, List<String>>();
	
	
	/**
	 * Cache of the columns for a given type.
	 */
	private Map<String, ColumnsForType> columnsForTypeCache = new TreeMap<String, ColumnsForType>();
	
	/**
	 * Will load the column configuration from an XML file.
	 * @param resourceName
	 */
	@Inject
	public ColumnConfigProvider(@Named(ServerConstants.KEY_COLUMN_CONFIG_XML_FILE) String resourceName){
		InputStream in = ColumnConfigProvider.class.getClassLoader().getResourceAsStream(resourceName);
		if(in != null){
			try{
				InputStreamReader reader = new InputStreamReader(in);
				// Load the from the xml
				this.config = ColumnConfig.fromXml(reader);
				init();
			}finally{
				try {
					in.close();
				} catch (IOException e) {}
			}
		}else{
			logger.severe("Cannot find ColumnConfiguration file on classpath: "+resourceName); 
		}
	}
	
	/**
	 * Provided for testing purposes.
	 * @param config
	 */
	public ColumnConfigProvider(ColumnConfig config){
		this.config = config;
		init();
	}

	private void init() {
		// Create a map of each value
		map = new LinkedHashMap<String, HeaderData>();
		List<HeaderData> list = config.getColumns();
		for(HeaderData info: list){
			String id = info.getId();
			if(id == null) throw new IllegalArgumentException("Null id found for "+info.getClass().getName());
			// Map the id to info
			map.put(info.getId(), info);
		}
	}
	
	/**
	 * Iterate over all columns
	 * @return
	 */
	public Iterator<String> getKeyIterator(){
		return map.keySet().iterator();
	}
	
	/**
	 * Get a column by its key.
	 * @param key
	 * @return
	 */
	public HeaderData get(String key){
		return map.get(key);
	}
	
	/**
	 * Get all dependencies for a given column.
	 * @param id
	 * @return
	 */
	public List<String> getColumnDependancies(String id){
		LinkedHashSet<String> result = new LinkedHashSet<String>();
		HashSet<String> visted = new HashSet<String>();
		getColumnDependanciesRecursive(result, visted, id);
		// Convert back to a list		
		return new ArrayList<String>(result);
	}
	
	/**
	 * Will add all of the dependencies for each column found in the passed list.
	 * @param ids
	 * @return
	 */
	public List<String> addAllDependancies(List<String> ids){
		// To preserve the original order, we start by adding all of the
		// columns to the set.
		LinkedHashSet<String> result = new LinkedHashSet<String>(ids);
		HashSet<String> visted = new HashSet<String>();
		// Process each column
		for(String id: ids){
			getColumnDependanciesRecursive(result, visted, id);			
		}
		// Convert back to a list		
		return new ArrayList<String>(result);
	}
	
	/**
	 * Non-recursive check to get a columns dependencies.
	 * @param id
	 * @return
	 */
	private List<String> getColumnDependanciesImpl(String id){
		HeaderData data = get(id);
		if(data != null){
			if(data instanceof CompositeColumn){
				return ((CompositeColumn) data).getBaseDependencyIds();
			}else if(data instanceof UrlTemplate){
				return UrlTemplateUtil.getTempateDependencyIds((UrlTemplate) data);
			}
		}
		return null;
	}
	
	/**
	 * Recursive deep first search of the dependencies.
	 * @param result
	 * @param id
	 */
	private void getColumnDependanciesRecursive(LinkedHashSet<String> result, HashSet<String> visted, String id){
		// DFS
		List<String> children = getColumnDependanciesImpl(id);
		// Process each child
		if(children != null){
			for(String child: children){
				// Visit each id once. No cycles!
				if(!visted.contains(child)){
					visted.add(child);
					result.add(child);
					getColumnDependanciesRecursive(result, visted, child);
				}
			}
		}
	}
	
	/**
	 * Injects the default columns
	 * 
	 * @param defaults
	 */
	@Inject
	public void setDefaultDatasetColumns(@Named(ServerConstants.KEY_DEFAULT_DATASET_COLS) String defaults) {
		// convert from a string to a list
		List<String> keyList = splitCommaSeparatedString(defaults);
		// Add this list to the map
		defaultColumns.put(FromType.dataset.name(), keyList);
	}
	
	@Inject
	public void setAdditionalDatasetsColumns(@Named(ServerConstants.KEY_ADDITIONAL_DATASET_COLS) String additional){
		// convert from a string to a list
		List<String> keyList = splitCommaSeparatedString(additional);
		additoinalColumns.put(FromType.dataset.name(), keyList);
	}

	/**
	 * Helper to split a comma separated list of strings
	 * @param defaults
	 * @return
	 */
	private List<String> splitCommaSeparatedString(String defaults) {
		String[] split = defaults.split(",");
		List<String> keyList = new LinkedList<String>();
		for (int i = 0; i < split.length; i++) {
			keyList.add(split[i].trim());
		}
		return keyList;
	}
	
	/**
	 * Injects the default columns
	 * 
	 * @param defaults
	 */
	@Inject
	public void setDefaultLayerColumns(
			@Named(ServerConstants.KEY_DEFAULT_LAYER_COLS) String defaults) {
		List<String> keyList = splitCommaSeparatedString(defaults);
		// Add this list to the map
		defaultColumns.put(FromType.layer.name(), keyList);
	}
	
	/**
	 * Get the default column ids for a given type
	 * @param type
	 * @return
	 */
	public List<String> getDefaultColumnIds(String type) {
		return defaultColumns.get(type);
	}

	/**
	 * Get the full list
	 * @param type
	 * @return
	 */
	public List<String> getAdditionalColumnIds(String type) {
		return additoinalColumns.get(type);
	}
	
	/**
	 * Get the FullHeaderData for a given type.
	 * @param type
	 * @return
	 */
	public ColumnsForType getColumnsForType(String type){
		ColumnsForType cft = columnsForTypeCache.get(type);
		if(cft == null){
			cft = buildCache(type);
			columnsForTypeCache.put(type, cft);
		}
		return cft;
		
	}

	/**
	 * Used for lazy initialization
	 * @param type
	 * @return
	 */
	private ColumnsForType buildCache(String type) {
		// Using a set to ensure we only add columns once.
		HashSet<String> usedKeySet = new HashSet<String>();
		// First add all of the default columns for this type.
		List<String> defaultColumns = getDefaultColumnIds(type);
		// We must have default columns
		if(defaultColumns == null) throw new IllegalArgumentException("Cannot find any default columns for type: "+type);
		List<String> additionatlColumns = getAdditionalColumnIds(type);
		// Additional columns is optional
		if(additionatlColumns == null){
			additionatlColumns = new LinkedList<String>();
		}
		List<HeaderData> defaultHeaders = new LinkedList<HeaderData>();
		List<HeaderData> additionalHeaders = new LinkedList<HeaderData>();

		// First the defaults
		for(String id: defaultColumns){
			HeaderData column = this.map.get(id);
			if(column == null) throw new IllegalArgumentException("Unknown column id: "+id);
			if(!usedKeySet.contains(id)){
				usedKeySet.add(id);
				defaultHeaders.add(column);
			}
		}
		// Now add any additional that is not on the list
		for(String id: additionatlColumns){
			HeaderData column = this.map.get(id);
			if(column == null) throw new IllegalArgumentException("Unknown column id: "+id);
			if(!usedKeySet.contains(id)){
				usedKeySet.add(id);
				additionalHeaders.add(column);
			}
		}
		return new ColumnsForType(type, defaultHeaders, additionalHeaders);
	}

	/**
	 * Exposed for testing.
	 * @return
	 */
	public Map<String, ColumnsForType> getColumnsForTypeCache() {
		return columnsForTypeCache;
	}

}
