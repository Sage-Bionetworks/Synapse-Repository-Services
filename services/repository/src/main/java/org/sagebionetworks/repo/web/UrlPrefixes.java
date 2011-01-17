package org.sagebionetworks.repo.web;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.Message;

/**
 * This is a work-in progress.  Let's see if I can factor this info here.
 * 
 * @author deflaux
 *
 */
public class UrlPrefixes {

    public static final String MESSAGE = "/message";
    
    public static final Map<Class, String> MODEL2URL;
    
    static {
        MODEL2URL = new HashMap<Class, String>();
        MODEL2URL.put(Message.class, MESSAGE);
    }
    
}
