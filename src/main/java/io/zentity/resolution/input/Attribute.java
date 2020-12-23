package io.zentity.resolution.input;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.util.ClassUtil;
import io.zentity.common.Json;
import io.zentity.common.Patterns;
import io.zentity.model.ValidationException;
import io.zentity.resolution.input.value.Value;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Attribute {

    private final String name;
    private final String type;
    private final Map<String, String> params = new HashMap<>();
    private final Set<Value> values = new HashSet<>();

    public Attribute(String name, String type, JsonNode json) throws ValidationException, JsonProcessingException {
        this(name, type);
        this.deserialize(json);
    }

    public Attribute(String name, String type, String json) throws ValidationException, IOException {
        this(name, type);
        this.deserialize(json);
    }

    public Attribute(String name, String type, Map<String, String> params, Set<Value> values) throws ValidationException {
        this(name, type);
        this.params.putAll(params);
        this.values.addAll(values);
    }

    public Attribute(String name, String type) throws ValidationException {
        validateName(name);
        validateType(type);
        this.name = name;
        this.type = type;
    }

    public String name() {
        return this.name;
    }

    public Map<String, String> params() {
        return this.params;
    }

    public String type() {
        return this.type;
    }

    public Set<Value> values() {
        return this.values;
    }

    private void validateName(String value) throws ValidationException {
        if (Patterns.EMPTY_STRING.matcher(value).matches()) {
            throw new ValidationException("'attributes' has an attribute with empty name.");
        }
    }

    /**
     * Validate the value of "attributes".ATTRIBUTE_NAME."type".
     * Must be a non-empty string containing a recognized type.
     *
     * @param value The value of "attributes".ATTRIBUTE_NAME."type".
     * @throws ValidationException
     */
    private void validateType(String value) throws ValidationException {
        if (Patterns.EMPTY_STRING.matcher(value).matches()) {
            throw new ValidationException("'attributes." + this.name + ".type'' must not be empty.");
        }
        if (!io.zentity.model.Attribute.VALID_TYPES.contains(value)) {
            throw new ValidationException("'attributes." + this.name + ".type' has an unrecognized type '" + value + "'.");
        }
    }

    /**
     * Parse a single input attribute value. The following examples are all valid attribute structures, although the
     * first example would be converted to the second example.
     * <pre>
     *  {
     *   ATTRIBUTE_NAME: [
     *     ATTRIBUTE_VALUE,
     *     ...
     *   ]
     * }
     *
     * {
     *   ATTRIBUTE_NAME: {
     *     "values": [
     *       ATTRIBUTE_VALUE,
     *       ...
     *     ]
     *   }
     * }
     *
     * {
     *   ATTRIBUTE_NAME: {
     *     "values": [
     *       ATTRIBUTE_VALUE,
     *       ...
     *     ],
     *     "params": {
     *       ATTRIBUTE_PARAM_FIELD: ATTRIBUTE_PARAM_VALUE,
     *       ...
     *     }
     *   }
     * }
     *
     * {
     *   ATTRIBUTE_NAME: {
     *     "params": {
     *       ATTRIBUTE_PARAM_FIELD: ATTRIBUTE_PARAM_VALUE,
     *       ...
     *     }
     *   }
     * }
     * </pre>
     *
     * @param json Attribute object of an entity model.
     * @throws ValidationException
     */
    private void deserialize(JsonNode json) throws ValidationException, JsonProcessingException {
        if (json.isNull()) {
            return;
        }
        if (!json.isObject() && !json.isArray()) {
            throw new ValidationException("'attributes." + this.name + "' must be an object or array.");
        }

        Iterator<JsonNode> valuesNode = ClassUtil.emptyIterator();
        Iterator<Map.Entry<String, JsonNode>> paramsNode = ClassUtil.emptyIterator();

        // Parse values from array
        if (json.isArray()) {
            valuesNode = json.elements();
        } else if (json.isObject()) {

            // Parse values from object
            if (json.has("values")) {
                if (!json.get("values").isArray()) {
                    throw new ValidationException("'attributes." + this.name + ".values' must be an array.");
                }
                valuesNode = json.get("values").elements();
            }

            // Parse params from object
            if (json.has("params")) {
                if (!json.get("params").isObject()) {
                    throw new ValidationException("'attributes." + this.name + ".params' must be an object.");
                }
                paramsNode = json.get("params").fields();
            }
        } else {
            throw new ValidationException("'attributes." + this.name + "' must be an object or array.");
        }

        // Set any values or params that were specified in the input.
        while (valuesNode.hasNext()) {
            JsonNode valueNode = valuesNode.next();
            this.values().add(Value.create(this.type, valueNode));
        }

        // Set any params that were specified in the input, with the values serialized as strings.
        this.params().putAll(Json.toStringMap(paramsNode));
    }

    private void deserialize(String json) throws ValidationException, IOException {
        deserialize(Json.MAPPER.readTree(json));
    }
}
