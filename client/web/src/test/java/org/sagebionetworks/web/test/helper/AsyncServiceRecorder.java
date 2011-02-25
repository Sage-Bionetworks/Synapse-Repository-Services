package org.sagebionetworks.web.test.helper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.Map;

/**
 * A test helper to aid with testing Asynchronous service calls. The class
 * intercepts asynchronous calls, and records the methods, parameters and
 * callbacks. After the calls are recored, they can be played back using the
 * synchronous service.
 * 
 * So why would anyone need such a utility?
 * 
 * GWT asynchronous services have two parts, a synchronous interface (extends
 * RemoteService), and a complementary asynchronous interface. Presenter classes
 * (MVP) communicate the the server (via RPC) using an auto-generated
 * implementation of the asynchronous interface. In order to test the business
 * logic in Presenter classes, the presenter must be provided with mock or stub
 * implementation of the asynchronous service.
 * 
 * The simplest stub implementation of one of these asynchronous service, will
 * provide an in-line implementation of the required AsyncCallback. The only
 * problem with these simple stubs is they call AsyncCallback.onSuccess() or
 * AsyncCallback.onFailure() before returning from the asynchronous call. To
 * understand why this is a problem lets quickly outline the order of events
 * that occur in a deployed Presenter.
 * 
 * Since GWT is a single threaded application all of the Presenter code will be
 * run the same UI thread. That means when an asynchronous call is made, a
 * request is pushed to another thread (lets call it Remote Thread) for
 * processing (mostly likely pushed to a queue that is processed by Remote
 * Thread). The asynchronous call returns as soon as the request is pushed, not
 * when it is executed. Even if the Request Thread processes the request
 * instantly, it must push the results back to the UI thread's queue. The UI
 * thread cannot start processing the results request until it finishes
 * processing its current request, in this case the method that called the
 * Asynchronous call in the first place. In other words, in a real GTW
 * deployment it is impossible for AsyncCallback.onSuccess() or
 * AsyncCallback.onFailure() to be called before returning from the asynchronous
 * call. That means the simple stub implementation describe above does not
 * represent the order in which things will occur in a real Deployment
 * 
 * Furthermore, if more than one asynchronous method is called from within a
 * single method, there is no guarantee which order the results will come back
 * to the UI thread queue. That means if we are going throughly test a presenter
 * with two asynchronous calls (say call A, and call B), then we must test the
 * full matrix:
 * 
 * <ul>
 * <li>original -> A.succes -> B.succes</li>
 * <li>original -> B.succes -> A.succes</li>
 * <li>original -> A.fail -> B.succes</li>
 * <li>original -> B.fail -> A.succes</li>
 * <li>original -> A.succes -> B.fail</li>
 * <li>original -> B.succes -> A.fail</li>
 * <li>original -> B.fail -> A.fail</li>
 * <li>original -> A.fail -> B.fail</li>
 * </ul>
 * 
 * This test helper provides utilities for testing a full matrix of events, such
 * as the the described above.
 * 
 * @author jmhill
 * 
 */
public class AsyncServiceRecorder<U,T> implements InvocationHandler {

	private int sequence = 0;

	private Map<Integer, MethodCall> calls = new TreeMap<Integer, MethodCall>();

	private U serviceImpl = null;
	private Class<T> asynchInterface;

	public static class MethodCall {
		public MethodCall(Method method, Object[] args) {
			super();
			this.method = method;
			this.args = args;
		}

		private Method method = null;
		private Object[] args = null;

		public Method getMethod() {
			return method;
		}

		public Object[] getArgs() {
			return args;
		}
	}

	/**
	 * A new AsyncServiceRecorder is created by providing an implementation of
	 * the the synchronous remote service. The synchronous interface is the only
	 * interface that needs to be stubbed, as the asynchronous service will be
	 * auto-generated using <code>java.lang.reflect.Proxy</code>. The recorder
	 * will use the provided synchronous service to play the appropriate
	 * onSuccess() and onFailure() calls.
	 * 
	 * @see #createAsyncProxyToRecord()
	 * 
	 * @param serviceImpl
	 * @param asynchClassInterface
	 */
	public AsyncServiceRecorder(U serviceImpl, Class<T> asynchClassInterface) {
		this.serviceImpl = serviceImpl;
		this.asynchInterface = asynchClassInterface;
	}

	/**
	 * Auto-generates a asynchronous service proxy that can be used by the
	 * Provider class you are testing. Whenever, the Provider calls any of the
	 * methods on the resulting asynchronous service proxy the method calls will
	 * be recored. This recored data is then used to play the calls on the
	 * synchronous service stub provided to the constructor.
	 * 
	 * @see #playOnSuccess(Integer)
	 * @see #playOnFailure(Integer, Throwable)
	 * 
	 * @param proxyInterface
	 *            - the interface that defines the asynchronous service to be
	 *            implemented.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public T createAsyncProxyToRecord() {
		return (T) Proxy.newProxyInstance(asynchInterface.getClassLoader(),
				new Class[] { asynchInterface }, this);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		// Keep track of each call
		calls.put(new Integer(sequence), new MethodCall(method, args));
		sequence++;
		// All Asynch services are void.
		return null;
	}
	
	/**
	 * How many calls have been recored.
	 * @return
	 */
	public int getRecoredCallCount() {
		return calls.size();
	}

	public Map<Integer, MethodCall> getAllCalls() {
		return calls;
	}
	
	public MethodCall getCall(Integer index){
		return calls.get(index);
	}

	public void clearAllCalls() {
		calls.clear();
		sequence = 0;
	}

	/**
	 * Play the given call number as an onSuccess(). The synchronous services
	 * stub provided to the constructor will be used to execute the the method.
	 * Any results will be passed to the
	 * 
	 * @param callNumber
	 * @throws Exception
	 */
	public void playOnSuccess(Integer callNumber) throws Exception {
		// Get the method to call
		MethodCall call = getcallNumber(callNumber);
		Method asynchMethod = call.getMethod();
		Object[] asynchArgs = call.getArgs();
		// We use the n-1 parameter types to find the synchronous method.
		Class<?>[] asychParamTypes = asynchMethod.getParameterTypes();

		// Now forward this call to the real service
		Class<U> synchServiceclazz = (Class<U>) serviceImpl.getClass();
		// The synchronous method will have one fewer args than the asynch.
		Class<?>[] paramsClassArray = new Class[asynchArgs.length - 1];
		Object[] paramValueArray = new Object[asynchArgs.length - 1];
		for (int i = 0; i < paramsClassArray.length; i++) {
			// Match the types of the n-1 asynch method.
			paramsClassArray[i] = asychParamTypes[i];
			// Pass all along all parameters
			paramValueArray[i] = asynchArgs[i];
		}
		// The synchrous method should match the 
		Method synchMethod = findAccessableMethod(synchServiceclazz, asynchMethod.getName(), paramsClassArray);
		// Now call the synchronous method method
		Object results = synchMethod.invoke(serviceImpl, paramValueArray);
		// Now pass the results to the callback.
		Object callback = asynchArgs[asynchArgs.length - 1];
		// Find the onSuccess method
		Class callbackClass = callback.getClass();
		Class returnType = synchMethod.getReturnType();
		// We will use the synch methods return type to determine the callback's
		// parameter type.
		Class callBackParamType = getAsyncCallbackTypeFromSynchReturnType(returnType);
		Method onSuccessMethod = findAccessableMethod(callbackClass, "onSuccess", callBackParamType);
		// Invoke the callback
		onSuccessMethod.invoke(callback, results);
	}

	private MethodCall getcallNumber(Integer callNumber) {
		MethodCall call = calls.get(callNumber);
		if (call == null) {
			throw new IllegalArgumentException(
					"No call found for call number: " + callNumber);
		}
		Object[] asynchArgs = call.getArgs();
		// Asynchronous methods must have at least one argument (the callback).
		if(asynchArgs == null || asynchArgs.length < 1){
			throw new IllegalArgumentException("Asynchronous methods must have at least one argument (the AsyncCallback)!");
		}
		return call;
	}
	
	/**
	 * Using the synchronous return type, determine the parameter type 
	 * for the asynchronous callback type.
	 * @param returnType
	 * @return
	 */
	protected static Class getAsyncCallbackTypeFromSynchReturnType(Class returnType){

		// If the return type of the synch method is a primitive
		// then we need to return the Object representation of that primitive.
		if(returnType.isPrimitive()){
			// then the parameter type of the callback is java.lang.Void.
			String name = returnType.getName();
			if("void".equals(name)){
				return Void.class;
			}else if("int".equals(name)){
				return Integer.class;
			}else if("long".equals(name)){
				return Long.class;
			}else if("boolean".equals(name)){
				return Boolean.class;
			}else if("char".equals(name)){
				return Character.class;
			}else if("byte".equals(name)){
				return Byte.class;
			}else if("double".equals(name)){
				return  Double.class;
			}else if("float".equals(name)){
				return Float.class;
			}else{
				throw new IllegalArgumentException("Unknown primitve type: "+name);
			}			
		}else{
			// For non-primitives, the synch return type is the same as the 
			return returnType; 
		}
	}
	
	/**
	 * Finds the method and ensures we can call it.
	 * @param clazz
	 * @param name
	 * @param parameterTypes
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	private Method findAccessableMethod(Class clazz, String name, Class<?>... parameterTypes) throws SecurityException, NoSuchMethodException{
		Method results = clazz.getDeclaredMethod(name, parameterTypes);
		// If it is not accessible then make it so
		if(!results.isAccessible()){
			results.setAccessible(true);
		}
		return results;
	}

	/**
	 * Play back a failure passing the given exception.
	 * @param callNumber
	 * @param caught
	 * @throws Exception
	 */
	public void playOnFailure(Integer callNumber, Throwable caught) throws Exception{
		MethodCall call = getcallNumber(callNumber);
		Object[] asynchArgs = call.getArgs();
		// Now pass the results to the callback.
		Object callback = asynchArgs[asynchArgs.length - 1];
		Class callbackClass = callback.getClass();
		Method onFailMethod = findAccessableMethod(callbackClass, "onFailure", Throwable.class);
		// Now invoke the method
		onFailMethod.invoke(callback, caught);
	}


}
