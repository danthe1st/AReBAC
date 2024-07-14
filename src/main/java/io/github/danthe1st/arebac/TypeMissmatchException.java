package io.github.danthe1st.arebac;

public final class TypeMissmatchException extends RuntimeException {
	public TypeMissmatchException(Class<?> expected, Object value) {
		super("expected: " + expected.getName() + ", actual: " + value.getClass() + " (object is " + value + ")");
	}
}
