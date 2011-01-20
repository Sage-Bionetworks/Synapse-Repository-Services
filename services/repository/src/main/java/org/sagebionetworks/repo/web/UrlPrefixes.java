package org.sagebionetworks.repo.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.Comment;
import org.sagebionetworks.repo.model.Message;

/**
 * Mapping of URLs to model classes
 * 
 * @author deflaux
 *
 */
@SuppressWarnings("unchecked")
public class UrlPrefixes {

    /**
     * URL prefix for comment model objects
     * 
     */
    public static final String COMMENT = "/comment";

    /**
     * URL prefix for dataset model objects
     * 
     */
    public static final String DATASET = "/dataset";

    /**
     * URL prefix for entity model objects<p>
     * 
     * Entities are maps.  This is experimental 
     * and this code for this is not checked in yet.
     * 
     */
    public static final String ENTITY = "/entity";
    
    /**
     * URL prefix for message objects
     * 
     */
    public static final String MESSAGE = "/message";
    
    /**
     * Mapping of type to url prefix
     */
    private static final Map<Class, String> MODEL2URL;
    
    static {
        Map<Class, String> model2url = new HashMap<Class, String>();
        model2url.put(Comment.class, COMMENT);
        model2url.put(Object.class, DATASET);
        model2url.put(Object.class, ENTITY);
        model2url.put(Message.class, MESSAGE);
        MODEL2URL = Collections.unmodifiableMap(model2url);
    }

    /**
     * Determine the controller URL prefix for a given model class
     * 
     * @param theModelClass 
     * @return the URL for the model class
     */
    public static String getUrlForModel(Class theModelClass) {
        return MODEL2URL.get(theModelClass);
    }
    
}
