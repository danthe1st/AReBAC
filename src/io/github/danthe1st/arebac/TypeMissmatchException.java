package io.github.danthe1st.arebac;

public class TypeMissmatchException extends RuntimeException {
	public TypeMissmatchException(Class<?> expected, Object value) {
		super("expected: " + expected.getName() + ", actual: " + value.getClass() + " (object is " + value + ")");
	}
}
