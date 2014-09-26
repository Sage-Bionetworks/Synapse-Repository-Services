package org.sagebionetworks.util;

import java.lang.reflect.Field;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ReflectionUtils;

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
}
