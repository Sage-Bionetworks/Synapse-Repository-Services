package org.sagebionetworks.util;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Lists;

public class IntrospectionUtils {

	private static Map<String, Method[]> methodMap = new ConcurrentHashMap<String, Method[]>();

	public static Method findNearestMethod(Object obj, String methodName, Object arg) {
		Class<?> argClass = arg.getClass();
		Class<?> objClass = obj.getClass();

		String key = objClass.getName() + ':' + methodName;
		Method[] methodsMatchingName = methodMap.get(key);
		if (methodsMatchingName == null) {
			List<Method> matchingMethods = Lists.newArrayListWithCapacity(20);
			Method[] methods = objClass.getMethods();
			for (Method method : methods) {
				if (method.getName().equals(methodName)) {
					matchingMethods.add(method);
				}
			}
			methodsMatchingName = matchingMethods.toArray(new Method[matchingMethods.size()]);
			methodMap.put(key, methodsMatchingName);
		}

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

	private static Method findMatchingMethod(Class<?> argClassToFind, Method[] methods) {
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
