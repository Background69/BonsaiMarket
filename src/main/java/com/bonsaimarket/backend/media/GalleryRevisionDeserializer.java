package com.bonsaimarket.backend.media;

import tools.jackson.core.*;
import tools.jackson.databind.*;

/** Never truncate a fractional JSON revision before applying a concurrency guard. */
public final class GalleryRevisionDeserializer extends ValueDeserializer<Long> {
    @Override public Long deserialize(JsonParser parser,DeserializationContext context){
        if(!parser.hasToken(JsonToken.VALUE_NUMBER_INT))return context.reportInputMismatch(Long.class,"Revision gallery phải là số nguyên JSON.");
        return parser.getLongValue();
    }
}
