package org.sagebionetworks.search;

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
	public static final String CREATED_BY_RETURN_FIELD = "created_by_r";
	public static final String CREATED_ON_FIELD = "created_on";
	public static final String DESCRIPTION_FIELD = "description";
	public static final String DISEASE_FACET_FIELD = "disease";
	public static final String DISEASE_RETURN_FIELD = "disease_r";
	public static final String ETAG_FIELD = "etag";
	public static final String ID_FIELD = "id";
	public static final String MODIFIED_BY_RETURN_FIELD = "modified_by_r";
	public static final String MODIFIED_ON_FIELD = "modified_on";
	public static final String NAME_FIELD = "name";
	public static final String NODE_TYPE_FACET_FIELD = "node_type";
	public static final String NODE_TYPE_RETURN_FIELD = "node_type_r";
	public static final String NUM_SAMPLES_FIELD = "num_samples";
	public static final String SPECIES_FACET_FIELD = "species";
	public static final String TISSUE_FACET_FIELD = "tissue";
	public static final String TISSUE_RETURN_FIELD = "tissue_r";
	public static final String PLATFORM_FACET_FIELD = "platform";
	public static final String CREATED_BY_FACET_FIELD = "created_by";
	public static final String MODIFIED_BY_FACET_FIELD = "modified_by";
	public static final String REFERENCE_FACET_FIELD = "reference";



	static {
		Map<String, FacetTypeNames> facetTypes = new HashMap<String, FacetTypeNames>();
		facetTypes.put(NODE_TYPE_FACET_FIELD, FacetTypeNames.LITERAL);
		facetTypes.put(DISEASE_FACET_FIELD, FacetTypeNames.LITERAL);
		facetTypes.put(TISSUE_FACET_FIELD, FacetTypeNames.LITERAL);
		facetTypes.put(SPECIES_FACET_FIELD, FacetTypeNames.LITERAL);
		facetTypes.put(PLATFORM_FACET_FIELD, FacetTypeNames.LITERAL);
		facetTypes.put(CREATED_BY_FACET_FIELD, FacetTypeNames.LITERAL);
		facetTypes.put(MODIFIED_BY_FACET_FIELD, FacetTypeNames.LITERAL);
		facetTypes.put(REFERENCE_FACET_FIELD, FacetTypeNames.LITERAL);
		facetTypes.put(ACL_INDEX_FIELD, FacetTypeNames.LITERAL);
		facetTypes.put(CREATED_ON_FIELD, FacetTypeNames.DATE);
		facetTypes.put(MODIFIED_ON_FIELD, FacetTypeNames.DATE);
		facetTypes.put(NUM_SAMPLES_FIELD, FacetTypeNames.CONTINUOUS);
		FACET_TYPES = Collections.unmodifiableMap(facetTypes);
	}


	public static SearchRequest generateSearchRequest(SearchQuery searchQuery){
		ValidateArgument.required(searchQuery, "searchQuery");
		SearchRequest searchRequest = new SearchRequest();

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
			queryTermsStringBuilder.append( (queryTermsStringBuilder.length() > 0 ? " ":"") + String.join(" ", bqTerms)+ ")");
			queryTermsStringBuilder.insert(0, "(and "); //add to the beginning of string
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
			StringBuilder facetStringBuilder = new StringBuilder('{');
			int initialStrBuilderLen = facetStringBuilder.length();
			for(String facetFieldName : searchQuery.getFacet()){
				if (facetStringBuilder.length() > initialStrBuilderLen){
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
			searchRequest.setReturn(String.join(",", searchQuery.getReturnFields()));

		// size
		if (searchQuery.getSize() != null)
			searchRequest.setSize(searchQuery.getSize());

		// start
		if (searchQuery.getStart() != null)
			searchRequest.setStart(searchQuery.getStart());

		return searchRequest;
	}

	public static SearchResults convertToSynapseSearchResult(SearchResult cloudSearchResult){
		SearchResults synapseSearchResults = new SearchResults();

		//Handle Translating of facets
		List<Facet> facetList = new ArrayList<>();
		Map<String, BucketInfo> facetMap = cloudSearchResult.getFacets();
		if(facetMap != null) {
			for (Map.Entry<String, BucketInfo> facetInfo : facetMap.entrySet()) {//iterate over each facet
				facetList.add(convertToSynapseSearchFacet(facetInfo.getKey(), facetInfo.getValue()));
			}
		}
		synapseSearchResults.setFacets(facetList);

		//Handle translation of Hits
		//class names are clashing feelsbadman
		List<org.sagebionetworks.repo.model.search.Hit> hitList = new ArrayList<>();
		Hits hits = cloudSearchResult.getHits();
		if (hits != null) {
			synapseSearchResults.setFound(hits.getFound());
			synapseSearchResults.setStart(hits.getStart());

			for (com.amazonaws.services.cloudsearchdomain.model.Hit cloudSearchHit : hits.getHit()) {
				hitList.add(convertToSynapseHit(cloudSearchHit));
			}
		}
		synapseSearchResults.setHits(hitList);

		return synapseSearchResults;
	}

	private static Facet convertToSynapseSearchFacet(String facetName, BucketInfo bucketInfo) {
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
		// Note: min and max are never set since the frontend never makes use of them and so the results won't ever have them.
		// A IllegalArgumentException would have been throw when converting from Synapse's SearchQuery to Amazon's SearchRequest

		List<FacetConstraint> facetConstraints = new ArrayList<>();
		for (Bucket bucket: bucketInfo.getBuckets()){
			FacetConstraint facetConstraint = new FacetConstraint();
			facetConstraint.setValue(bucket.getValue());
			facetConstraint.setCount(bucket.getCount());
			facetConstraints.add(facetConstraint);
		}
		synapseFacet.setConstraints(facetConstraints);
		return synapseFacet;
	}

	private static org.sagebionetworks.repo.model.search.Hit convertToSynapseHit(com.amazonaws.services.cloudsearchdomain.model.Hit cloudSearchHit){
		Map<String, List<String>> fieldsMap = cloudSearchHit.getFields();

		org.sagebionetworks.repo.model.search.Hit synapseHit = new org.sagebionetworks.repo.model.search.Hit();
		synapseHit.setCreated_by(getFirstListValueFromMap(fieldsMap, CREATED_BY_RETURN_FIELD));
		synapseHit.setCreated_on(NumberUtils.createLong(getFirstListValueFromMap(fieldsMap, CREATED_ON_FIELD)));
		synapseHit.setDescription(getFirstListValueFromMap(fieldsMap, DESCRIPTION_FIELD));
		synapseHit.setDisease(getFirstListValueFromMap(fieldsMap, DISEASE_RETURN_FIELD));
		synapseHit.setEtag(getFirstListValueFromMap(fieldsMap, ETAG_FIELD));
		synapseHit.setId(getFirstListValueFromMap(fieldsMap, ID_FIELD));
		synapseHit.setModified_by(getFirstListValueFromMap(fieldsMap, MODIFIED_BY_RETURN_FIELD));
		synapseHit.setModified_on(NumberUtils.createLong(getFirstListValueFromMap(fieldsMap, MODIFIED_ON_FIELD)));
		synapseHit.setName(getFirstListValueFromMap(fieldsMap, NAME_FIELD));
		synapseHit.setNode_type(getFirstListValueFromMap(fieldsMap, NODE_TYPE_RETURN_FIELD));
		synapseHit.setNum_samples(NumberUtils.createLong(getFirstListValueFromMap(fieldsMap, NUM_SAMPLES_FIELD)));
		synapseHit.setTissue(getFirstListValueFromMap(fieldsMap, TISSUE_RETURN_FIELD));
		//synapseHit.setPath() also exists but there does not appear to be a path field in the cloudsearch anymore.
		return synapseHit;
	}


	private static String getFirstListValueFromMap(Map<String, List<String>> map, String key){
		ValidateArgument.required(map, "map");
		List<String> list = map.get(key);
		return (list == null || list.isEmpty()) ? null : list.get(0);
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
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(userInfo.getGroups(), "userInfo.getGroups()");

		Set<Long> groups = userInfo.getGroups();
		if (groups.isEmpty()) {
			// being extra paranoid here, this is unlikely
			throw new DatastoreException("no groups for user " + userInfo);
		}

		// Make our boolean query
		StringBuilder authorizationFilterBuilder = new StringBuilder("(or ");
		int initialLen = authorizationFilterBuilder.length();
		for (Long group : groups) {
			if (authorizationFilterBuilder.length() > initialLen) {
				authorizationFilterBuilder.append(" ");
			}
			authorizationFilterBuilder.append(ACL_INDEX_FIELD).append(":'").append(group).append("'");
		}
		authorizationFilterBuilder.append(")");

		return authorizationFilterBuilder.toString();
	}
}
