sadcas2222222222222222222222 096package com.bonsaimarket.backend.cart;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** Cart JSON quantities must be integer tokens; range and null checks stay in Bean Validation. */
public final class CartQuantityDeserializer extends ValueDeserializer<Integer> {
    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return context.reportInputMismatch(Integer.class, "Số lượng phải là số nguyên JSON.");
        }
        return parser.getIntValue();
    }
}
