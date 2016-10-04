package org.sagebionetworks.repo.web.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;

public class SearchUtil {
	
	public static String generateStructuredQueryString(SearchQuery searchQuery) throws UnsupportedEncodingException{
		if (searchQuery == null) {
			throw new IllegalArgumentException("No search query was provided.");
		}

		List<String> params = new ArrayList<String>();
		List<String> q = searchQuery.getQueryTerm();
		List<KeyValue> bq = searchQuery.getBooleanQuery();
		StringBuilder queryTermsStringBuilder = new StringBuilder();
		
		// clean up empty q
		if(q != null && q.size() == 1 && "".equals(q.get(0))) {
			q = null;
		}
		
		// test for minimum search requirements
		if (!(q != null && q.size() > 0) && !(bq != null && bq.size() > 0)) {
			throw new IllegalArgumentException(
					"Either one queryTerm or one booleanQuery must be defined");
		}

		// unstructured query terms into structured query terms
		if (q != null && q.size() > 0)
			queryTermsStringBuilder.append("(and " + joinQueries(q, " ") + ")");

		// boolean query into structured query terms
		if (bq != null && bq.size() > 0) {
			List<String> bqTerms = new ArrayList<String>();
			for (KeyValue pair : bq) {
				// this regex is pretty lame to have. need to work continuous into KeyValue model
				String value = pair.getValue();
				
				if(value.contains("*")){ //prefix queries are treated differently
					String prefixQuery = createPrefixQuery(value, pair.getKey());
					bqTerms.add(prefixQuery);
					continue;
				}
				
				//convert numeric ranges from 2011 cloudsearch syntax to 2013 syntax, for example: 200.. to [200,}
				if(value.contains("..")) {
					//TODO: remove this part once client stops using ".." notation for ranges
					String[] range = value.split("\\.\\.", -1);
					
					if(range.length != 2 ){
						throw new IllegalArgumentException("Numeric range is incorrectly formatted");
					}
					
					StringBuilder rangeStringBuilder = new StringBuilder();
					//left bound
					if(range[0].equals("")){
						rangeStringBuilder.append("{");
					}else{
						rangeStringBuilder.append("[" + range[0]);
					}
					
					//right bound
					rangeStringBuilder.append(",");
					if(range[1].equals("")){
						rangeStringBuilder.append("}");
					}else{
						rangeStringBuilder.append( range[1] + "]");
					}
					value = rangeStringBuilder.toString();
				}
				
				if((value.contains("{") || value.contains("[")) 
					&& (value.contains("}") || value.contains("]")) ){ //if is a continuous range such as [300,}
					bqTerms.add("(range field=" + pair.getKey()+ " " + value + ")");
					continue;
				}
				
				//add quotes around value. i.e. value -> 'value'
				value = "'" + escapeQuotedValue(pair.getValue()) + "'"; 
				String term = pair.getKey() + ":" + value; 
				if(pair.getNot() != null && pair.getNot()) {
					term = "(not " + term + ")";
				}
				bqTerms.add(term);
			}
			
			//turns it from (and <q1> <q2> ... <qN>) into (and (and <q1> <q2> ... <qN>) <bqterm1> <bqterm2> ... <bqtermN>)
			queryTermsStringBuilder.append( (queryTermsStringBuilder.length() > 0 ? " ":"") + join(bqTerms, " ")+ ")");
			queryTermsStringBuilder.insert(0, "(and "); //add to the beginning of string
		}
		
		params.add("q.parser=structured");
		params.add("q=" + URLEncoder.encode(queryTermsStringBuilder.toString(), "UTF-8"));
		
		//preprocess the FacetSortConstraints
		// facet field constraints
		if (searchQuery.getFacetFieldConstraints() != null
				&& searchQuery.getFacetFieldConstraints().size() > 0) {
			throw new IllegalArgumentException("Facet field constraints are no longer supported");
		}
		if (searchQuery.getFacetFieldSort() != null){
			throw new IllegalArgumentException("Sorting of facets is no longer supported");
		}
		
		// facets
		if (searchQuery.getFacet() != null && searchQuery.getFacet().size() > 0){ //iterate over all facets
			for(String facetFieldName : searchQuery.getFacet()){
				//no options inside {} since none are used by the webclient 
				params.add("facet." + facetFieldName +"=" + URLEncoder.encode("{}", "UTF-8"));
			}
		}
		
		//switch to size parameter in facet
		// facet top n
		if (searchQuery.getFacetFieldTopN() != null) {
			throw new IllegalArgumentException("facet-field-top-n is no longer supported");
		}
		
		// rank
		if (searchQuery.getRank() != null){
			throw new IllegalArgumentException("Rank is no longer supported");
		}
		

		// return-fields
		if (searchQuery.getReturnFields() != null
				&& searchQuery.getReturnFields().size() > 0)
			params.add("return="
					+ URLEncoder.encode(join(searchQuery.getReturnFields(), ","), "UTF-8"));

		// size
		if (searchQuery.getSize() != null)
			params.add("size=" + searchQuery.getSize());

		// start
		if (searchQuery.getStart() != null)
			params.add("start=" + searchQuery.getStart());

		return join(params, "&");
	}

	/*
	 * Private Methods
	 */
	/**
	 * Creates a prefix query if there is an asterisk
	 * @param prefixStringWithAsterisk prefix string containing the * symbol
	 * @param fieldName optional. used in boolean queries but not in regular queries. 
	 * @return
	 */
	private static String createPrefixQuery(String prefixStringWithAsterisk, String fieldName){
		int asteriskIndex = prefixStringWithAsterisk.indexOf('*');
		if(asteriskIndex == -1){
			throw new IllegalArgumentException("the prefixString does not contain an * (asterisk) symbol");
		}
		return "(prefix" + (fieldName==null ? "" : " field=" + fieldName) + " '" + prefixStringWithAsterisk.substring(0, asteriskIndex) + "')";
	}
	
	
	private static String join(List<String> list, String delimiter){
		return joinHelper(list, delimiter, false);
	}
	
	private static String joinQueries(List<String> list, String delimiter){
		return joinHelper(list, delimiter, true);
	}
	
	private static String joinHelper(List<String> list, String delimiter, boolean forQueries) {
		StringBuilder sb = new StringBuilder();
		for (String item : list) {
			if(forQueries){
				if(item.contains("*")){
					sb.append(createPrefixQuery(item, null));
				}else{
					sb.append('\''); //appends ' character
					sb.append(item);
					sb.append('\'');
				}
			}else{
				sb.append(item);
			}
			sb.append(delimiter);
		}
		String str = sb.toString();
		if (str.length() > 0) {
			str = str.substring(0, str.length()-1);
		}
		return str;
	}

	private static String escapeQuotedValue(String value) {
		value = value.replaceAll("\\\\", "\\\\\\\\"); // replace \ -> \\
		value = value.replaceAll("'", "\\\\'"); // replace ' -> \'
		return value;
	}
	
	private static boolean isNumeric(String str) {
		try {
			double d = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}
	
}
