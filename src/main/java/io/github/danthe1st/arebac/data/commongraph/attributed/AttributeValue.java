package io.github.danthe1st.arebac.data.commongraph.attributed;

import java.util.Objects;

import io.github.danthe1st.arebac.TypeMissmatchException;

/**
 * Holds a value of an attribute in an attributed graph.
 * @see StringAttribute
 * @see BooleanAttribute
 * @see IntAttribute
 * @param <T> The type of the value
 */
public sealed interface AttributeValue<T> {

	T value();

	public sealed interface NumericalAttributeValue<T> extends AttributeValue<T> {
		boolean lessThan(AttributeValue<?> other);
	}

	public record StringAttribute(String value) implements AttributeValue<String> {
		public StringAttribute {
			Objects.requireNonNull(value);
		}
	}

	public record BooleanAttribute(boolean boolValue) implements AttributeValue<Boolean> {
		public BooleanAttribute {
			Objects.requireNonNull(boolValue);
		}

		@Override
		public Boolean value() {
			return boolValue;
		}
	}

	public record IntAttribute(int intValue) implements NumericalAttributeValue<Integer> {

		@Override
		public boolean lessThan(AttributeValue<?> other) {
			return switch(other) {
			case StringAttribute attr -> throw new TypeMissmatchException(NumericalAttributeValue.class, attr);
			case BooleanAttribute attr -> throw new TypeMissmatchException(NumericalAttributeValue.class, attr);
			case IntAttribute(int o) -> value() < o;
			};
		}

		@Override
		public Integer value() {
			return intValue;
		}
	}

	static IntAttribute attribute(int value) {
		return new IntAttribute(value);
	}

	static StringAttribute attribute(String value) {
		return new StringAttribute(value);
	}
	
	static BooleanAttribute attribute(boolean value) {
		return new BooleanAttribute(value);
	}
}
