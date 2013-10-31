package org.sagebionetworks.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A Utility for creating a proxy of the Bridge Java Client that profiles all
 * calls and logs the results.
 * 
 * @author John
 * 
 */
public class BridgeProfileProxy {
	
    private static Logger log = LogManager.getLogger(BridgeProfileProxy.class);
	
	private static String MESSAGE_TEMPALTE = "%1$s in %2$,d ms";
	
    /**
     * Create a proxy of the Bridge Java that profiles all calls.
     * 
     * @param toProxy
     * @return
     */
    public static BridgeClient createProfileProxy(BridgeClient toProxy) {
        InvocationHandler handler = new BridgeInvocationHandler(toProxy);
        return (BridgeClient) Proxy.newProxyInstance(BridgeClient.class.getClassLoader(),
                new Class[] { BridgeClient.class }, handler);
	}
	
	/**
	 * This handler just times and logs each call.
	 *
	 */
    private static class BridgeInvocationHandler implements InvocationHandler {

        public BridgeInvocationHandler(BridgeClient wrapped) {
			super();
			this.wrapped = wrapped;
		}

        private BridgeClient wrapped;
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			// Start the timer
			long start = System.currentTimeMillis();
			try{
				return method.invoke(wrapped, args);
			} catch (InvocationTargetException e) {
				// We must catch InvocationTargetException to avoid UndeclaredThrowableExceptions
				// see: http://amitstechblog.wordpress.com/2011/07/24/java-proxies-and-undeclaredthrowableexception/
				throw e.getCause();
			} finally{
				long elapse = System.currentTimeMillis()-start;
				if(log.isTraceEnabled()){
					log.trace(String.format(MESSAGE_TEMPALTE, method.getName(), elapse));
				}
			}
		}
		
	}
}
