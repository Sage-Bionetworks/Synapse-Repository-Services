package org.sagebionetworks.file.controller;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;

public class LogUtils {

	/**
	 * Log all of the information about this request.
	 * 
	 * @param log
	 * @param request
	 */
	public static void logRequest(Log log, HttpServletRequest request){
		if(log.isDebugEnabled()){
			// Log all public strings.
			Class clazz = request.getClass();
			Method[] methods = clazz.getMethods();
			for(Method method: methods){
				if(method.getName().startsWith("get")){
					if(Modifier.isPublic(method.getModifiers())){
						Class returnType = method.getReturnType();
//						log.debug("return type for "+method.getName()+" = "+returnType);
						if(returnType == String.class || returnType == int.class || returnType == long.class || returnType == double.class){
							Class[] params = method.getParameterTypes();
							if(params.length == 0){
								try {
									Object value = method.invoke(request, null);
									log.debug(method.getName()+" = "+value);
								} catch (Exception e) {
									throw new RuntimeException(e);
								}
							}
						}
					}
				}
			}
			// Parameters
			log.debug("Parameters:");
			for(Object keyObj: request.getParameterMap().keySet()){
				String key = (String)keyObj;
				String[] value = (String[]) request.getParameterMap().get(key);
				if(value != null){
					StringBuilder builder = new StringBuilder();
					builder.append("\t").append(key).append(" = ");
					for(int i=0; i<value.length; i++){
						if(i > 0){
							builder.append(", ");
						}
						builder.append(value[i]);
					}
					log.debug(builder.toString());
				}
			}
			
			// headers
			log.debug("Headers:");
			Enumeration headers = request.getHeaderNames();
			while(headers.hasMoreElements()){
				String key = (String) headers.nextElement();
				String value = request.getHeader(key);
				log.debug("\t "+key+" = "+value);
			}
		}

	}
}
