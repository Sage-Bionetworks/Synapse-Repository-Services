package org.sagebionetworks.repo.model.gaejdo;

import javax.jdo.JDOHelper; 
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory; 
 
public final class PMF { 
    private static final PersistenceManagerFactory pmfInstance = 
        JDOHelper.getPersistenceManagerFactory("transactions-optional"); 
    
    //private static PersistenceManager pm = null;
 
    private PMF() {} 
 
    public static PersistenceManager get() { 
    	return pmfInstance.getPersistenceManager(); 
//    	if (pm==null) {
//    		pm = pmfInstance.getPersistenceManager(); 
//    	}
//        return pm;
    } 
    
//    public static void close() {
//    	if (pm!=null) pm.close();
//    	pm=null;
//    }
}