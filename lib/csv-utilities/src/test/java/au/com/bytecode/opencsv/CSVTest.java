package au.com.bytecode.opencsv;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;


public class CSVTest {
    
	@Test
    public void testDefault() {
		CSV DEFAULT = CSV.create();

		// writing
		StringWriter w = new StringWriter();
		DEFAULT.writeAndClose(w, new CSVWriteProc() {
			@Override
			public void process(CSVWriter out) {
				try {
					out.writeNext("Col1", "Col2", "Col3");
					out.writeNext("V1", "V2", "V3");
					out.writeNext(",V,2,", "\"V2\"", "  V3  ");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				
			}
		});
		String text = w.getBuffer().toString();
		
		assertEquals(
				"\"Col1\",\"Col2\",\"Col3\"\n" +
				"\"V1\",\"V2\",\"V3\"\n" +
				"\",V,2,\",\"\"\"V2\"\"\",\"  V3  \"\n",
						text);

		// reading
		DEFAULT.readAndClose(new StringReader(text), new CSVReadProc() {
			@Override
			public void procRow(int rowIndex, String... values) {
				switch (rowIndex) {
				case 0:
					assertArrayEquals(new String [] {"Col1", "Col2", "Col3"} , values);
					break;
				case 1:
					assertArrayEquals(new String [] {"V1", "V2", "V3"} , values);
					break;
				case 2:
					assertArrayEquals(new String [] {",V,2,", "\"V2\"", "  V3  "} , values);
					break;
				default: fail();
				}
			}
		});
    }
	
	@Test
    public void testConfigured() {
		CSV CFG = CSV
				.separator('|')
				.charset("UTF-8")
				.escape('^')
				.quote('*')
				.ignoreLeadingWhiteSpace()
				.lineEnd("#")
				.strictQuotes()
				.skipLines(1)
				.create();
		
		// writing
		StringWriter w = new StringWriter();
		CFG.writeAndClose(w, new CSVWriteProc() {
			@Override
			public void process(CSVWriter out) {
				try {
					out.writeNext("Col1", "Col2", "Col3");
					out.writeNext("V1", "V2", "V3");
					out.writeNext("|V|2|", "*V2*", "  V3  ");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				
			}
		});
		String text = w.getBuffer().toString();
		
		assertEquals(
				"*Col1*|*Col2*|*Col3*#" +
				"*V1*|*V2*|*V3*#" +
				"*|V|2|*|***V2***|*  V3  *#",
				text);

		// reading
		CFG.readAndClose(new StringReader("*Col1*|*Col2*|*Col3*#" +
				"*V1*|*V2*|*V3*#" +
				"*|V|2|*|*^*V2^**|  *  V3  *#"), new CSVReadProc() {
			@Override
			public void procRow(int rowIndex, String... values) {
				switch (rowIndex) {
				case 0:
					assertArrayEquals(new String [] {"V1", "V2", "V3"} , values);
					break;
				case 1:
					assertArrayEquals(new String [] {"|V|2|", "*V2*", "  V3  "} , values);
					break;
				default: fail();
				}
			}
		});
    }

}
