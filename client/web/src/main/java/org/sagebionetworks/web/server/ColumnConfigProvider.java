package org.sagebionetworks.web.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

import org.sagebionetworks.web.shared.HeaderData;

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
	public ColumnConfigProvider(@Named("org.sagebionetworks.column.config.xml.resource") String resourceName){
		InputStream in = ColumnConfigProvider.class.getClassLoader().getResourceAsStream(resourceName);
		if(in != null){
			try{
				InputStreamReader reader = new InputStreamReader(in);
				// Load the from the xml
				this.config = ColumnConfig.fromXml(reader);
				// Create a map of each value
				map = new LinkedHashMap<String, HeaderData>();
				List<HeaderData> list = config.getColumns();
				for(HeaderData info: list){
					map.put(info.getId(), info);
				}
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

}
