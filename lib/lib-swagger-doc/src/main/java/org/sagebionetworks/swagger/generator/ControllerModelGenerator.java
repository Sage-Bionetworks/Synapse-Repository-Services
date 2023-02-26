package org.sagebionetworks.swagger.generator;
import java.lang.reflect.Field;


import java.lang.reflect.Method;

import java.util.Arrays;
import java.io.IOException;

import java.lang.reflect.Parameter;
import java.lang.annotation.Annotation;

import jdk.javadoc.doclet.*;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;

/**
 * This generator creates a mapping between each controller to the methods it has as well
 * as the attributes in each of these methods.
 * 
 * @author lli
 *
 */
public class ControllerModelGenerator {
	
	// This method is used for reading spring annotation from a controller using
	// Java Reflection. Will be used when generating the ControllerModel.
	private static void readSpringAnnotations(Class clazz) throws Exception {
		Method[] methods = clazz.getMethods();
		String fullClassName = clazz.getName();
		for (Method method : methods) {
			// A class contains more than just the methods that we define since
			// it will inherit things like the ".equals" method. We only want to 
			// examine the methods that we have added
			if (!method.toString().contains(fullClassName)) continue;

			System.out.println("[METHOD NAME]: " + method.getName());
			System.out.println("[RETURN TYPE]: " + method.getReturnType());
			for (Parameter param : method.getParameters()) {
				System.out.println("[PARAMETER] " + param.getName() + " [TYPE] " + param.getType());
			}

			System.out.println("[METHOD ANNOTATIONS]");
			parseMethodAnnotations(method);

			// We need to properly see if this is a interface or concrete implementation (look at springMVC example).
			// If it is an interface, then we need to find all instances of its concrete implementation
			// and add a "oneOf" reference into the returned openAPI specification
			System.out.println();
			Class<?> returnClass = method.getReturnType();
			System.out.println("[METHOD RETURN TYPE]: " + returnClass.getSimpleName());
			
			System.out.println();
			System.out.println();
		}
	}
	
	// Given a method, extract values from the Spring annotations in method header.
	public static void parseMethodAnnotations(Method method) throws Exception {
		for (Annotation a : method.getAnnotations()) {
			String annotation = a.toString();
			int firstParenIndex = annotation.indexOf("(");
			int lastParenIndex = annotation.lastIndexOf(")");
			if (firstParenIndex < 0 || lastParenIndex < 0) {
				throw new IllegalArgumentException("Malformed annotation.");
			}
			
			System.out.println("For annotation " + a.annotationType().getName());
			String annotationParameters = annotation.substring(firstParenIndex + 1, lastParenIndex);
			if (annotationParameters.equals("")) {
				System.out.println("No parameters");
				return;
			}
			String[] parameters = annotationParameters.split(",");
			
			for (String parameter : parameters) {
				if (parameter.equals("")) continue;
				
				parameter = parameter.strip();
				String[] paramData = parameter.split("=");
				if (paramData.length != 2) throw new IllegalArgumentException("Malformed parameter " + parameter + "!");
				
				String paramName = paramData[0];
				String paramValue = paramData[1];
				System.out.println("[NAME]: " + paramName + " [VALUE]: " + paramValue);
			}
			System.out.println();
			System.out.println();
		}
	}

	
	// Reading Javadoc using Java Doclet code will go here.
	public static boolean run(DocletEnvironment environment) throws Exception {
		return true;
	}
}
