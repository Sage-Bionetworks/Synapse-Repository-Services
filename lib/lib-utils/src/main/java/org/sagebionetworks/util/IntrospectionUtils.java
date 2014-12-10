package org.sagebionetworks.util;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Lists;

/**
 * All static methods to find the nearest match given a method name and an argument. There are caches, and we expect a
 * limited set of method/arguments to be used. If this becomes used by more different pieces of code, we might have to
 * make this a non-static class, so each separate piece of code can have its own cache
 * 
 */
public class IntrospectionUtils {

	private static Map<String, Method> matchingMethodMap = new ConcurrentHashMap<String, Method>();

	private static String createClassMethodKey(Class<?> objClass, String methodName, Class<?> argClass) {
		return objClass.getName() + ':' + methodName + ':' + argClass.getName();
	}

	public static Method findNearestMethod(Object obj, String methodName, Object arg) {
		Class<?> argClass = arg.getClass();
		Class<?> objClass = obj.getClass();

		String key = createClassMethodKey(objClass, methodName, argClass);
		Method matchingMethod = matchingMethodMap.get(key);
		if (matchingMethod != null) {
			// found it!
			return matchingMethod;
		}
		List<Method> methodsMatchingName = Lists.newArrayListWithCapacity(20);
		Method[] methods = objClass.getMethods();
		for (Method method : methods) {
			if (method.getName().equals(methodName)) {
				methodsMatchingName.add(method);
			}
		}

		Method m = findNearestMatchingMethod(methodName, argClass, methodsMatchingName);
		matchingMethodMap.put(key, m);
		return m;
	}

	private static Method findNearestMatchingMethod(String methodName, Class<?> argClass, List<Method> methodsMatchingName) {
		for (Class<?> argClassToFind = argClass; argClassToFind != Object.class; argClassToFind = argClassToFind.getSuperclass()) {
			Method m = findMatchingMethod(argClassToFind, methodsMatchingName);
			if (m != null) {
				return m;
			}
			for (Class<?> clazz : argClassToFind.getDeclaredClasses()) {
				m = findMatchingMethod(clazz, methodsMatchingName);
				if (m != null) {
					return m;
				}
			}
			for (Class<?> clazz : argClassToFind.getInterfaces()) {
				m = findMatchingMethod(clazz, methodsMatchingName);
				if (m != null) {
					return m;
				}
			}
		}
		throw new IllegalArgumentException("No method " + methodName + " found for arg of type " + argClass.getName());
	}

	private static Method findMatchingMethod(Class<?> argClassToFind, List<Method> methods) {
		for (Method m : methods) {
			Class<?>[] parameterTypes = m.getParameterTypes();
			if (parameterTypes.length != 1) {
				continue;
			}
			if (parameterTypes[0] == argClassToFind) {
				return m;
			}
		}
		return null;
	}
}
