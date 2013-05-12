package org.sagebionetworks.profiler.usagemetrics;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.sagebionetworks.logging.SynapseLoggingUtils;

@Aspect
public class ActivityLogger {
	/**
	 * constant for nanosecond conversion to milliseconds
	 */
	private static final long NANOSECOND_PER_MILLISECOND = 1000000L;

	private static final List<String> HEADERS_TO_LOG = Arrays.asList("user-agent", "sessiontoken");

	private static Log log = LogFactory.getLog(ActivityLogger.class);

	private boolean shouldProfile;

	public static Log getLog() {
		return log;
	}

	public static void setLog(Log log) {
		ActivityLogger.log = log;
	}

	public boolean shouldProfile() {
		return shouldProfile;
	}

	public void setShouldProfile(boolean shouldProfile) {
		this.shouldProfile = shouldProfile;
	}

	public ActivityLogger() {
	}

	/**
	 * A method to do basic logging of all calls to the REST service.
	 * It logs like this: ClassName/MethodName?latency=<time-in-ms>[&argName=argValue]...
	 * argValue is just the result of calling toString on the argument object.
	 * All argValue's are {@link java.net.URLEncoder#encode(String, String) URLEncoded}.
	 * @param pjp
	 * @return
	 * @throws Throwable
	 */
	@Around("@within(org.springframework.stereotype.Controller) &&" +
			"@annotation(org.springframework.web.bind.annotation.ResponseStatus) &&" +
			"execution(* org.sagebionetworks.repo.web.controller.*.*(..))")
	public Object doBasicLogging(ProceedingJoinPoint pjp) throws Throwable {
		if (!this.shouldProfile){
			//if turned off, just proceed with method
			return pjp.proceed();
		}

		long start = System.nanoTime();
		Object result = pjp.proceed();
		long end = System.nanoTime();

		Signature sig = pjp.getSignature();
		MethodSignature signature = null;

		if (sig instanceof MethodSignature) {
			signature = (MethodSignature) pjp.getSignature();
		} else {
			log.error("Signature of joinpoint was of wrong type.  Expected MethodSignature, got "+sig.getClass().getSimpleName());
			return result;
		}

		String methodName = signature.getName();

		Class<?> declaringClass = signature.getDeclaringType();

		List<Object> args = Arrays.asList(pjp.getArgs());
		List<String> argNames = Arrays.asList(signature.getParameterNames());

		Map<String, String> properties = new LinkedHashMap<String, String>();

		Iterator<Object> argItr = args.iterator();
		Iterator<String> argNameItr = argNames.iterator();
		Object arg = null;
		String argName = null;

		for (; argItr.hasNext() && argNameItr.hasNext(); arg = argItr.next(), argName = argNameItr.next()) {
			if (arg instanceof HttpServletRequest) {
				args.remove(argItr);
				addHeaders(properties, (HttpServletRequest) arg);
			} else {
				String argString = arg != null ? arg.toString()
						: "null";
				String argNameString = argName != null ? argName
						: (arg != null ? arg.getClass().toString() : "none");

				properties.put(argNameString, argString);
			}
		}

		String argString = SynapseLoggingUtils.makeArgString(properties);

		//converting from nanoseconds to milliseconds
		long latencyMS = (end - start) / NANOSECOND_PER_MILLISECOND;

		log.trace(SynapseLoggingUtils.makeLogString(declaringClass.getSimpleName(), methodName, latencyMS, argString));

		return result;
	}

	private void addHeaders(Map<String, String> properties, HttpServletRequest request) {
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String header = headerNames.nextElement();
			if (HEADERS_TO_LOG.contains(header)) {
				properties.put(header, request.getHeader(header));
			}
		}
	}

}
