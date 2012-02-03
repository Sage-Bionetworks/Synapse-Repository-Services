package org.sagebionetworks.client;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.search.query.FacetSort;
import org.sagebionetworks.repo.model.search.query.FacetTopN;
import org.sagebionetworks.repo.model.search.query.KeyList;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;

import com.amazonaws.services.cloudfront.model.InvalidArgumentException;

public class SearchUtil {

	public static String generateQueryString(SearchQuery searchQuery) {
		if (searchQuery == null) {
			throw new InvalidArgumentException("No search query was provided.");
		}

		List<String> params = new ArrayList<String>();
		List<String> q = searchQuery.getQueryTerm();
		List<KeyValue> bq = searchQuery.getBooleanQuery();

		// test for minimum search requirements
		if (!(q != null && q.size() > 0) && !(bq != null && bq.size() > 0)) {
			throw new InvalidArgumentException(
					"Either one queryTerm or one booleanQuery must be defined");
		}

		// query terms
		if (q != null && q.size() > 0)
			params.add("q=" + join(q, ","));

		// boolean query
		if (bq != null && bq.size() > 0) {
			for (KeyValue pair : bq) {
				// this regex is pretty lame to have. need to work continuous into KeyValue model
				String value = pair.getValue().contains("..") ? pair.getValue()
						: "'" + pair.getValue() + "'";									
				params.add("bq=" + pair.getKey() + ":" + value);
			}
		}

		// facets
		if (searchQuery.getFacet() != null && searchQuery.getFacet().size() > 0)
			params.add("facet=" + join(searchQuery.getFacet(), ","));

		// facet field constraints
		if (searchQuery.getFacetFieldConstraints() != null
				&& searchQuery.getFacetFieldConstraints().size() > 0) {
			for (KeyList pair : searchQuery.getFacetFieldConstraints()) {
				String key = "facet-" + pair.getKey() + "-constraints";
				params.add(key + "=" + join(pair.getValues(), ","));
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
				params.add(key + "=" + value);
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
			params.add("rank=" + join(searchQuery.getRank(), ","));

		// return-fields
		if (searchQuery.getReturnFields() != null
				&& searchQuery.getReturnFields().size() > 0)
			params.add("return-fields="
					+ join(searchQuery.getReturnFields(), ","));

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

}
