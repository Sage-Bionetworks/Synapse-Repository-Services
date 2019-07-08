package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class BeanTest implements ApplicationContextAware {

	ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	private static final List<String> EXCEPTIONS = Lists.newArrayList(
			"org.springframework.transaction.annotation.AnnotationTransactionAttributeSource",
			"org.springframework.transaction.interceptor.TransactionInterceptor",
			"org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor");
	private static final Pattern UNNAMED_BEAN_PATTERN = Pattern.compile("^(.*)#[0-9]+$");

	@Test
	public void testNoUnnamedBeans() {
		List<String> foundBeans = Lists.newLinkedList();
		for (String beanName : applicationContext.getBeanDefinitionNames()) {
			Matcher matcher = UNNAMED_BEAN_PATTERN.matcher(beanName);
			if (matcher.matches() && !EXCEPTIONS.contains(matcher.group(1))) {
				foundBeans.add(beanName);
			}
		}
		assertEquals(
				"Found beans without name/id. Either give the bean a name/id or add to exceptions in the test, otherwise Spring will not guarantee that the bean is a singleton",
				"",
				StringUtils.join(foundBeans, ","));
	}

	@Test
	public void testTransactionalNotUsed() {
		// Transactional is not used anymore, use @WriteTransaction, @NewWriteTransaction or @MandatoryWriteTransaction
		Reflections reflections = new Reflections("org.sagebionetworks", new MethodAnnotationsScanner(), new TypeAnnotationsScanner());
		assertEquals(0, reflections.getTypesAnnotatedWith(Transactional.class).size());
		assertEquals(0, reflections.getMethodsAnnotatedWith(Transactional.class).size());
	}

	private static final List<String> readMethodPrefixes = Lists.newArrayList("check", "get");
	private static final List<String> exceptions = Lists.newArrayList(
			"checkSessionToken",
			"getSessionToken",
			"getEtagForUpdate",
			"getForumByProjectId",
			"getForUpdate",
			"getAccessRequirementForUpdate",
			"getThread",
			"checkPasswordWithThrottling",
			"getTableStatusOrCreateIfNotExists",
			"getUsersDownloadListForUpdate",
			"getDoiAssociationForUpdate",
			"getUnsuccessfulLoginLockoutInfoIfExist",
			"checkIsLockedOut",
			"getTableIdWithLock");

	@Test
	public void testNoGetterWriteTransactions() {
		Reflections reflections = new Reflections("org.sagebionetworks", new MethodAnnotationsScanner());
		Set<Method> writeMethods = reflections.getMethodsAnnotatedWith(WriteTransaction.class);
		writeMethods.addAll(reflections.getMethodsAnnotatedWith(NewWriteTransaction.class));
		writeMethods.addAll(reflections.getMethodsAnnotatedWith(MandatoryWriteTransaction.class));
		Set<String> prefixes = Sets.newHashSet();
		for (Method method : writeMethods) {
			String prefix = method.getName().replaceAll("[A-Z].*$", "");
			if (readMethodPrefixes.contains(prefix)) {
				if (!exceptions.contains(method.getName())) {
					fail("Possible read only method that has write transaction: " + method);
				}
			} else {
				prefixes.add(prefix);
			}
		}
		System.out.println("method prefixes for modifying methods: " + prefixes);
	}
}
