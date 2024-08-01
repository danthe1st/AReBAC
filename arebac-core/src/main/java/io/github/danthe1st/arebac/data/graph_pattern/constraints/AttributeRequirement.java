package io.github.danthe1st.arebac.data.graph_pattern.constraints;

import java.util.Objects;
import java.util.function.Predicate;

import io.github.danthe1st.arebac.TypeMissmatchException;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeAware;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.NumericalAttributeValue;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.StringAttribute;

/**
 * A constraint on nodes or edges containing a requirement on a specific attribute.
 *
 * The attribute is identified by a {@link AttributeRequirement#key} and compared to a {@link AttributeRequirement#value} using an {@link AttributeRequirement#operator}, e.g. <code>someAttribute.someKey <= someValue</code>.
 */
public record AttributeRequirement(
		/**
		 * Identifies the constraint attribute.
		 */
		String key, AttributeRequirementOperator operator, AttributeValue<?> value) {

	public static final String ID_KEY = "#id";
	
	public AttributeRequirement {
		Objects.requireNonNull(key);
		Objects.requireNonNull(value);
		Objects.requireNonNull(operator);
		
		boolean boolToEnsureExhaustivenessChecking = switch(operator) {
		case GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL ->
			checkNumeric(value, val -> true);
		case EQUAL -> true;
		};
		assert boolToEnsureExhaustivenessChecking;
		
		if(ID_KEY.equals(key) && !(value instanceof StringAttribute)){
			throw new IllegalArgumentException("ID requirements must be Strings");
		}
	}

	public boolean evaluate(AttributeAware aware) {
		if(ID_KEY.equals(key)){
			return value.value().equals(aware.id());
		}
		AttributeValue<?> attributeValue = aware.getAttribute(key);
		if(attributeValue == null){
			return false;
		}

		return switch(operator) {
		case EQUAL ->
			attributeValue.equals(value);
		case LESS_THAN ->
			checkNumeric(attributeValue, val -> val.lessThan(value));
		case LESS_THAN_OR_EQUAL ->
			checkNumeric(attributeValue, val -> val.equals(value) || val.lessThan(value));
		case GREATER_THAN ->
			checkNumeric(value, val -> val.lessThan(attributeValue));
		case GREATER_THAN_OR_EQUAL ->
			checkNumeric(value, val -> val.equals(attributeValue) || val.lessThan(attributeValue));
		};

	}

	private static boolean checkNumeric(AttributeValue<?> toCheck, Predicate<NumericalAttributeValue<?>> evaluator) {
		if(!(toCheck instanceof NumericalAttributeValue<?> checked)){
			throw new TypeMissmatchException(NumericalAttributeValue.class, toCheck);
		}
		return evaluator.test(checked);
	}
}
