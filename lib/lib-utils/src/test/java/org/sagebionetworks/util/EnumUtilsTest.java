package org.sagebionetworks.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class EnumUtilsTest {
	
	public enum TestEnum {
		A, B, C;
	}
	
	@Test
	public void testNamesForArray() {
		List<String> result = EnumUtils.names(TestEnum.A, TestEnum.B);
		assertEquals(ImmutableList.of("A", "B"), result);
	}
	
	@Test
	public void testNamesForEmptyArray() {
		List<String> result = EnumUtils.names();
		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void testNamesForList() {
		List<String> result = EnumUtils.names(ImmutableList.of(TestEnum.A, TestEnum.B));
		assertEquals(ImmutableList.of("A", "B"), result);
	}
	
	@Test
	public void testNamesForClass() {
		List<String> result = EnumUtils.names(TestEnum.class);
		assertEquals(ImmutableList.of("A", "B", "C"), result);
	}
	

}
