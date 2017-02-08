package org.sagebionetworks.repo.model.exception;

import java.util.Stack;

public class ExceptionThreadLocal {
	private static ThreadLocal<Stack<Throwable>> exceptionThreadLocal = new ThreadLocal<Stack<Throwable>>();

	public static void push(Throwable throwable) {
		Stack<Throwable> stack = exceptionThreadLocal.get();
		if (stack == null) {
			stack = new Stack<Throwable>();
			exceptionThreadLocal.set(stack);
		}
		stack.push(throwable);
	}

	public static Throwable pop(Class ignoreType) {
		Stack<Throwable> stack = exceptionThreadLocal.get();
		if (stack == null) {
			return null;
		}
		while (!stack.isEmpty()) {
			Throwable throwable = stack.pop();
			if (!ignoreType.isAssignableFrom(throwable.getClass())) {
				stack.clear();
				return throwable;
			}
		}
		return null;
	}
}
