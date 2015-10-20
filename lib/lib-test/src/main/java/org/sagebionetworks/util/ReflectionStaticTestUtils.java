package org.sagebionetworks.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import com.google.common.collect.Maps;

public class ReflectionStaticTestUtils {

	public static void setStaticField(Class<?> clazz, String name, Object value) {
		Field field;
		try {
			field = clazz.getDeclaredField(name);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		ReflectionUtils.makeAccessible(field);
		ReflectionUtils.setField(field, null, value);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getTargetObject(T proxy) throws Exception {
		if (AopUtils.isJdkDynamicProxy(proxy)) {
			return (T) ((Advised) proxy).getTargetSource().getTarget();
		} else {
			return proxy;
		}
	}

	public static <T> Object getField(T proxy, String fieldName) throws Exception {
		T target = getTargetObject(proxy);
		return ReflectionTestUtils.getField(target, fieldName);
	}

	public static <T> void setField(T proxy, String fieldName, Object value) throws Exception {
		T target = getTargetObject(proxy);
		ReflectionTestUtils.setField(target, fieldName, value);
	}

	public static void mockAutowire(Object instanceOfTestClass, Object instanceOfMockedObject) throws Exception {
		Map<Class<?>, Field> mocks = Maps.newHashMap();
		for (Field field : instanceOfTestClass.getClass().getDeclaredFields()) {
			if (mocks.containsKey(field.getType())) {
				// we don't handle duplicate objects of the same class
				mocks.put(field.getType(), null);
			} else {
				mocks.put(field.getType(), field);
			}
		}

		for (Field field : instanceOfMockedObject.getClass().getDeclaredFields()) {
			if (field.getAnnotation(Autowired.class) != null) {
				Class<?> type = field.getType();
				assertTrue("No single mock field of type " + type.getName(), mocks.containsKey(type));
				Field mock = mocks.get(type);
				assertNotNull("No mock field of type " + type.getName(), mock);
				if (mock != null) {
					Object value = getField(instanceOfTestClass, mock.getName());
					setField(instanceOfMockedObject, field.getName(), value);
				}
			}
		}
	}
}
