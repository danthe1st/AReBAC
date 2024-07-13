package io.github.danthe1st.arebac.data.graph_pattern.constraints;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import io.github.danthe1st.arebac.TypeMissmatchException;
import io.github.danthe1st.arebac.data.graph_pattern.AttributeValue;
import io.github.danthe1st.arebac.data.graph_pattern.AttributeValue.NumericalAttributeValue;

public record AttributeRequirement(
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
	}

	public boolean evaluate(Map<String, AttributeValue<?>> attributes) {
		AttributeValue<?> attributeValue = attributes.get(key);
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

	private boolean checkNumeric(AttributeValue<?> toCheck, Predicate<NumericalAttributeValue<?>> evaluator) {
		if(!(toCheck instanceof NumericalAttributeValue<?> checked)){
			throw new TypeMissmatchException(NumericalAttributeValue.class, toCheck);
		}
		return evaluator.test(checked);
	}
}
