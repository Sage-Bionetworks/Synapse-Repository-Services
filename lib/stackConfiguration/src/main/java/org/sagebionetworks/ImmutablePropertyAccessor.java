package org.sagebionetworks;

public class ImmutablePropertyAccessor implements PropertyAccessor {
	private String str = null;
	private Long l = null;
	private Integer i = null;
	private Boolean b = null;
	private Double d = null;

	public ImmutablePropertyAccessor(String str) {
		this.str = str;
	}

	public ImmutablePropertyAccessor(Long l) {
		this.l = l;
	}

	public ImmutablePropertyAccessor(Integer i) {
		this.i = i;
	}

	public ImmutablePropertyAccessor(Boolean b) {
		this.b = b;
	}

	public ImmutablePropertyAccessor(Double d) {
		this.d = d;
	}

	@Override
	public String getString() {
		return str;
	}

	@Override
	public long getLong() {
		return l;
	}

	@Override
	public int getInteger() {
		return i;
	}

	@Override
	public boolean getBoolean() {
		return b;
	}

	@Override
	public double getDouble() {
		return d;
	}
}
