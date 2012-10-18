package org.sagebionetworks.repo.web.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.search.query.FacetSort;
import org.sagebionetworks.repo.model.search.query.FacetTopN;
import org.sagebionetworks.repo.model.search.query.KeyList;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;

import com.amazonaws.services.cloudfront.model.InvalidArgumentException;

public class SearchUtil {

	public static String generateQueryString(SearchQuery searchQuery) throws UnsupportedEncodingException {
		if (searchQuery == null) {
			throw new InvalidArgumentException("No search query was provided.");
		}

		List<String> params = new ArrayList<String>();
		List<String> q = searchQuery.getQueryTerm();
		List<KeyValue> bq = searchQuery.getBooleanQuery();
		
		// clean up empty q
		if(q != null && q.size() == 1 && "".equals(q.get(0))) {
			q = null;
		}		

		// clean up empty q
		if(q != null && q.size() == 1 && "".equals(q.get(0))) {
			q = null;
		}		
		
		// test for minimum search requirements
		if (!(q != null && q.size() > 0) && !(bq != null && bq.size() > 0)) {
			throw new InvalidArgumentException(
					"Either one queryTerm or one booleanQuery must be defined");
		}

		// query terms
		if (q != null && q.size() > 0)
			params.add("q=" + URLEncoder.encode(join(q, ","), "UTF-8"));

		// boolean query
		if (bq != null && bq.size() > 0) {
			for (KeyValue pair : bq) {
				// this regex is pretty lame to have. need to work continuous into KeyValue model
				String value = pair.getValue().contains("..") ? pair.getValue()
						: "'" + escapeQuotedValue(pair.getValue()) + "'";
				String term = pair.getKey() + ":" + value; 
				if(pair.getNot() != null && pair.getNot()) {
					term = "(not " + term + ")";
				}
				params.add("bq=" + URLEncoder.encode(term, "UTF-8"));
			}
		}

		// facets
		if (searchQuery.getFacet() != null && searchQuery.getFacet().size() > 0)
			params.add("facet=" + URLEncoder.encode(join(searchQuery.getFacet(), ","), "UTF-8"));

		// facet field constraints
		if (searchQuery.getFacetFieldConstraints() != null
				&& searchQuery.getFacetFieldConstraints().size() > 0) {
			for (KeyList pair : searchQuery.getFacetFieldConstraints()) {
				String key = "facet-" + pair.getKey() + "-constraints";
				
				// quote and escape strings but not numbers or ranges
				List<String> values = new ArrayList<String>();
				for(String value : pair.getValues()) {
					if(!value.contains("..") && !isNumeric(value)) {
						value = "'" + escapeQuotedValue(value) + "'";
						value = value.replaceAll(",","\\\\,"); // replace , -> \,  -- do this after the escapeQuotedValue
						values.add(value);
					} else {
						values.add(value);
					}
				}
				params.add(key + "=" + URLEncoder.encode(join(values, ","), "UTF-8"));
			}
		}

		// facet field sort
		if (searchQuery.getFacetFieldSort() != null
				&& searchQuery.getFacetFieldSort().size() > 0) {
			for (FacetSort facetSort : searchQuery.getFacetFieldSort()) {
				String key = "facet-" + facetSort.getFacetName() + "-sort";
				String value = null;
				switch (facetSort.getSortType()) {
				case ALPHA:
					value = "alpha";
					break;
				case COUNT:
					value = "count";
					break;
				case MAX:
					if (facetSort.getMaxfield() != null) {
						value = "max(" + facetSort.getMaxfield() + ")";
					} else {
						throw new InvalidArgumentException(
								"maxField must be set for type: "
										+ facetSort.getSortType());
					}
					break;
				case SUM:
					if (facetSort.getSumFields() != null
							&& facetSort.getSumFields().size() > 0) {
						value = "sum(" + join(facetSort.getSumFields(), ",")
								+ ")";
					} else {
						throw new InvalidArgumentException(
								"sumFields must contain at least one value for type: "
										+ facetSort.getSortType());
					}
					break;
				default:
					throw new InvalidArgumentException(
							"Unknown Facet Field Sort: "
									+ facetSort.getSortType());
				}
				params.add(key + "=" + URLEncoder.encode(value, "UTF-8"));
			}
		}

		// face top n
		if (searchQuery.getFacetFieldTopN() != null
				&& searchQuery.getFacetFieldTopN().size() > 0) {
			for (FacetTopN topN : searchQuery.getFacetFieldTopN()) {
				params.add("facet-" + topN.getKey() + "-top-n="
						+ topN.getValue());
			}
		}

		// rank
		if (searchQuery.getRank() != null && searchQuery.getRank().size() > 0)
			params.add("rank=" + URLEncoder.encode(join(searchQuery.getRank(), ","), "UTF-8"));

		// return-fields
		if (searchQuery.getReturnFields() != null
				&& searchQuery.getReturnFields().size() > 0)
			params.add("return-fields="
					+ URLEncoder.encode(join(searchQuery.getReturnFields(), ","), "UTF-8"));

		// size
		if (searchQuery.getSize() != null)
			params.add("size=" + searchQuery.getSize());

		// start
		if (searchQuery.getStart() != null)
			params.add("start=" + searchQuery.getStart());

		// TODO : support t-FIELD ?

		return join(params, "&");
	}

	/*
	 * Private Methods
	 */
	private static String join(List<String> list, String delimiter) {
		StringBuilder sb = new StringBuilder();
		for (String item : list) {
			sb.append(item);
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
