package org.sagebionetworks.repo.manager.grid.synch.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

public class RowWriterReaderTest {

	@Test
	public void testRoundTrip() throws IOException {
		File temp = File.createTempFile("RowWriterReaderTest", ".bin");
		try {
			List<DiskPointer> dp = new ArrayList<>();
			List<SynchRow> rows = List.of(
					new SynchRow(
							new TreeMap<>(Map.of("aLong", new ConValue(ConType.LONG, 123L), "aBoolean",
									new ConValue(ConType.BOOLEAN, true), "aString",
									new ConValue(ConType.STRING, "some string"))),
							"one", new SynapseRow().setRowId(34L).setVersionNumber(3L).setEtag("e")),
					new SynchRow(new TreeMap<>(Map.of("aLong", new ConValue(ConType.LONG, 452L), "aBoolean",
							new ConValue(ConType.BOOLEAN, Boolean.FALSE), "aString",
							new ConValue(ConType.STRING, "something elese"))), "two"),
					new SynchRow(new TreeMap<>(Collections.emptyMap()), "three")

			);
			try (RowWriter writer = new RowWriter(new BufferedOutputStream(new FileOutputStream(temp)))) {
				rows.forEach(r -> dp.add(writer.nextRow(r)));
			}
			List<SynchRow> fetched = new ArrayList<>();
			try (RowReader reader = new RowReader(dp, new RandomAccessFile(temp, "r"))) {

				for (SynchRow row : rows) {
					Optional<RowHeader> header = reader.consumeRow(row.getKey());
					if (header.isPresent()) {
						fetched.add(header.get().fetchRow());
					}
				}
			}
			assertEquals(rows, fetched);
		} finally {
			temp.delete();
		}
	}
}
