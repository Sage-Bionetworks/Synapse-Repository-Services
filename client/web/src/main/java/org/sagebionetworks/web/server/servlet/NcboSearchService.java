package org.sagebionetworks.web.server.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.sagebionetworks.web.server.HttpUtils;
import org.sagebionetworks.web.server.NcboUtils;

public class NcboSearchService extends HttpServlet {

	private static final String QUERY_PARAM = "query";
	private static final String JSONP_PARAM = "callback";
	private static final String LIMIT_PARAM = "limit";
	private static final String OFFSET_PARAM = "offset";
	private static final String ONTOLOGYIDS_PARAM = "ontologyids";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// required params
		String searchTerm = request.getParameter(QUERY_PARAM);
		
		// optional params
		String jsonpCallback = request.getParameter(JSONP_PARAM);
		String limit = request.getParameter(LIMIT_PARAM); 
		String offsetStr = request.getParameter(OFFSET_PARAM);
		Integer offset = Integer.parseInt(offsetStr);
		String ontologyids = request.getParameter(ONTOLOGYIDS_PARAM);

		if(searchTerm == null) {
			HttpUtils.respondBadRequest(response, "Required parameter: " + QUERY_PARAM + " not provided.");		
		} else {
			
			String url = NcboUtils.BIOPORTAL_SEARCH_URL + searchTerm;
			Map<String,String> params = new HashMap<String, String>();
			NcboUtils.addAPIKey(params);
			if(limit != null) params.put("pagesize", limit);
			if(offset != null) params.put("pagenum", Integer.toString(offset + 1));			
			if(ontologyids != null) params.put(ONTOLOGYIDS_PARAM, ontologyids);
			
			try {
				String searchResultXml = HttpUtils.httpGet(url, params);
				JSONObject searchResult = XML.toJSONObject(searchResultXml);
				JSONObject returnObj = new JSONObject();
				
				// empty results to start
				returnObj.put("numResultsTotal", 0);
				returnObj.put("searchBean", new JSONArray());
				
				if(searchResult != null && searchResult.has("success") 
						&& searchResult.getJSONObject("success").has("data")
						&& searchResult.getJSONObject("success").getJSONObject("data").has("page")) {
					JSONObject pageObj = searchResult.getJSONObject("success").getJSONObject("data").getJSONObject("page");
					// set total results
					if(pageObj.has("numResultsTotal")) returnObj.put("numResultsTotal", pageObj.getInt("numResultsTotal"));
					// set search results
					if(pageObj.has("contents") 
							&& pageObj.getJSONObject("contents").has("searchResultList")
							&& pageObj.getJSONObject("contents").getJSONObject("searchResultList").has("searchBean")) {
						JSONArray searchBean = pageObj.getJSONObject("contents").getJSONObject("searchResultList").getJSONArray("searchBean");
						returnObj.put("searchBean", searchBean);
					}
						
				}
				HttpUtils.respondJSON(response, returnObj, jsonpCallback);
			} catch (Exception e) {
				HttpUtils.respondError(response, "An error occured accessing the NCBO search service");
			}
		}		
	}

	
}
