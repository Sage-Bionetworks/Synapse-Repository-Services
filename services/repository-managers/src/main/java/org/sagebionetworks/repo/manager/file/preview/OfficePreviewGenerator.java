package org.sagebionetworks.repo.manager.file.preview;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.ProcessBuilder.Redirect;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.util.Closer;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.util.StreamUtils;

import com.google.common.collect.ImmutableSet;
import com.sun.star.beans.PropertyValue;
import com.sun.star.bridge.UnoUrlResolver;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.document.XTypeDetection;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XStorable;
import com.sun.star.io.BufferSizeExceededException;
import com.sun.star.io.NotConnectedException;
import com.sun.star.io.XInputStream;
import com.sun.star.io.XSeekable;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lib.uno.adapter.OutputStreamToXOutputStreamAdapter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseable;

/**
 * Generates previews for image content types.
 * 
 * @author John
 * 
 */
public class OfficePreviewGenerator implements PreviewGenerator {

	static final String[] OPENOFFICE_PATHS = { "/usr/bin/soffice", "C:/Program Files (x86)/OpenOffice 4/program/soffice.exe" };

	private static final String LOCAL_CONNECT_STRING = "socket,host=127.0.0.1,port=8100;urp";
	private static final String OFFICE_CONNECT_STRING = "uno:" + LOCAL_CONNECT_STRING + ";StarOffice.ComponentContext";

	public static final Set<String> OFFICE_MIME_TYPES = ImmutableSet
			.<String> builder()
			.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel", "text/rtf",
					"application/rtf", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
					"application/vnd.ms-powerpoint", "application/x-mspublisher", "application/xls", "application/postscript",
					"application/msword", "application/vnd.oasis.opendocument.spreadsheet", "application/msexcel", "application/excel",
					"application/vnd.openxmlformats-officedoc", "application/vnd.openxmlformats-officedocument.word",
					"application/vnd.oasis.opendocument.text", "application/vnd.ms-word.document.macroEnabled.12", "application/powerpoint")
			.build();

	public static final String ENCODING = "UTF-8";

	private static class XInputStreamAdapter implements XInputStream, XSeekable, Closeable {
		RandomAccessFile file;

		XInputStreamAdapter(File file) throws IOException {
			this.file = new RandomAccessFile(file, "r");
		}

		@Override
		public long getLength() throws com.sun.star.io.IOException {
			try {
				return file.length();
			} catch (IOException e) {
				throw new com.sun.star.io.IOException(e.getMessage(), e);
			}
		}

		@Override
		public long getPosition() throws com.sun.star.io.IOException {
			try {
				return file.getFilePointer();
			} catch (IOException e) {
				throw new com.sun.star.io.IOException(e.getMessage(), e);
			}
		}

		@Override
		public void seek(long pos) throws com.sun.star.lang.IllegalArgumentException, com.sun.star.io.IOException {
			try {
				file.seek(pos);
			} catch (IOException e) {
				throw new com.sun.star.io.IOException(e.getMessage(), e);
			}
		}

		@Override
		public int available() throws NotConnectedException, com.sun.star.io.IOException {
			try {
				return (int) (file.length() - file.getFilePointer());
			} catch (IOException e) {
				throw new com.sun.star.io.IOException(e.getMessage(), e);
			}
		}

		@Override
		public void close() throws IOException {
			file.close();
		}

		@Override
		public void closeInput() throws NotConnectedException, com.sun.star.io.IOException {
			try {
				file.close();
			} catch (IOException e) {
				throw new com.sun.star.io.IOException(e.getMessage(), e);
			}
		}

		@Override
		public int readBytes(byte[][] b, int len) throws NotConnectedException, BufferSizeExceededException, com.sun.star.io.IOException {
			try {
				if (len > 0 && b[0] == null) {
					b[0] = new byte[len];
				}
				int result = file.read(b[0], 0, len);
				return result == -1 ? 0 : result;
			} catch (IOException e) {
				throw new com.sun.star.io.IOException(e.getMessage(), e);
			}
		}

		@Override
		public int readSomeBytes(byte[][] b, int len) throws NotConnectedException, BufferSizeExceededException, com.sun.star.io.IOException {
			try {
				if (len > 0 && b[0] == null) {
					b[0] = new byte[len];
				}
				int result = file.read(b[0], 0, len);
				return result == -1 ? 0 : result;
			} catch (IOException e) {
				throw new com.sun.star.io.IOException(e.getMessage(), e);
			}
		}

		@Override
		public void skipBytes(int len) throws NotConnectedException, BufferSizeExceededException, com.sun.star.io.IOException {
			try {
				file.skipBytes(len);
			} catch (IOException e) {
				throw new com.sun.star.io.IOException(e.getMessage(), e);
			}
		}
	}

	private final PdfPreviewGenerator pdfPreviewGenerator = new PdfPreviewGenerator();

	private static boolean isInitialized = false;
	private static XComponentContext xLocalContext;
	private static XDesktop xDesktop;
	private static XComponentContext xContext;
	private static XMultiComponentFactory xServiceManager;

	public OfficePreviewGenerator() {
	}

	public static synchronized void initialize() throws Exception {
		if (isInitialized) {
			// check to see that the process is still running...
			try {
				// try to connect to office
				XUnoUrlResolver xUrlResolver = UnoUrlResolver.create(xLocalContext);
				Object context = xUrlResolver.resolve(OFFICE_CONNECT_STRING);
				UnoRuntime.queryInterface(XComponentContext.class, context);
			} catch (com.sun.star.connection.NoConnectException ex) {
				isInitialized = false;
			}
		}
		if (!isInitialized) {
			if (xLocalContext == null) {
				xLocalContext = Bootstrap.createInitialComponentContext(null);
				if (xLocalContext == null)
					throw new BootstrapException("no local component context!");
			}

			// locate the executable
			File officeExe = pathToOffice();
			File tmpIn = File.createTempFile("sofficein", ".txt");
			tmpIn.createNewFile();
			tmpIn.deleteOnExit();
			File tmpOut = File.createTempFile("sofficeout", ".txt");
			tmpOut.deleteOnExit();
			File tmpErr = File.createTempFile("sofficeerr", ".txt");
			tmpErr.deleteOnExit();
			ProcessBuilder procBuilder = new ProcessBuilder()
					.command(officeExe.getAbsolutePath(), "-headless", "-accept=" + LOCAL_CONNECT_STRING + ";", "-nofirststartwizard",
							"-nologo", "-nodefault", "-norestore", "-nocrashreport", "-nolockcheck").directory(officeExe.getParentFile())
					.redirectInput(Redirect.from(tmpIn)).redirectOutput(tmpOut).redirectOutput(tmpErr);
			// office itself will take care of avoiding multiple instances
			procBuilder.start();

			final XMultiComponentFactory xLocalServiceManager = xLocalContext.getServiceManager();
			if (xLocalServiceManager == null)
				throw new BootstrapException("no initial service manager!");

			// create a URL resolver
			final XUnoUrlResolver xUrlResolver = UnoUrlResolver.create(xLocalContext);

			xDesktop = TimeUtils.waitFor(300000, 500, new Callable<Pair<Boolean, XDesktop>>() {
				@Override
				public Pair<Boolean, XDesktop> call() throws Exception {
					try {
						// try to connect to office
						Object context = xUrlResolver.resolve(OFFICE_CONNECT_STRING);
						xContext = UnoRuntime.queryInterface(XComponentContext.class, context);
						xServiceManager = xContext.getServiceManager();
						Object oDesktop = xServiceManager.createInstanceWithContext("com.sun.star.frame.Desktop", xContext);

						XDesktop xDesktop = (XDesktop) UnoRuntime.queryInterface(XDesktop.class, oDesktop);
						if (xDesktop == null) {
							throw new BootstrapException("no desktop found!");
						}
						return Pair.create(true, xDesktop);
					} catch (com.sun.star.connection.NoConnectException ex) {
						return Pair.create(false, null);
					}
				}
			});
			isInitialized = true;
		}
	}

	@Override
	public PreviewOutputMetadata generatePreview(InputStream from, OutputStream to) throws IOException {
		File tempPdfFile = null;
		File tempInputFile = null;
		OutputStream tempInputFileOut = null;
		OutputStream tempPdfFileOut = null;
		InputStream tempPdfFileIn = null;
		try {
			initialize();

			// create temp file for reading from
			tempInputFile = File.createTempFile("temp_oo_doc", "");
			tempInputFileOut = new FileOutputStream(tempInputFile);
			StreamUtils.copy(from, tempInputFileOut);
			tempInputFileOut.close();

			// create temp file to write pdf to
			tempPdfFile = File.createTempFile("convert", ".pdf");
			tempPdfFileOut = new BufferedOutputStream(new FileOutputStream(tempPdfFile));

			convertToPdf(tempInputFile, tempPdfFileOut, "writer_pdf_Export");

			tempPdfFileOut.close();

			// convert pdf to image
			tempPdfFileIn = new BufferedInputStream(new FileInputStream(tempPdfFile));
			return pdfPreviewGenerator.generatePreview(tempPdfFileIn, to);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			Closer.closeQuietly(tempInputFileOut, tempPdfFileOut, tempPdfFileIn);
			Closer.deleteQuietly(tempInputFile, tempPdfFile);
		}
	}

	// Method to convert OpenOffice document to html and vice versa
	private void convertToPdf(File inputFile, OutputStream outputStream, String conversionType) throws Exception {
		XComponent document = loadDocument(inputFile);
		saveDocument(document, outputStream, conversionType);
	}

	private XComponent loadDocument(File inputFile) throws Exception {
		XInputStreamAdapter xInputStream = new XInputStreamAdapter(inputFile);

		XComponentLoader xCompLoader = (XComponentLoader) UnoRuntime.queryInterface(XComponentLoader.class, xDesktop);
		PropertyValue[] properties = createProperties("Hidden", true, "InputStream", xInputStream, "AsTemplate", false);

		XComponent document = xCompLoader.loadComponentFromURL("private:stream", "_blank", 0, properties);
		if (document == null) {
			throw new IllegalArgumentException("document could not be loaded");
		}
		return document;
	}

	private void saveDocument(XComponent document, OutputStream outputStream, String conversionType) throws com.sun.star.io.IOException,
			CloseVetoException {
		PropertyValue[] properties = createProperties("OutputStream", new OutputStreamToXOutputStreamAdapter(outputStream), "FilterName",
				conversionType, "FilterData", createProperties("PageRange", "1-1"));
		XStorable storable = (XStorable) UnoRuntime.queryInterface(XStorable.class, document);
		storable.storeToURL("private:stream", properties);
		XCloseable xclosable = (XCloseable) UnoRuntime.queryInterface(XCloseable.class, document);
		xclosable.close(true);
	}

	private PropertyValue[] createProperties(Object... keysAndValues) {
		if (keysAndValues.length % 2 != 0) {
			throw new IllegalArgumentException("Not an even number of params");
		}
		PropertyValue[] propertyValues = new PropertyValue[keysAndValues.length / 2];
		for (int i = 0; i < keysAndValues.length; i += 2) {

			PropertyValue pv = new PropertyValue();
			pv.Name = (String) keysAndValues[i];
			pv.Value = keysAndValues[i + 1];
			propertyValues[i / 2] = pv;
		}
		return propertyValues;
	}

	@Override
	public boolean supportsContentType(String contentType, String extension) {
		return OFFICE_MIME_TYPES.contains(contentType);
	}

	@Override
	public long calculateNeededMemoryBytesForPreview(String mimeType, long contentSize) {
		// whole file is read into memory pretty much
		return contentSize * 2;
	}

	private static File pathToOffice() throws FileNotFoundException {
		for (String path : OPENOFFICE_PATHS) {
			File exeFile = new File(path);
			if (exeFile.exists()) {
				return exeFile;
			}
		}
		throw new FileNotFoundException("Path to office executable not found in "
				+ StringUtils.join(OPENOFFICE_PATHS, File.pathSeparatorChar));
	}
}
