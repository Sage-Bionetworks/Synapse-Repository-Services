package org.sagebionetworks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import com.google.common.base.Predicate;

public class ThreadStepperTest {

	ThreadStepper stepper = new ThreadStepper(30000);

	@Test
	public void testOneDrivingThread() {
		final boolean[] steps = { false, false, false, false, false };

		stepper.add(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				steps[0] = true;
				stepper.stepDone("step0");
				steps[1] = true;
				Thread.sleep(30);
				stepper.stepDone("step1");
				Thread.sleep(30);
				steps[2] = true;
				stepper.stepDone("step2");
				steps[3] = true;
				Thread.sleep(30);
				stepper.stepDone("step3");
				Thread.sleep(30);
				steps[4] = true;
				stepper.stepDone("step4");
				return null;
			}
		});

		for (int i = 0; i < 5; i++) {
			stepper.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					stepper.waitForStepDone("step0");
					assertEquals(true, steps[0]);
					stepper.waitForStepDone("step1");
					assertEquals(true, steps[1]);
					stepper.waitForStepDone("step2");
					assertEquals(true, steps[2]);
					stepper.waitForStepDone("step3");
					assertEquals(true, steps[3]);
					Thread.sleep(30);
					stepper.waitForStepDone("step4");
					Thread.sleep(30);
					assertEquals(true, steps[4]);
					return null;
				}
			});
		}
		stepper.run();
	}

	@Test
	public void testLinkedThreads() {
		final int[] step = { -1 };

		stepper.add(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				assertEquals(-1, step[0]);
				step[0] = 0;
				stepper.stepDone("step0");
				stepper.waitForStepDone("step1");
				assertEquals(1, step[0]);
				step[0] = 2;
				stepper.stepDone("step2");
				stepper.waitForStepDone("step3");
				assertEquals(3, step[0]);
				step[0] = 4;
				stepper.stepDone("step4");
				stepper.waitForStepDone("step5");
				Thread.sleep(30);
				assertEquals(5, step[0]);
				step[0] = 6;
				stepper.stepDone("step6");
				stepper.waitForStepDone("step7");
				assertEquals(7, step[0]);
				step[0] = 8;
				Thread.sleep(30);
				stepper.stepDone("step8");
				stepper.waitForStepDone("step9");
				Thread.sleep(30);
				assertEquals(9, step[0]);
				step[0] = 10;
				stepper.stepDone("step10");
				stepper.waitForStepDone("step11");
				assertEquals(11, step[0]);
				step[0] = 12;
				Thread.sleep(30);
				return null;
			}
		});

		stepper.add(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				stepper.waitForStepDone("step0");
				assertEquals(0, step[0]);
				step[0] = 1;
				stepper.stepDone("step1");
				stepper.waitForStepDone("step2");
				assertEquals(2, step[0]);
				step[0] = 3;
				stepper.stepDone("step3");
				stepper.waitForStepDone("step4");
				assertEquals(4, step[0]);
				Thread.sleep(30);
				step[0] = 5;
				stepper.stepDone("step5");
				stepper.waitForStepDone("step6");
				assertEquals(6, step[0]);
				step[0] = 7;
				Thread.sleep(30);
				stepper.stepDone("step7");
				stepper.waitForStepDone("step8");
				assertEquals(8, step[0]);
				step[0] = 9;
				stepper.stepDone("step9");
				stepper.waitForStepDone("step10");
				assertEquals(10, step[0]);
				step[0] = 11;
				stepper.stepDone("step11");
				return null;
			}
		});

		stepper.run();
		assertEquals(12, step[0]);
	}
}
