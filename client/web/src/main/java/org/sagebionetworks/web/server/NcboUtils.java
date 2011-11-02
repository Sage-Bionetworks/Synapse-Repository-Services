package org.sagebionetworks.web.server;

import java.util.Map;

public class NcboUtils {

    public static final String BIOPORTAL_BASE_URL = "http://rest.bioontology.org";
    public static final String BIOPORTAL_ONTOLOGY_ID_URL_BASE = "http://bioportal.bioontology.org/ontologies/"; // append id
    public static final String BIOPORTAL_RECOMMENDER_URL = BIOPORTAL_BASE_URL + "/recommender";
    public static final String BIOPORTAL_SEARCH_URL = BIOPORTAL_BASE_URL + "/bioportal/search/";
    
    public static final String BIOPORTAL_API_KEY = "60c5ce35-9acd-4cd4-ac6a-0334c0e13871"; // db's    
    public static final String JSONP_CALLBACK_PARAM = "callback";
           
    public static void addAPIKey(Map<String,String> params) {
        params.put("apikey", BIOPORTAL_API_KEY);
    }
        
}
