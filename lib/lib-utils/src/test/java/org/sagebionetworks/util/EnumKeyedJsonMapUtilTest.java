package org.sagebionetworks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class EnumKeyedJsonMapUtilTest {

	private enum TestEnum {
		ALPHA,
		BETA,
		GAMMA
	}

	@Test
	public void testConvertToEnum() {
		Map<String, String> stringMap = new HashMap<>();
		stringMap.put("ALPHA", "alphaVal");
		stringMap.put("BETA", "betaVal");
		stringMap.put("GAMMA", "gammaVal");

		// Call under test
		Map<TestEnum, String> enumMap = EnumKeyedJsonMapUtil.convertKeysToEnums(stringMap, TestEnum.class);

		assertEquals(3, enumMap.size());
		assertEquals(stringMap.get("ALPHA"), enumMap.get(TestEnum.ALPHA));
		assertEquals(stringMap.get("BETA"), enumMap.get(TestEnum.BETA));
		assertEquals(stringMap.get("GAMMA"), enumMap.get(TestEnum.GAMMA));
	}

	@Test
	public void testConvertToEnum_unrecognizedKey() {
		Map<String, String> stringMap = new HashMap<>();
		stringMap.put("ALPHA", "alphaVal");
		stringMap.put("BETA", "betaVal");
		stringMap.put("EPSILON", "epsilonVal"); // not in the enum!

		// Call under test
		Map<TestEnum, String> enumMap = EnumKeyedJsonMapUtil.convertKeysToEnums(stringMap, TestEnum.class);

		assertEquals(2, enumMap.size());
		assertEquals(stringMap.get("ALPHA"), enumMap.get(TestEnum.ALPHA));
		assertEquals(stringMap.get("BETA"), enumMap.get(TestEnum.BETA));
		assertNull(enumMap.get(TestEnum.GAMMA));

	}

	@Test
	public void testConvertToString() {
		Map<TestEnum, String> enumMap = new HashMap<>();
		enumMap.put(TestEnum.ALPHA, "alphaVal");
		enumMap.put(TestEnum.BETA, "betaVal");
		enumMap.put(TestEnum.GAMMA, "gammaVal");

		// Call under test
		Map<String, String> stringMap = EnumKeyedJsonMapUtil.convertKeysToStrings(enumMap);

		assertEquals(3, stringMap.size());
		assertEquals(enumMap.get(TestEnum.ALPHA), stringMap.get("ALPHA"));
		assertEquals(enumMap.get(TestEnum.BETA), stringMap.get("BETA"));
		assertEquals(enumMap.get(TestEnum.GAMMA), stringMap.get("GAMMA"));
	}
}
