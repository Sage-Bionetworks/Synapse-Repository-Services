package org.sagebionetworks.web.server;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.UrlTemplate;
import org.springframework.web.util.UriTemplate;

/**
 * A utility for processing UrlTempates.  For each UrlTemplate column, the variables in the template 
 * string will be fetched for each row.  These value will be used to build a URI for each row.
 * 
 * @author jmhill
 *
 */
public class UrlTemplateUtil {
	
	/**
	 * Expand a URL using the passed values.
	 * @param url
	 * @param map
	 * @return
	 */
	public static URI expandUrl(String url, Map<String, ?> map){
		UriTemplate tempalte = new UriTemplate(url);
		return tempalte.expand(map);
	}
	
	/**
	 * Will first find all UrlTemplates in the list of HeaderData.  For each template
	 * found, process each row by reading in the variables 
	 * @param metaList the list of columns, of which some can be UrlTemplates.
	 * @param rows the populated list of rows.
	 */
	public static void processUrlTemplates(List<HeaderData> metaList, List<Map<String, Object>> rows) {
		if (metaList != null && rows != null) {
			// Find the UrlTemplate columns
			for (HeaderData header : metaList) {
				if (header instanceof UrlTemplate) {
					processUrlTempalte((UrlTemplate) header, rows);
				}
			}
		}
	}
	
	/**
	 * Process a single UrlTempalte column. The variable names will be extracted form the template
	 * string, and then for each row the variables will be fetched to build the final URL string.
	 * string
	 * @param meta
	 * @param rows
	 */
	public static void processUrlTempalte(UrlTemplate meta, List<Map<String, Object>> rows){
		if (meta.getUrlTemplate() != null) {
			UriTemplate template = new UriTemplate(meta.getUrlTemplate());
			List<String> variables = template.getVariableNames(); 
			if (variables != null) {
				// Now process each row
				for(Map<String, Object> row: rows){
					// Bind the variables to their values for this row
					URI uri = template.expand(row);
					// Add this value back to the row
					row.put(meta.getId(), uri.toString());
				}
			}
		}
	}

	/**
	 * What columns does this template depend on?
	 * @param data
	 * @return
	 */
	public static List<String> getTempateDependencyIds(UrlTemplate data) {
		if(data != null){
			UriTemplate template = new UriTemplate(data.getUrlTemplate());
			return template.getVariableNames(); 
		}
		return null;
	}

}
