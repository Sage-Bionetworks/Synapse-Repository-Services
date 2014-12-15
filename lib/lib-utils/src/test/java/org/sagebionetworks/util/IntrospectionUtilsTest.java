package org.sagebionetworks.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IntrospectionUtilsTest {

	public interface Arg {
	}

	public interface Other {
	}

	public class Arg1 implements Arg {
	}

	public class Arg2 extends Arg1 implements Other {
	}

	public class Arg3 extends Arg2 {
	}

	public class Arg4 implements Arg {
	}

	public class Arg5 extends Arg4 {
	}

	public abstract class Call {
		public String call(Arg arg) {
			return "CallBase";
		}
	}

	public class Call1 extends Call {
		@Override
		public String call(Arg arg) {
			return "Call1";
		}

		public String call(Arg1 arg) {
			return "Call1-1";
		}
	}

	public class Call2 extends Call {
		@Override
		public String call(Arg arg) {
			return "Call2";
		}

		public String call(Arg1 arg) {
			return "Call2-1";
		}

		public String call(Arg2 arg) {
			return "Call2-2";
		}
	}

	@Test
	public void testSuccess() throws Exception {
		Call call1 = new Call1();
		Call call2 = new Call2();
		Arg arg1 = new Arg1();
		Arg arg2 = new Arg2();
		Arg arg3 = new Arg3();
		Arg arg4 = new Arg4();
		Arg arg5 = new Arg5();

		// not cached
		assertEquals("Call1-1", IntrospectionUtils.findNearestMethod(call1, "call", arg1).invoke(call1, arg1));
		// cached
		assertEquals("Call1-1", IntrospectionUtils.findNearestMethod(call1, "call", arg1).invoke(call1, arg1));
		assertEquals("Call1-1", IntrospectionUtils.findNearestMethod(call1, "call", arg2).invoke(call1, arg2));
		assertEquals("Call1-1", IntrospectionUtils.findNearestMethod(call1, "call", arg3).invoke(call1, arg3));
		assertEquals("Call1", IntrospectionUtils.findNearestMethod(call1, "call", arg4).invoke(call1, arg4));
		assertEquals("Call1", IntrospectionUtils.findNearestMethod(call1, "call", arg5).invoke(call1, arg5));
		assertEquals("Call2-1", IntrospectionUtils.findNearestMethod(call2, "call", arg1).invoke(call2, arg1));
		assertEquals("Call2-2", IntrospectionUtils.findNearestMethod(call2, "call", arg2).invoke(call2, arg2));
		assertEquals("Call2-2", IntrospectionUtils.findNearestMethod(call2, "call", arg3).invoke(call2, arg3));
		assertEquals("Call2", IntrospectionUtils.findNearestMethod(call2, "call", arg4).invoke(call2, arg4));
		assertEquals("Call2", IntrospectionUtils.findNearestMethod(call2, "call", arg5).invoke(call2, arg5));
	}
}
