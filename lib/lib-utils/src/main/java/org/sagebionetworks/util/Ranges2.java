package org.sagebionetworks.util;

import java.util.Collections;
import java.util.Iterator;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;

public class Ranges2 {
	public static Iterable<Range<Long>> rangeIteratable(Range<Long> rangeToIterate, long maxBlockSize) {

		final long rangeMinInclusive = rangeToIterate.lowerEndpoint() + (rangeToIterate.lowerBoundType() == BoundType.OPEN ? 1 : 0);
		final long rangeMaxExclusive = rangeToIterate.upperEndpoint() + (rangeToIterate.upperBoundType() == BoundType.CLOSED ? 1 : 0);

		final long distance = rangeMaxExclusive - rangeMinInclusive;
		if (distance <= 0) {
			// nothing to iterator over
			return Collections.emptyList();
		}

		// try to keep the blocks as evenly sized as possible
		final long stepCount = distance / maxBlockSize + (distance % maxBlockSize == 0 ? 0 : 1);
		final long blockSize = distance / stepCount;

		return new Iterable<Range<Long>>() {
			@Override
			public Iterator<Range<Long>> iterator() {
				return new Iterator<Range<Long>>() {
					long lastIndex = rangeMinInclusive - 1;
					long remainder = distance % stepCount;

					@Override
					public Range<Long> next() {
						long nextBlockSize = blockSize;
						if (remainder > 0) {
							nextBlockSize++;
							remainder--;
						}
						Range<Long> step = Ranges.closedOpen(lastIndex + 1, Math.min(rangeMaxExclusive, lastIndex + 1 + nextBlockSize));
						lastIndex = lastIndex + nextBlockSize;
						return step;
					}

					@Override
					public boolean hasNext() {
						return lastIndex + 1 < rangeMaxExclusive;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException("Remove not supported");
					}
				};
			}
		};
	}
}
