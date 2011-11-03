package org.sagebionetworks.web.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

public class HttpUtils {
	private static Logger logger = Logger.getLogger(HttpUtils.class.getName());

	
    public static String httpGet(String url) throws ClientProtocolException, IOException {
    	return httpGet(url, null);
    }

	public static String httpGet(String url, Map<String,String> params) throws ClientProtocolException, IOException {
        String responseString = null;

        HttpClient httpclient = new DefaultHttpClient();        
        try {        	
        	String fullUrl = url + "?" + paramsToString(params);
        	logger.info("GET:" + fullUrl);
            HttpGet httpGet = new HttpGet(fullUrl);                      
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            responseString = httpclient.execute(httpGet, responseHandler);            
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
        
        return responseString;
	}

    public static String httpPost(String url, Map<String,String> params) throws ClientProtocolException, IOException {
        String responseString = null;

        HttpClient httpclient = new DefaultHttpClient();
        try {
        	logger.info("GET:" + url + " params: " + paramsToString(params));
            HttpPost httppost = new HttpPost(url);
            List <NameValuePair> nvps = new ArrayList <NameValuePair>();
            for(String key : params.keySet()) {
                nvps.add(new BasicNameValuePair(key, params.get(key)));
            }
            httppost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            responseString = httpclient.execute(httppost, responseHandler);            
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
            
        return responseString;
    }

    public static void respondJSON(HttpServletResponse response, JSONObject obj) throws IOException {
        respondJSON(response, obj, null);
    }
    
    public static void respondJSON(HttpServletResponse response, JSONObject obj, String jsonpCallback) throws IOException {
        if(obj != null) {            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");            
            String json = obj.toString();
            if(jsonpCallback != null) {
                json = jsonpCallback + "(" + json + ")";
            }
            response.getWriter().write(json);
        }
    }

    public static void respondBadRequest(HttpServletResponse response, String reason) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");            
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        
        JSONObject obj = new JSONObject();
        try {
            obj.put("reason", reason);
        } catch (JSONException e) {
        }        
        response.getWriter().write(obj.toString());
    }

    public static void respondError(HttpServletResponse response, String reason) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");                    
        
        JSONObject obj = new JSONObject();
        try {
            obj.put("reason", reason);
        } catch (JSONException e) {
        }        
        response.getWriter().write(obj.toString());
    }

    
    /*
     * Private Methods
     */
	private static String paramsToString(Map<String, String> params) {
		String paramStr = "";
		for(String key : params.keySet()) {
			if(!paramStr.equals("")) paramStr += "&";        		
			paramStr += key + "=" + params.get(key);
		}
		return paramStr;
	}	

    
	
}
