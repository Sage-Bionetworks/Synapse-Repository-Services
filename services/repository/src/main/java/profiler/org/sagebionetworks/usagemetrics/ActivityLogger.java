package profiler.org.sagebionetworks.usagemetrics;

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

	private boolean shouldLogAnnotations = false;

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

	public boolean shouldLogAnnotations() {
		return shouldLogAnnotations;
	}

	public void setShouldLogAnnotations(boolean shouldLogAnnotations) {
		this.shouldLogAnnotations = shouldLogAnnotations;
	}

	public ActivityLogger() {
	}

	@Around("profiler.org.sagebionetworks.usagemetrics.SystemArchitecture.isWebService() && " +
			"execution(* org.sagebionetworks.repo.web.controller.EntityController.*(..))")
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

		String args = getArgs(declaringClass, signature, pjp.getArgs());

		//converting from nanoseconds to milliseconds
		long latencyMS = (end - start) / NANOSECOND_PER_MILLISECOND;
		
		String toLog = String.format("%dms %s.%s(%s)", 
				latencyMS, declaringClass.getSimpleName(), methodName, args);
		
		log.trace(toLog);

		return result;
	}

	/**
	 * Method for returning a coherent arg string from the relevant information.
	 * We probably want to use the org.springframework.core.LocalVariableTableParameterNameDiscoverer
	 * because this is how spring discovers parameter names.
	 * @param declaringClass the class that declared the join point
	 * @param sig method signature from the join point
	 * @param args list of actual arguments to be passed to the join point
	 * @return
	 */
	public String getArgs(Class<?> declaringClass, MethodSignature sig, Object[] args) {
		Method method = sig.getMethod();

		if (method == null) {
			return Arrays.toString(args);
		}
		String[] parameterNames = sig.getParameterNames();
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();

		String annotationSep = "";
		String argSep = "";

		StringBuilder argString = new StringBuilder();

		for (int i = 0; i < args.length; i++) {
			argString.append(argSep);
			argString.append(parameterNames[i]);

			if (shouldLogAnnotations && parameterAnnotations[i].length > 0) {
				argString.append("{");
				for (Annotation annotation : parameterAnnotations[i]) {
					argString.append(annotationSep);
					argString.append(annotation.toString());
					annotation.annotationType();

					annotationSep = ";";
				}
				argString.append("}");
			}
			argString.append("=");

			argString.append(args[i]);
			// Reset for next iteration
			annotationSep = "";
			argSep = ",";
		}

		return argString.toString();
	}

}
