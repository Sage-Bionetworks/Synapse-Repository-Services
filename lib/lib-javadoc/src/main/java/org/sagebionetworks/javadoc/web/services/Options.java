package org.sagebionetworks.javadoc.web.services;

/**
 * Options used by this doclet.
 * 
 * @author John
 *
 */
public class Options {


	/**
	 * Used to indicate the oupt directory.
	 */
    public static final String DIRECTORY_FLAG = "-d";

	/**
     * Check for doclet added options here.
     *
     * @return number of arguments to option. Zero return means
     * option not known.  Negative value means error occurred.
     */
    public static int optionLength(String option) {
    	if(DIRECTORY_FLAG.equals(option)){
    		// this flag is composed of two parts: '-d' and the path.
    		return 2;
    	}else{
    		// unknown
            return 0;
    	}
    }
    
    /**
     * For options composed of a key and value, get the value.
     * @param options
     * @return
     */
    public static String getOptionValue(String[][] options, String key){
    	for(int i=0; i<options.length; i++){
    		if(options[i].length == 2){
    			if(key.equals(options[i][0])){
    				return options[i][1];
    			}
    		}
    	}
    	// Null if not found
    	return null;
    }
}
