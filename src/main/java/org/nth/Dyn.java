package org.nth;

import com.google.gson.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * A dynamic wrapper for storing and manipulating values of multiple types.
 * Supports strings, numbers, booleans, collections, JSON, and more.
 */
public final class Dyn {
    private Map<Class<?>, Object> values = new HashMap<>(); // Store values by type
    private Class<?> primaryType; // Most recently set type
    boolean isNullSafe = false;
    private boolean isImmutable = false;
    private static final Gson gson = new Gson();
    private static final Map<String, Method> methodCache = new ConcurrentHashMap<>(); // Cache for method lookups

    // Static Factory Methods

    /** Creates a new Dyn with the given value. */
    public static Dyn of(Object value) {
        return new Dyn().set(value);
    }

    /** Shorthand for of(value). */
    public static Dyn $(Object value) {
        return of(value);
    }

    /** Creates an immutable Dyn with the given value. */
    public static Dyn immutable(Object value) {
        Dyn dyn = new Dyn().set(value);
        dyn.isImmutable = true;
        return dyn;
    }

    /** Creates a list Dyn from elements. */
    public static Dyn list(Object... elements) {
        List<Object> list = new ArrayList<>();
        for (Object e : elements) {
            list.add(e instanceof Dyn ? ((Dyn) e).get() : e);
        }
        return new Dyn().set(list, List.class);
    }

    /** Creates a set Dyn from elements. */
    public static Dyn set(Object... elements) {
        return new Dyn().set(Arrays.stream(elements).map(Dyn::of).collect(Collectors.toSet()), Set.class);
    }

    /** Creates a map Dyn from key-value pairs. */
    public static Dyn map(Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("map() requires even key-value pairs.");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put(keyValuePairs[i].toString(), keyValuePairs[i + 1]);
        }
        return new Dyn().set(map, Map.class);
    }

    /** Creates a concurrent list Dyn. */
    public static Dyn concurrentList() {
        return new Dyn().set(new CopyOnWriteArrayList<>(), List.class);
    }

    /** Creates an array Dyn from elements. */
    public static Dyn array(Object... elements) {
        return new Dyn().set(Arrays.stream(elements).map(Dyn::of).toArray(Dyn[]::new));
    }

    /** Creates a LocalDate Dyn. */
    public static Dyn localDate(int year, int month, int dayOfMonth) {
        return new Dyn().set(LocalDate.of(year, month, dayOfMonth));
    }

    /** Creates a LocalDateTime Dyn. */
    public static Dyn localDateTime(int year, int month, int dayOfMonth, int hour, int minute) {
        return new Dyn().set(LocalDateTime.of(year, month, dayOfMonth, hour, minute));
    }

    /** Creates a null-safe Dyn. */
    public static Dyn optional(Object value) {
        Dyn dyn = new Dyn().set(value);
        dyn.isNullSafe = true;
        return dyn;
    }

    /** Creates a Dyn from JSON string. */
    public static Dyn fromJson(String json) {
        try {
            return new Dyn().set(gson.fromJson(json, Object.class));
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
        }
    }

    // Core Functionality
    private Dyn() {}

    /**
     * Sets a value, optionally under a specific type key.
     * @return this Dyn for chaining
     */
    public Dyn set(Object value, Class<?> typeKey) {
        if (isImmutable) {
            throw new UnsupportedOperationException("Cannot modify immutable Dyn");
        }
        if (value instanceof Dyn) {
            Dyn other = (Dyn) value;
            this.values.putAll(other.values);
            this.primaryType = other.primaryType;
            this.isNullSafe = other.isNullSafe;
        } else {
            Class<?> type = typeKey != null ? typeKey : (value != null ? value.getClass() : Object.class);
            this.values.put(type, value);
            this.primaryType = type;
        }
        return this;
    }

    public Dyn set(Object value) {
        return set(value, null);
    }

    /** Gets the primary value, cast to the requested type. */
    @SuppressWarnings("unchecked")
    public <T> T get() {
        Object value = values.get(primaryType);
        if (value == null && !isNullSafe) {
            throw new NullPointerException("Dyn value is null for type " + (primaryType != null ? primaryType.getSimpleName() : "null"));
        }
        return (T) value;
    }

    /**
     * Gets a value of the specified type, checking type hierarchy.
     * @throws NullPointerException if no value is found and not null-safe
     * @throws ClassCastException if the value cannot be cast to the type
     */
    public <T> T get(Class<T> type) {
        Object value = values.get(type);
        if (value != null) {
            return type.cast(value);
        }
        for (Map.Entry<Class<?>, Object> entry : values.entrySet()) {
            if (type.isInstance(entry.getValue())) {
                return type.cast(entry.getValue());
            }
        }
        if (!isNullSafe) {
            throw new NullPointerException("No value found for type " + type.getSimpleName() +
                    ". Available types: " + values.keySet().stream().map(Class::getSimpleName).collect(Collectors.joining(", ")));
        }
        return null;
    }

    // Type-Specific Getters

    /** Gets the String value. */
    public String string() {
        return get(String.class);
    }

    /** Gets the int value, defaulting to 0 if null. */
    public int intValue() {
        Number num = get(Number.class);
        return num != null ? num.intValue() : 0;
    }

    /** Gets the double value, defaulting to 0.0 if null. */
    public double number() {
        Number num = get(Number.class);
        return num != null ? num.doubleValue() : 0.0;
    }

    /** Gets the boolean value, defaulting to false if null. */
    public boolean bool() {
        Boolean b = get(Boolean.class);
        return b != null ? b : false;
    }

    /** Gets the list value with elements of the specified type. */
    public <T> List<T> list(Class<T> elementType) {
        List<?> rawList = get(List.class);
        if (rawList == null) return new ArrayList<>();

        return rawList.stream()
                .map(e -> elementType.cast(e))
                .collect(Collectors.toList());
    }

    /** Gets the map value with keys and values of the specified types. */
    public <K, V> Map<K, V> map(Class<K> keyType, Class<V> valueType) {
        return get(Map.class) != null ? ((Map<?, ?>) get(Map.class)).entrySet().stream()
                .collect(Collectors.toMap(
                        e -> keyType.cast(e.getKey()),
                        e -> e.getValue() instanceof Dyn ? ((Dyn) e.getValue()).get(valueType) : valueType.cast(e.getValue()),
                        (a, b) -> b, LinkedHashMap::new)) : new LinkedHashMap<>();
    }

    // Type Checking
    public boolean isString() { return values.values().stream().anyMatch(String.class::isInstance); }
    public boolean isNumber() { return values.values().stream().anyMatch(Number.class::isInstance); }
    public boolean isBoolean() { return values.values().stream().anyMatch(Boolean.class::isInstance); }
    public boolean isList() { return values.values().stream().anyMatch(List.class::isInstance); }
    public boolean isMap() { return values.values().stream().anyMatch(Map.class::isInstance); }
    public boolean isArray() { return values.values().stream().anyMatch(v -> v != null && v.getClass().isArray()); }
    public boolean isLocalDate() { return values.values().stream().anyMatch(LocalDate.class::isInstance); }
    public boolean isLocalDateTime() { return values.values().stream().anyMatch(LocalDateTime.class::isInstance); }

    // Arithmetic Operations

    /** Adds two numbers or concatenates strings. */
    public Dyn add(Dyn other) {
        if (isNumber() && other.isNumber()) {
            return new Dyn().set(new BigDecimal(number()).add(new BigDecimal(other.number())));
        } else if (isString() && other.isString()) {
            return new Dyn().set(string() + other.string());
        }
        throw new UnsupportedOperationException("Add requires numbers or strings");
    }

    /** Subtracts two numbers. */
    public Dyn subtract(Dyn other) {
        if (isNumber() && other.isNumber()) {
            return new Dyn().set(new BigDecimal(number()).subtract(new BigDecimal(other.number())));
        }
        throw new UnsupportedOperationException("Subtract requires numbers");
    }

    /** Multiplies two numbers. */
    public Dyn multiply(Dyn other) {
        if (isNumber() && other.isNumber()) {
            return new Dyn().set(new BigDecimal(number()).multiply(new BigDecimal(other.number())));
        }
        throw new UnsupportedOperationException("Multiply requires numbers");
    }

    /** Divides two numbers. */
    public Dyn divide(Dyn other) {
        if (isNumber() && other.isNumber()) {
            if (other.number() == 0) {
                throw new ArithmeticException("Division by zero");
            }
            return new Dyn().set(new BigDecimal(number()).divide(new BigDecimal(other.number()), 10, BigDecimal.ROUND_HALF_UP));
        }
        throw new UnsupportedOperationException("Divide requires numbers");
    }

    // Bitwise Operations

    /** Performs bitwise AND on integers. */
    public Dyn bitwiseAnd(Dyn other) {
        if (isNumber() && other.isNumber()) {
            return new Dyn().set(intValue() & other.intValue());
        }
        throw new UnsupportedOperationException("Bitwise AND requires integers");
    }

    /** Performs bitwise OR on integers. */
    public Dyn bitwiseOr(Dyn other) {
        if (isNumber() && other.isNumber()) {
            return new Dyn().set(intValue() | other.intValue());
        }
        throw new UnsupportedOperationException("Bitwise OR requires integers");
    }

    // String Operations

    /** Concatenates strings. */
    public Dyn concat(Dyn other) {
        if (isString() && other.isString()) {
            return new Dyn().set(string() + other.string());
        }
        throw new UnsupportedOperationException("Concat requires strings");
    }

    /** Extracts a substring. */
    public Dyn substring(int beginIndex, int endIndex) {
        if (isString()) {
            return new Dyn().set(string().substring(beginIndex, endIndex));
        }
        throw new UnsupportedOperationException("Substring requires a string");
    }

    /** Converts string to uppercase. */
    public Dyn toUpperCase() {
        if (isString()) {
            return new Dyn().set(string().toUpperCase());
        }
        throw new UnsupportedOperationException("toUpperCase requires a string");
    }

    /** Checks if string matches regex. */
    public boolean matches(String regex) {
        if (isString()) {
            return string().matches(regex);
        }
        throw new UnsupportedOperationException("Matches requires a string");
    }

    /** Replaces all matches of regex in string. */
    public Dyn replaceAll(String regex, String replacement) {
        if (isString()) {
            return new Dyn().set(string().replaceAll(regex, replacement));
        }
        throw new UnsupportedOperationException("replaceAll requires a string");
    }

    // Collection Operations

    /** Adds an element to a list, validating type compatibility. */
    public Dyn addElement(Object element) {
        if (isImmutable) {
            throw new UnsupportedOperationException("Cannot modify immutable Dyn");
        }
        if (isList()) {
            List<Object> list = list(Object.class);
            Object toAdd = element instanceof Dyn ? ((Dyn) element).get() : element;
            list.add(toAdd);
            return this;
        }
        throw new UnsupportedOperationException("addElement requires a list. Use Dyn.list() to create one.");
    }

    /** Removes an element from a list. */
    public Dyn removeElement(Object element) {
        if (isImmutable) {
            throw new UnsupportedOperationException("Cannot modify immutable Dyn");
        }
        if (isList()) {
            List<Object> list = list(Object.class);
            Object toRemove = element instanceof Dyn ? ((Dyn) element).get() : element;
            list.remove(toRemove);
            return this;
        }
        throw new UnsupportedOperationException("removeElement requires a list. Use Dyn.list() to create one.");
    }

    /** Puts a key-value pair in a map. */
    public Dyn putKeyValue(String key, Object value) {
        if (isImmutable) {
            throw new UnsupportedOperationException("Cannot modify immutable Dyn");
        }
        if (isMap()) {
            map(String.class, Object.class).put(key, value instanceof Dyn ? value : Dyn.of(value));
            return this;
        }
        throw new UnsupportedOperationException("putKeyValue requires a map. Use Dyn.map() to create one.");
    }

    /** Clears a list or map. */
    public Dyn clear() {
        if (isImmutable) {
            throw new UnsupportedOperationException("Cannot modify immutable Dyn");
        }
        if (isList()) {
            list(Object.class).clear();
            return this;
        } else if (isMap()) {
            map(Object.class, Object.class).clear();
            return this;
        }
        throw new UnsupportedOperationException("clear requires a list or map");
    }

    /** Returns the size of a list or map. */
    public int size() {
        if (isList()) {
            return list(Object.class).size();
        } else if (isMap()) {
            return map(Object.class, Object.class).size();
        } else if (isString()) {
            return string().length();
        }
        throw new UnsupportedOperationException("size requires a list, map, or string");
    }

    /** Checks if a list or map is empty. */
    public boolean isEmpty() {
        if (isList()) {
            return list(Object.class).isEmpty();
        } else if (isMap()) {
            return map(Object.class, Object.class).isEmpty();
        }
        throw new UnsupportedOperationException("isEmpty requires a list or map");
    }

    // Comparison Methods

    /** Checks if values are equal. */
    public boolean isEqualTo(Dyn other) {
        Object value = values.get(primaryType);
        Object otherValue = other.values.get(other.primaryType);
        return Objects.equals(value, otherValue);
    }

    /** Checks if this number is greater than another. */
    public boolean isGreaterThan(Dyn other) {
        if (isNumber() && other.isNumber()) {
            return number() > other.number();
        }
        throw new UnsupportedOperationException("Comparison requires numbers");
    }

    // Method Invocation

    /** Invokes a method on the primary value. */
    public Dyn call(String methodName, Object... args) {
        Object target = values.get(primaryType);
        if (target == null) {
            throw new IllegalStateException("No value to call method on");
        }
        try {
            Class<?>[] argTypes = Arrays.stream(args)
                    .map(a -> a instanceof Dyn ? ((Dyn) a).primaryType : a != null ? a.getClass() : Object.class)
                    .toArray(Class[]::new);
            Object[] processedArgs = Arrays.stream(args)
                    .map(a -> a instanceof Dyn ? ((Dyn) a).get() : a)
                    .toArray();
            String cacheKey = target.getClass().getName() + "#" + methodName + Arrays.toString(argTypes);
            Method method = methodCache.computeIfAbsent(cacheKey, k -> findMethod(target.getClass(), methodName, argTypes));
            method.setAccessible(true);
            return new Dyn().set(method.invoke(target, processedArgs));
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke method " + methodName + ": " + e.getMessage(), e);
        }
    }

    private Method findMethod(Class<?> clazz, String methodName, Class<?>[] argTypes) {
        try {
            return clazz.getMethod(methodName, argTypes);
        } catch (NoSuchMethodException e) {
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName)) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == argTypes.length) {
                        boolean match = true;
                        for (int i = 0; i < paramTypes.length; i++) {
                            if (argTypes[i] != null && !paramTypes[i].isAssignableFrom(argTypes[i])) {
                                match = false;
                                break;
                            }
                        }
                        if (match) return method;
                    }
                }
            }
            throw new RuntimeException("No matching method " + methodName + " in " + clazz.getSimpleName(), e);
        }
    }

    // Type Conversion

    /** Converts the primary value to the specified type. */
    public <T> T to(Class<T> type) {
        Object value = values.get(primaryType);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        try {
            if (type == String.class) {
                return type.cast(value.toString());
            } else if (type == Integer.class) {
                return type.cast(Integer.parseInt(value.toString()));
            } else if (type == Double.class) {
                return type.cast(Double.parseDouble(value.toString()));
            } else if (type == Boolean.class) {
                return type.cast(Boolean.parseBoolean(value.toString()));
            } else {
                return gson.fromJson(gson.toJson(value), type);
            }
        } catch (Exception e) {
            throw new ClassCastException("Cannot convert " + value.getClass().getSimpleName() +
                    " to " + type.getSimpleName() + ": " + e.getMessage());
        }
    }

    // JSON Support

    /** Serializes the primary value to JSON. */
    public String asJsonString() {
        return gson.toJson(values.get(primaryType));
    }

    /** Gets a JSON map value by key. */
    public Dyn getJson(String key) {
        if (isMap()) {
            return Dyn.of(map(Object.class, Object.class).get(key));
        }
        throw new UnsupportedOperationException("Not a JSON map");
    }

    /** Deserializes JSON to the specified type. */
    public <T> T fromJson(Class<T> type) {
        return gson.fromJson(asJsonString(), type);
    }

    // Stream Support

    /** Creates a stream from a list or array. */
    public Stream<Dyn> stream() {
        if (isList()) {
            return list(Object.class).stream().map(Dyn::of);
        } else if (isArray()) {
            return Arrays.stream((Object[]) values.get(primaryType)).map(Dyn::of);
        }
        throw new UnsupportedOperationException("Stream not supported for type " + (primaryType != null ? primaryType.getSimpleName() : "null"));
    }

    // Exception Handling

    /** Executes a block with exception handling. */
    public Dyn tryCatch(Consumer<Dyn> tryBlock, BiConsumer<Dyn, Exception> catchBlock) {
        try {
            tryBlock.accept(this);
            return this;
        } catch (Exception e) {
            catchBlock.accept(this, e);
            return this;
        }
    }

    // Validation

    /** Validates that a value of the specified type exists. */
    public Dyn validate(Class<?> type) {
        if (!values.values().stream().anyMatch(type::isInstance)) {
            throw new IllegalStateException("Dyn does not contain a value of type " + type.getSimpleName());
        }
        return this;
    }

    /** Checks if a value of the specified type exists. */
    public boolean hasValue(Class<?> type) {
        return values.values().stream().anyMatch(type::isInstance);
    }

    // Debug Support

    /** Prints the internal state for debugging. */
    public Dyn debug() {
        System.out.println("Dyn{primaryType=" + (primaryType != null ? primaryType.getSimpleName() : "null") +
                ", values=" + values + ", isNullSafe=" + isNullSafe + ", isImmutable=" + isImmutable + "}");
        return this;
    }

    // Utility Methods
    @Override
    public String toString() {
        Object value = values.get(primaryType);
        return value != null ? value.toString() : "null";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Dyn)) return false;
        Dyn other = (Dyn) obj;
        return Objects.equals(values, other.values) && primaryType == other.primaryType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(values, primaryType);
    }
}