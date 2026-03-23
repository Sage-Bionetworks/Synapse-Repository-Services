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
			List<RowSourceItem> rows = List.of(
					new RowSourceItem(
							new TreeMap<>(Map.of("aLong", new ConValue(ConType.LONG, 123L), "aBoolean",
									new ConValue(ConType.BOOLEAN, true), "aString",
									new ConValue(ConType.STRING, "some string"))),
							"one", new SynapseRow().setRowId(34L).setVersionNumber(3L).setEtag("e")),
					new RowSourceItem(new TreeMap<>(Map.of("aLong", new ConValue(ConType.LONG, 452L), "aBoolean",
							new ConValue(ConType.BOOLEAN, Boolean.FALSE), "aString",
							new ConValue(ConType.STRING, "something elese"))), "two"),
					new RowSourceItem(new TreeMap<>(Collections.emptyMap()), "three")

			);
			try (RowSourceItemWriter writer = new RowSourceItemWriter(new BufferedOutputStream(new FileOutputStream(temp)))) {
				rows.forEach(r -> dp.add(writer.nextRow(r)));
			}
			List<RowSourceItem> fetched = new ArrayList<>();
			try (RowSourceItemReader reader = new RowSourceItemReader(dp, new RandomAccessFile(temp, "r"))) {

				for (RowSourceItem row : rows) {
					Optional<RowSourceItemReference> header = reader.consumeRow(row.getKey());
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
