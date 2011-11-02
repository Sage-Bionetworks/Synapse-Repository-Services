package org.sagebionetworks.web.server.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.sagebionetworks.web.server.HttpUtils;
import org.sagebionetworks.web.server.NcboUtils;

public class NcboSearchService extends HttpServlet {

	private static final String QUERY_PARAM = "query";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String searchTerm = request.getParameter(QUERY_PARAM);
		String url = NcboUtils.BIOPORTAL_SEARCH_URL + searchTerm;
		Map<String,String> params = new HashMap<String, String>();
		NcboUtils.addAPIKey(params);
		
		String searchResultXml = HttpUtils.httpGet(url, params);
		try {
			JSONObject searchResult = XML.toJSONObject(searchResultXml);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
}
