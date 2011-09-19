package org.sagebionetworks.repopipeline;

import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;


public class ScriptDownloader {
	
	static {
		DAVRepositoryFactory.setup();
	}
	
	// this can be used to check url and user credentials
	// will throw exception if it can't connect
	public static void testConnection(String urlString, String username, String password) throws SVNException {
		SVNURL url= SVNURL.parseURIDecoded(urlString);
		SVNRepository repository = SVNRepositoryFactory.create(url);

        ISVNAuthenticationManager authManager =
        	new BasicAuthenticationManager(username, password);
        
       //set an auth manager which will provide user credentials
        repository.setAuthenticationManager(authManager);
        
        repository.testConnection();
	}
	
    /*
     * returns the number of the revision at which the working copy is 
     */
    public static long checkout(String urlString, String username, String password, SVNRevision revision, File destPath) throws SVNException {
    	//1.default options and authentication drivers to use
        SVNClientManager clientManager = SVNClientManager.newInstance();
        
        ISVNAuthenticationManager authManager =
        	new BasicAuthenticationManager(username, password);
        
        clientManager.setAuthenticationManager(authManager);

        SVNUpdateClient updateClient = clientManager.getUpdateClient( );
        /*
         * sets externals not to be ignored during the checkout
         */
        updateClient.setIgnoreExternals( false );
        
        SVNDepth depth = SVNDepth.INFINITY;
        boolean allowUnversionedObstructions = true;
		SVNURL url= SVNURL.parseURIDecoded(urlString);


        return updateClient.doCheckout( url , destPath , revision , revision , depth,  allowUnversionedObstructions );
    }

}
