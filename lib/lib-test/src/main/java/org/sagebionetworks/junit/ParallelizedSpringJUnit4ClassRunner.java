package org.sagebionetworks.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.model.Statement;
import org.springframework.test.context.ParallelizedDefaultTestContext;
import org.springframework.test.context.ParallelizedTestContextManager;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Maps;

public class ParallelizedSpringJUnit4ClassRunner extends SpringJUnit4ClassRunner {

	private static class ParallelScheduler implements RunnerScheduler {
		private final boolean isThreaded;
		private final ExecutorService executor;

		public ParallelScheduler() {
			isThreaded = "true".equals(System.getProperty("test.threaded"));
			executor = isThreaded ? Executors.newFixedThreadPool(10) : null;
		}

		public void schedule(Runnable childStatement) {
			if (isThreaded) {
				executor.submit(childStatement);
			} else {
				childStatement.run();
			}
		}

		public void finished() {
			if (isThreaded) {
				executor.shutdown();
				try {
					executor.awaitTermination(20, TimeUnit.MINUTES);
				} catch (InterruptedException exc) {
					throw new RuntimeException(exc);
				}
			}
		}
	}

	/**
	 * Instantiates a new parallelized.
	 * 
	 * @param klass the klass
	 * @throws Throwable the throwable
	 */
	public ParallelizedSpringJUnit4ClassRunner(Class<?> klass) throws Throwable {
		super(klass);
		setScheduler(new ParallelScheduler());
	}

	@Override
	protected TestContextManager createTestContextManager(Class<?> clazz) {
		ParallelizedTestContextManager testContextManager = new ParallelizedTestContextManager(clazz, getDefaultContextLoaderClassName(clazz));
		testContextManager.registerTestExecutionListeners(new TestExecutionListener() {

			@Override
			public void beforeTestClass(TestContext testContext) throws Exception {
			}

			Map<TestContext, Boolean> hasBeenInitialized = Maps.newConcurrentMap();

			@Override
			public void prepareTestInstance(TestContext testContext) throws Exception {
				// this is injected after autowire, but before any test methods are called
				// run all @BeforeAll methods
				synchronized (testContext) {
					if (hasBeenInitialized.put(testContext, true) == null) {
						((ParallelizedDefaultTestContext) testContext).setTestInstanceForAfterAll(testContext.getTestInstance());
						final List<FrameworkMethod> beforeAllmethods = getTestClass().getAnnotatedMethods(BeforeAll.class);
						for (FrameworkMethod each : beforeAllmethods) {
							try {
								each.invokeExplosively(testContext.getTestInstance());
							} catch (Throwable e) {
								throw new Exception(e.getMessage(), e);
							}
						}
					}
				}
			}

			@Override
			public void beforeTestMethod(TestContext testContext) throws Exception {
			}

			@Override
			public void afterTestMethod(TestContext testContext) throws Exception {
			}

			@Override
			public void afterTestClass(TestContext testContext) throws Exception {
			}
		});
		return testContextManager;
	}

	private void runAfterAlls() throws Throwable {
		final List<FrameworkMethod> afterAllmethods = getTestClass().getAnnotatedMethods(AfterAll.class);
		Object testInstance = ((ParallelizedDefaultTestContext) ((ParallelizedTestContextManager) getTestContextManager()).getMyTestContext())
				.getTestInstanceForAfterAll();
		if (testInstance != null) {
			for (FrameworkMethod each : afterAllmethods) {
				each.invokeExplosively(testInstance);
			}
		}
	}

	@Override
	protected Statement withAfterClasses(final Statement statement) {
		Statement withAfterAll = new Statement() {
			@Override
			public void evaluate() throws Throwable {
				List<Throwable> errors = new ArrayList<Throwable>();
				try {
					statement.evaluate();
				} catch (Throwable e) {
					errors.add(e);
				}

				try {
					runAfterAlls();
				} catch (Exception e) {
					errors.add(e);
				}

				if (errors.isEmpty()) {
					return;
				}
				if (errors.size() == 1) {
					throw errors.get(0);
				}
				throw new org.junit.internal.runners.model.MultipleFailureException(errors);
			}
		};
		return super.withAfterClasses(withAfterAll);
	}
}
