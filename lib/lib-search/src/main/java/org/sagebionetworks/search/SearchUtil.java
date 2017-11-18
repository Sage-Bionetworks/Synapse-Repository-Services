package org.sagebionetworks.search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.cloudsearchdomain.model.Bucket;
import com.amazonaws.services.cloudsearchdomain.model.BucketInfo;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.cloudsearchdomain.model.QueryParser;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import org.apache.commons.lang.math.NumberUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.Facet;
import org.sagebionetworks.repo.model.search.FacetConstraint;
import org.sagebionetworks.repo.model.search.FacetTypeNames;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import org.sagebionetworks.util.ValidateArgument;

public class SearchUtil{
	public static final Map<String, FacetTypeNames> FACET_TYPES;

	/**
	 * The index field holding the access control list info
	 */
	public static final String ACL_INDEX_FIELD = "acl";

	static {
		Map<String, FacetTypeNames> facetTypes = new HashMap<String, FacetTypeNames>();
		facetTypes.put("node_type", FacetTypeNames.LITERAL);
		facetTypes.put("disease", FacetTypeNames.LITERAL);
		facetTypes.put("tissue", FacetTypeNames.LITERAL);
		facetTypes.put("species", FacetTypeNames.LITERAL);
		facetTypes.put("platform", FacetTypeNames.LITERAL);
		facetTypes.put("created_by", FacetTypeNames.LITERAL);
		facetTypes.put("modified_by", FacetTypeNames.LITERAL);
		facetTypes.put("reference", FacetTypeNames.LITERAL);
		facetTypes.put("acl", FacetTypeNames.LITERAL);
		facetTypes.put("created_on", FacetTypeNames.DATE);
		facetTypes.put("modified_on", FacetTypeNames.DATE);
		facetTypes.put("num_samples", FacetTypeNames.CONTINUOUS);
		FACET_TYPES = Collections.unmodifiableMap(facetTypes);
	}


	public static SearchRequest generateSearchRequest(SearchQuery searchQuery, UserInfo userInfo){
		ValidateArgument.required(searchQuery, "searchQuery");
		SearchRequest searchRequest = new SearchRequest();
		//TODO:z TEST


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

		//add additional condition to filter for only documents that user can see
		if(!userInfo.isAdmin()){ //TODO:z TEST
			queryTermsStringBuilder.insert(0, "(and "); //add to the beginning of string
			queryTermsStringBuilder.append(formulateAuthorizationFilter(userInfo));
			queryTermsStringBuilder.append(')');
		}

		searchRequest.setQueryParser(QueryParser.Structured);
		searchRequest.setQuery(queryTermsStringBuilder.toString());

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
			StringBuilder facetStringBuilder = new StringBuilder();
			facetStringBuilder.append('{');
			for(String facetFieldName : searchQuery.getFacet()){
				if (facetStringBuilder.length() > 0){
					facetStringBuilder.append(',');
				}
				//no options inside {} since none are used by the webclient
				facetStringBuilder.append("\""+ facetFieldName + "\":{}");
			}
			facetStringBuilder.append('}');
			searchRequest.setFacet(facetStringBuilder.toString());
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
			searchRequest.setReturn(join(searchQuery.getReturnFields(), ","));

		// size
		if (searchQuery.getSize() != null)
			searchRequest.setSize(searchQuery.getSize());

		// start
		if (searchQuery.getStart() != null)
			searchRequest.setStart(searchQuery.getStart());

		return searchRequest;
	}

	//TODO: Test
	public static SearchResults convertToSynapseSearchResult(SearchResult cloudSearchResult){
		SearchResults synapseSearchResults = new SearchResults();

		//Handle Translating of facets
		Map<String, BucketInfo> facetMap = cloudSearchResult.getFacets();
		if(facetMap != null) {
			List<Facet> facetList = new ArrayList<>();

			for (Map.Entry<String, BucketInfo> facetInfo : facetMap.entrySet()) {//iterate over each facet

				String facetName = facetInfo.getKey();
				//TODO: REFACTOR AwesomeSearchFactory
				FacetTypeNames facetType = FACET_TYPES.get(facetName);
				if (facetType == null) {
					throw new IllegalArgumentException(
							"facet "
									+ facetName
									+ " is not properly configured, add it to the facet type map");
				}

				Facet synapseFacet = new Facet();
				synapseFacet.setName(facetName);
				synapseFacet.setType(facetType);
				//Note: min and max are never set since the frontend never makes use of them and so the results won't ever have them.

				BucketInfo bucketInfo = facetInfo.getValue();
				List<FacetConstraint> facetConstraints = new ArrayList<>();
				for (Bucket bucket: bucketInfo.getBuckets()){
					FacetConstraint facetConstraint = new FacetConstraint();
					facetConstraint.setValue(bucket.getValue());
					facetConstraint.setCount(bucket.getCount());
				}
				synapseFacet.setConstraints(facetConstraints);

				facetList.add(synapseFacet);
			}
			synapseSearchResults.setFacets(facetList);
		}

		Hits hits = cloudSearchResult.getHits();
		//class names are clashing feelsbadman
		List<org.sagebionetworks.repo.model.search.Hit> hitList = new ArrayList<>();

		if (hits != null) {
			synapseSearchResults.setFound(hits.getFound());
			synapseSearchResults.setStart(hits.getStart());


			for (com.amazonaws.services.cloudsearchdomain.model.Hit cloudSearchHit : hits.getHit()) {
				org.sagebionetworks.repo.model.search.Hit synapseHit = new org.sagebionetworks.repo.model.search.Hit();
				Map<String, List<String>> fieldsMap = cloudSearchHit.getFields();
				//TODO: test to make sure the values are correct

				synapseHit.setCreated_by(getFirstListValueFromMap(fieldsMap, "created_by"));
				synapseHit.setCreated_on(NumberUtils.createLong(getFirstListValueFromMap(fieldsMap, "created_on")));
				synapseHit.setDescription(getFirstListValueFromMap(fieldsMap, "description"));
				synapseHit.setDisease(getFirstListValueFromMap(fieldsMap, "disease"));
				synapseHit.setEtag(getFirstListValueFromMap(fieldsMap, "etag"));
				synapseHit.setId(getFirstListValueFromMap(fieldsMap, "id"));
				synapseHit.setModified_by(getFirstListValueFromMap(fieldsMap, "modified_by"));
				synapseHit.setModified_on(NumberUtils.createLong(getFirstListValueFromMap(fieldsMap, "modified_on")));
				synapseHit.setName(getFirstListValueFromMap(fieldsMap, "name"));
				synapseHit.setNode_type(getFirstListValueFromMap(fieldsMap, "node_type"));
				synapseHit.setNum_samples(NumberUtils.createLong(getFirstListValueFromMap(fieldsMap, "num_samples")));
				synapseHit.setTissue(getFirstListValueFromMap(fieldsMap, "tissue"));
				//synapseHit.setPath() also exists but there does not appear to be a path field in the cloudsearch anymore.

				hitList.add(synapseHit);
			}
		}
		synapseSearchResults.setHits(hitList);
		return synapseSearchResults;
	}


	private static String getFirstListValueFromMap(Map<String, List<String>> map, String key){
		//TODO: are we on Java 8 yet? switch to lambda function?
		List<String> value = map.get(key);
		return value == null || value.isEmpty() ? null : value.get(0);
	}

	//TODO: DELETE
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

	/**
	 * Formulate the boolean query to enforce an access controll list for a
	 * particular user
	 *
	 * @param userInfo
	 * @return the acl boolean query
	 * @throws DatastoreException
	 */
	public static String formulateAuthorizationFilter(UserInfo userInfo)
			throws DatastoreException {
		//TOOD:z test
		Set<Long> groups = userInfo.getGroups();
		if (0 == groups.size()) {
			// being extra paranoid here, this is unlikely
			throw new DatastoreException("no groups for user " + userInfo);
		}

		// Make our boolean query
		String authorizationFilter = "";
		for (Long group : groups) {
			if (0 < authorizationFilter.length()) {
				authorizationFilter += " ";
			}
			authorizationFilter += ACL_INDEX_FIELD
					+ ":'" + group + "'";
		}
		if (groups.size() > 1){
			authorizationFilter = "(or " + authorizationFilter + ")";
		}
		return authorizationFilter;
	}
}
