package profiler.org.sagebionetworks.usagemetrics;

import static java.net.URLEncoder.encode;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class ActivityLogger {
	/**
	 * constant for nanosecond conversion to milliseconds
	 */
	private static final long NANOSECOND_PER_MILLISECOND = 1000000L;

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
		String args;

		try {
			args = getArgs(declaringClass, signature, pjp.getArgs());
		} catch (UnsupportedEncodingException e) {
			log.error("Could not properly encode arguments", e);
			args = Arrays.toString(pjp.getArgs());
		}


		//converting from nanoseconds to milliseconds
		long latencyMS = (end - start) / NANOSECOND_PER_MILLISECOND;

		String toLog = String.format("%s/%s?latency=%d&%s",
				declaringClass.getSimpleName(), methodName, latencyMS, args);

		log.trace(toLog);

		return result;
	}

	/**
	 * Method for returning a coherent arg string from the relevant information.
	 * @param declaringClass the class that declared the join point
	 * @param sig method signature from the join point
	 * @param args list of actual arguments to be passed to the join point
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public String getArgs(Class<?> declaringClass, MethodSignature sig, Object[] args) throws UnsupportedEncodingException {
		Method method = sig.getMethod();

		if (method == null) {
			return Arrays.toString(args);
		}
		String[] parameterNames = sig.getParameterNames();

		String encoding = "UTF-8";

		String argSep = "";

		StringBuilder argString = new StringBuilder();

		for (int i = 0; i < args.length; i++) {
			argString.append(argSep);
			argString.append(parameterNames[i]);

			argString.append("=");
			if (args[i] != null)
				argString.append(encode(args[i].toString(), encoding));
			// Reset for next iteration
			argSep = "&";
		}

		return argString.toString();
	}

}
