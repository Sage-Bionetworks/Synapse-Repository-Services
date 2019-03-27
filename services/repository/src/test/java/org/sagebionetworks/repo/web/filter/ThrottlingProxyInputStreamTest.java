package org.sagebionetworks.repo.web.filter;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.ServletInputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ThrottlingProxyInputStreamTest {
	
	@Mock
	ServletInputStream mockInputStream;
	
	ThrottlingProxyInputStream proxy;
	
	long maximumInputStreamBytes = 10;
	
	@Before
	public void before() {
		proxy = new ThrottlingProxyInputStream(mockInputStream, maximumInputStreamBytes);
	}
	
	@Test
	public void testClose() throws IOException {
		proxy.close();
		verify(mockInputStream).close();
	}
	
	@Test
	public void testMarkSupported() {
		proxy.markSupported();
		verify(mockInputStream).markSupported();
	}
	
	@Test
	public void testReset() throws IOException {
		proxy.reset();
		verify(mockInputStream).reset();
	}
	
	@Test
	public void testMark() throws IOException {
		proxy.mark(1);
		verify(mockInputStream).mark(1);
	}
	
	@Test
	public void testAvaiable() throws IOException {
		proxy.available();
		verify(mockInputStream).available();
	}
	
	@Test
	public void testSkip() throws IOException {
		proxy.skip(1);
		verify(mockInputStream).skip(1);
	}
	
	@Test
	public void testReadOne() throws IOException {
		byte[] b = new byte[] {1};
		int off = 1;
		int len = 2;
		proxy.read(b, off, len);
		verify(mockInputStream).read(b, off, len);
	}
	
	@Test (expected=ByteLimitExceededException.class)
	public void testReadOneOverLimit() throws IOException {
		byte[] b = new byte[] {1};
		int off = 1;
		int len = 2;
		when(mockInputStream.read(b, off, len)).thenReturn((int)maximumInputStreamBytes+1);
		proxy.read(b, off, len);
		verify(mockInputStream).read(b, off, len);
	}
	
	@Test
	public void testReadTwo() throws IOException {
		byte[] b = new byte[] {1};
		proxy.read(b);
		verify(mockInputStream).read(b);
	}
	
	@Test (expected=ByteLimitExceededException.class)
	public void testReadTwoOverLimit() throws IOException {
		byte[] b = new byte[] {1};
		when(mockInputStream.read(b)).thenReturn((int)maximumInputStreamBytes+1);
		proxy.read(b);
		verify(mockInputStream).read(b);
	}
	
	@Test
	public void testReadLine() throws IOException {
		byte[] b = new byte[] {1};
		int off = 1;
		int len = 2;
		proxy.readLine(b, off, len);
		verify(mockInputStream).readLine(b, off, len);
	}
	
	@Test (expected=ByteLimitExceededException.class)
	public void testReadLineOverLimit() throws IOException {
		byte[] b = new byte[] {1};
		int off = 1;
		int len = 2;
		when(mockInputStream.readLine(b, off, len)).thenReturn((int)maximumInputStreamBytes+1);
		proxy.readLine(b, off, len);
		verify(mockInputStream).readLine(b, off, len);
	}
	
	@Test
	public void testRead() throws IOException {
		// this read returns an actual byte, and not the count of read bytes.
		when(mockInputStream.read()).thenReturn(0,100,101,102,103,104,105,106,107,108,-1);
		int read;
		while((read = proxy.read())>-1) {
			System.out.println(read);
		}
		verify(mockInputStream, times(11)).read();
	}
	
	@Test (expected=ByteLimitExceededException.class)
	public void testReadOverLimit() throws IOException {
		// this read returns an actual byte, and not the count of read bytes.
		when(mockInputStream.read()).thenReturn(0,100,101,102,103,104,105,106,107,108,109,110,-1);
		int read;
		while((read = proxy.read())>-1) {
			System.out.println(read);
		}
		verify(mockInputStream).read();
	}

}
