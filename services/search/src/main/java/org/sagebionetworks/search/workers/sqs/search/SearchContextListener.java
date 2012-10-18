package org.sagebionetworks.search.workers.sqs.search;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Binds the message processing to the starting and stopping of the web application.
 * @author jmhill
 *
 */
public class SearchContextListener implements ServletContextListener {
	
	static private Log log = LogFactory.getLog(SearchContextListener.class);
	
	/**
	 * The Spring context for this application.
	 */
	ClassPathXmlApplicationContext context;


	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		if(context != null){
			log.info("Stopping context");
			context.stop();
			context.close();
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		// Start it up.
		log.info("Starting context");
		context = new  ClassPathXmlApplicationContext(new String[]{"sqs-manager-spb.xml"});
		context.start();
		Object scheduler = context.getBean("mainScheduler");
		if(scheduler == null){
			throw new RuntimeException("Failed to start the Spring application context. The 'mainScheduler' was null");
		}
	}

}
