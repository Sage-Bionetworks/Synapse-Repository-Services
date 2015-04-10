package org.sagebionetworks;

import java.lang.reflect.Method;
import java.util.Set;

import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TransactionExceptionTest {

	@Test
	public void checkForCheckedExceptionsOverWriteTransactions() {
		Reflections reflections = new Reflections("org.sagebionetworks", new MethodAnnotationsScanner());

		Set<Method> transactionMethods = reflections.getMethodsAnnotatedWith(Transactional.class);
		System.out.println("Found " + transactionMethods.size() + " transactional methods");
		Set<Class<?>> exceptionsThrown = Sets.newHashSet();
		for (Method transactionMethod : transactionMethods) {
			if (transactionMethod.getAnnotation(Transactional.class).readOnly()) {
				continue;
			}
			for (Class<?> exceptionClass : transactionMethod.getExceptionTypes()) {
				exceptionsThrown.add(exceptionClass);
			}
		}
		System.out.println(exceptionsThrown);
	}
}
