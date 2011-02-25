package org.sagebionetworks.web.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Logger;

import org.sagebionetworks.web.shared.CompositeColumn;
import org.sagebionetworks.web.shared.HeaderData;
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
		logger.info("Loaded column types:");
		for(HeaderData info: list){
			logger.info(info.getId());
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

}
