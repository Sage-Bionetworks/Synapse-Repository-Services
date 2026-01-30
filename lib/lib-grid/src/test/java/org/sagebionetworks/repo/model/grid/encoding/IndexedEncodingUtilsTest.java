package org.sagebionetworks.repo.model.grid.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class IndexedEncodingUtilsTest {

	@Test
	public void testWriteNodeTypeAndLength() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		int bytesWritten = IndexedEncodingUtils.writeNodeTypeAndLength(0b000, 5L, out);
		byte[] bytes = out.toByteArray();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

		assertEquals(1, bytesWritten);
		// Type 000 in upper 4 bits (after shift), length 5 in lower 5 bits
		// 0b0000_0101 = 0x05
		assertEquals(0x05, out.toByteArray()[0] & 0xFF);

		// Verify decode
		IndexedNodeHeader typeAndLength = IndexedEncodingUtils.readNodeTypeAndLength(byteStream);
		assertEquals(0b000, typeAndLength.getNodeType());
		assertEquals(5L, typeAndLength.getLength());
	}

	@Test
	public void testWriteNodeTypeAndLengthMaxOneByte() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		int bytesWritten = IndexedEncodingUtils.writeNodeTypeAndLength(0b010, 30L, out);
		byte[] bytes = out.toByteArray();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

		assertEquals(1, bytesWritten);
		// Type 010 shifted left 5 = 0100_0000 = 0x40, length 30 = 0x1E
		assertEquals(0x40 | 0x1E, out.toByteArray()[0] & 0xFF);

		// Verify decode
		IndexedNodeHeader typeAndLength = IndexedEncodingUtils.readNodeTypeAndLength(byteStream);
		assertEquals(0b010, typeAndLength.getNodeType());
		assertEquals(30L, typeAndLength.getLength());
	}

	@Test
	public void testWriteNodeTypeAndLengthLargeLength() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		int bytesWritten = IndexedEncodingUtils.writeNodeTypeAndLength(0b011, 100L, out);
		byte[] bytes = out.toByteArray();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

		// First byte: type in bits 7-5, 0x1F in bits 4-0 (length extension indicator)
		// Type 011 shifted left 5 = 0110_0000 = 0x60, extension = 0x1F
		assertEquals(0x60 | 0x1F, out.toByteArray()[0] & 0xFF);

		// Remaining bytes should be vu57 encoding of 100
		byte[] expectedVu57 = Vu57Utils.encodeVu57(100L);
		assertEquals(1 + expectedVu57.length, bytesWritten);

		// Verify decode
		IndexedNodeHeader typeAndLength = IndexedEncodingUtils.readNodeTypeAndLength(byteStream);
		assertEquals(0b011, typeAndLength.getNodeType());
		assertEquals(100L, typeAndLength.getLength());
	}

	@Test
	public void testWriteNodeTypeAndLengthNullOutputStream() {
		assertThrows(IllegalArgumentException.class, () -> {
			IndexedEncodingUtils.writeNodeTypeAndLength(0b000, 5L, null);
		});
	}
}
