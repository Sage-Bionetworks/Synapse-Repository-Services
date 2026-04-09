package org.sagebionetworks.repo.model.grid.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class IndexedEncodingUtilsTest {

	@Test
	public void testWriteNodeHeader() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		int bytesWritten = IndexedEncodingUtils.writeNodeHeader(0b000, 5L, out);
		byte[] bytes = out.toByteArray();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

		assertEquals(1, bytesWritten);
		// Type 000 in upper 4 bits (after shift), length 5 in lower 5 bits
		// 0b0000_0101 = 0x05
		assertEquals(0x05, out.toByteArray()[0] & 0xFF);

		// Verify decode
		IndexedNodeHeader typeAndLength = IndexedEncodingUtils.readNodeHeader(byteStream);
		assertEquals(0b000, typeAndLength.getNodeType());
		assertEquals(5L, typeAndLength.getLength());
	}

	@Test
	public void testWriteNodeHeaderMaxOneByte() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		int bytesWritten = IndexedEncodingUtils.writeNodeHeader(0b010, 23L, out);
		byte[] bytes = out.toByteArray();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

		assertEquals(1, bytesWritten);
		// Type 010 shifted left 5 = 0100_0000 = 0x40, length 23 = 0x17
		assertEquals(0x40 | 0x17, out.toByteArray()[0] & 0xFF);

		// Verify decode
		IndexedNodeHeader typeAndLength = IndexedEncodingUtils.readNodeHeader(byteStream);
		assertEquals(0b010, typeAndLength.getNodeType());
		assertEquals(23L, typeAndLength.getLength());
	}

	@Test
	public void testWriteNodeHeaderMinMultiByte() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		int bytesWritten = IndexedEncodingUtils.writeNodeHeader(0b010, 24L, out);
		byte[] bytes = out.toByteArray();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

		assertEquals(2, bytesWritten);
		// Type 010 shifted left 5 = 0100_0000 = 0x40, minor 24 = 0x18
		assertEquals(0x40 | 0x18, out.toByteArray()[0] & 0xFF);
		// 1-byte unsigned length follows: 24 = 0x18
		assertEquals(0x18, out.toByteArray()[1] & 0xFF);

		// Verify decode
		IndexedNodeHeader typeAndLength = IndexedEncodingUtils.readNodeHeader(byteStream);
		assertEquals(0b010, typeAndLength.getNodeType());
		assertEquals(24L, typeAndLength.getLength());
	}

	@Test
	public void testWriteNodeHeaderOneByteLengthExtension() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		int bytesWritten = IndexedEncodingUtils.writeNodeHeader(0b011, 100L, out);
		byte[] bytes = out.toByteArray();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

		assertEquals(2, bytesWritten);
		// Type 011 shifted left 5 = 0110_0000 = 0x60, minor 24 = 0x18
		assertEquals(0x60 | 0x18, bytes[0] & 0xFF);
		// 1-byte unsigned length: 100 = 0x64
		assertEquals(0x64, bytes[1] & 0xFF);

		// Verify decode
		IndexedNodeHeader typeAndLength = IndexedEncodingUtils.readNodeHeader(byteStream);
		assertEquals(0b011, typeAndLength.getNodeType());
		assertEquals(100L, typeAndLength.getLength());
	}

	@Test
	public void testWriteNodeHeaderTwoByteLengthExtension() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		int bytesWritten = IndexedEncodingUtils.writeNodeHeader(0b001, 1000L, out);
		byte[] bytes = out.toByteArray();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

		assertEquals(3, bytesWritten);
		// Type 001 shifted left 5 = 0010_0000 = 0x20, minor 25 = 0x19
		assertEquals(0x20 | 0x19, bytes[0] & 0xFF);
		// 2-byte big-endian unsigned length: 1000 = 0x03E8
		assertEquals(0x03, bytes[1] & 0xFF);
		assertEquals(0xE8, bytes[2] & 0xFF);

		// Verify decode
		IndexedNodeHeader typeAndLength = IndexedEncodingUtils.readNodeHeader(byteStream);
		assertEquals(0b001, typeAndLength.getNodeType());
		assertEquals(1000L, typeAndLength.getLength());
	}

	@Test
	public void testWriteNodeHeaderFourByteLengthExtension() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		long length = 100_000L;
		int bytesWritten = IndexedEncodingUtils.writeNodeHeader(0b100, length, out);
		byte[] bytes = out.toByteArray();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

		assertEquals(5, bytesWritten);
		// Type 100 shifted left 5 = 1000_0000 = 0x80, minor 26 = 0x1A
		assertEquals(0x80 | 0x1A, bytes[0] & 0xFF);
		// 4-byte big-endian unsigned length: 100000 = 0x000186A0
		assertEquals(0x00, bytes[1] & 0xFF);
		assertEquals(0x01, bytes[2] & 0xFF);
		assertEquals(0x86, bytes[3] & 0xFF);
		assertEquals(0xA0, bytes[4] & 0xFF);

		// Verify decode
		IndexedNodeHeader typeAndLength = IndexedEncodingUtils.readNodeHeader(byteStream);
		assertEquals(0b100, typeAndLength.getNodeType());
		assertEquals(length, typeAndLength.getLength());
	}

	@Test
	public void testWriteNodeHeaderNullOutputStream() {
		assertThrows(IllegalArgumentException.class, () -> {
			IndexedEncodingUtils.writeNodeHeader(0b000, 5L, null);
		});
	}
}
