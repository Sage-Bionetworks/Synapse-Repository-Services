package org.sagebionetworks.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A Utility for creating a proxy of the Synapse Java Client that profiles all calls and logs the results.
 * 
 * @author John
 *
 */
public class SynapseProfileProxy {
	
	private static Logger log = LogManager.getLogger(SynapseProfileProxy.class);
	
	private static String MESSAGE_TEMPALTE = "%1$s in %2$,d ms";
	/**
	 * Create a proxy of the Synapse Java that profiles all calls. 
	 * @param toProxy
	 * @return
	 */
	public static SynapseInt createProfileProxy(SynapseInt toProxy){
	     InvocationHandler handler = new SynapseInvocationHandler(toProxy);
	     return (SynapseInt) Proxy.newProxyInstance(SynapseInt.class.getClassLoader(),
					new Class[] { SynapseInt.class }, handler);
	}
	
	/**
	 * This handler just times and logs each call.
	 *
	 */
	private static class SynapseInvocationHandler implements InvocationHandler {

		public SynapseInvocationHandler(SynapseInt wrapped) {
			super();
			this.wrapped = wrapped;
		}

		private SynapseInt wrapped;
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			// Start the timer
			long start = System.currentTimeMillis();
			try{
				return method.invoke(wrapped, args);
			}finally{
				long elapse = System.currentTimeMillis()-start;
				if(log.isTraceEnabled()){
					log.trace(String.format(MESSAGE_TEMPALTE, method.getName(), elapse));
				}
			}
		}
		
	}
}
