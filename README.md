# Dyn - Dynamic Value Handler for Java

[![Maven Central](https://img.shields.io/maven-central/v/org.nth/dyn.svg)](https://search.maven.org/search?q=g:org.nth%20AND%20a:dyn)
[![javadoc](https://javadoc.io/badge2/org.nth/dyn/javadoc.svg)](https://javadoc.io/doc/org.nth/dyn)
[![GitHub License](https://img.shields.io/github/license/nth-org/dyn)](https://github.com/nth-org/dyn/blob/main/LICENSE)
[![Build Status](https://img.shields.io/github/workflow/status/nth-org/dyn/Java%20CI)](https://github.com/nth-org/dyn/actions)
[![Coverage Status](https://img.shields.io/codecov/c/github/nth-org/dyn)](https://codecov.io/gh/nth-org/dyn)
[![GitHub Issues](https://img.shields.io/github/issues/nth-org/dyn)](https://github.com/nth-org/dyn/issues)

## Overview

Dyn is a versatile Java library designed to handle dynamic values with multiple representations. It simplifies type conversion, provides flexible data manipulation, and offers a clean API for working with dynamic data.

## Features

- 🔄 **Multiple Value Representations** - Store and access the same data in different formats
- 🧩 **Type Flexibility** - Easily switch between types without explicit casting
- 🔢 **Arithmetic Operations** - Perform math operations directly on Dyn objects
- 📝 **String Manipulation** - Built-in methods for string operations
- 📊 **Collection Support** - Work with lists and maps using a consistent API
- 🔄 **JSON Integration** - Seamlessly convert between JSON and Java objects
- 🛡️ **Null Safety** - Built-in null handling to prevent NullPointerExceptions
- ⚡ **Method Invocation** - Call methods dynamically on wrapped objects
- 🧪 **Validation** - Type validation to ensure data integrity

## Installation

### Maven

```xml
<dependency>
    <groupId>org.nth</groupId>
    <artifactId>dyn</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'org.nth:dyn:1.0.0'
```

## Quick Start

Import the static factory method and create your first Dyn object:

```java
import static org.nth.Dyn.$;

// Create a Dyn with a string value
Dyn text = $("Hello World");
System.out.println(text.string()); // Hello World

// Create a Dyn with a numeric value
Dyn number = $(42);
System.out.println(number.intValue()); // 42
```

## Tutorial

### 1. Multiple Value Representations

Dyn objects can hold multiple value representations simultaneously:

```java
Dyn agent = $("James Bond");
agent.set(007);

System.out.println(agent.string());    // James Bond
System.out.println(agent.intValue());  // 7

agent.debug(); // Dyn{primaryType=Integer, values={String=James Bond, Integer=7}, ...}
```

### 2. Basic Usage

Create Dyn objects with different primitive types:

```java
Dyn str = $("Hello");
Dyn num = $(42);
Dyn bool = $(true);

System.out.println(str.string());    // Hello
System.out.println(num.intValue());  // 42
System.out.println(bool.bool());     // true
```

### 3. Type Changing

Add new value representations while preserving original ones:

```java
Dyn str = $("Hello");
str.set(123);

System.out.println(str.intValue()); // 123
System.out.println(str.string());   // Hello (keeps original)
```

### 4. Arithmetic Operations

Perform arithmetic operations directly with Dyn objects:

```java
Dyn x = $(10);
Dyn y = $(3);

System.out.println(x.add(y).intValue());      // 13
System.out.println(x.subtract(y).intValue()); // 7
System.out.println(x.multiply(y).intValue()); // 30
System.out.println(x.divide(y).number());     // 3.3333333333
```

### 5. String Operations

Manipulate strings easily:

```java
Dyn text = $("Hello World");
System.out.println(text.concat($("!")).string());      // Hello World!
System.out.println(text.substring(6, 11).string());    // World
System.out.println(text.toUpperCase().string());       // HELLO WORLD
```

### 6. Collection Operations

Work with lists seamlessly:

```java
Dyn list = Dyn.list(1, 2, 3);
list.addElement(4);
System.out.println(list.list(Integer.class)); // [1, 2, 3, 4]

list.removeElement(Dyn.of(2));
System.out.println(list.list(Integer.class)); // [1, 3, 4]
```

### 7. Map and JSON

Handle maps and JSON with a consistent API:

```java
// Working with maps
Dyn map = Dyn.map("name", "Alice", "age", 30);
System.out.println(map.getJson("name").string());   // Alice
System.out.println(map.getJson("age").intValue());  // 30

// Parse JSON string
String jsonStr = "{\"name\":\"Bob\",\"age\":25}";
Dyn json = Dyn.fromJson(jsonStr);
System.out.println(json.getJson("name").string());  // Bob
System.out.println(json.asJsonString());            // {"name":"Bob","age":25}

// Convert to POJO
Person person = json.fromJson(Person.class);
System.out.println(person.getName());  // Bob
```

### 8. Method Invocation

Call methods dynamically:

```java
Dyn text = $("Hello World");
Dyn length = text.call("length");
System.out.println(length.intValue());  // 11
```

### 9. Exception Handling

Handle exceptions gracefully:

```java
Dyn.of(10).tryCatch(
    d -> System.out.println(d.divide(Dyn.of(0)).number()),
    (d, e) -> System.out.println("Error: " + e.getMessage())
); // Error: Division by zero
```

### 10. Stream Support

Process collections using Java streams:

```java
Dyn numbers = Dyn.list(1, 2, 3, 4, 5);
numbers.stream()
       .map(d -> d.intValue())
       .filter(n -> n % 2 == 0)
       .forEach(System.out::println); // 2 4
```

### 11. Type Conversion

Convert between types seamlessly:

```java
Dyn val = $("123");
System.out.println(val.to(Integer.class)); // 123
System.out.println(val.to(Double.class));  // 123.0
```

### 12. Immutable Dyn

Create immutable Dyn objects:

```java
Dyn immutable = Dyn.immutable("Fixed");
try {
    immutable.set("Changed");
} catch (UnsupportedOperationException e) {
    System.out.println("Immutable: " + e.getMessage()); // Cannot modify immutable Dyn
}
System.out.println(immutable.string()); // Fixed
```

### 13. Null Safety

Handle null values safely:

```java
Dyn nullable = Dyn.optional(null);
System.out.println(nullable.isNullSafe); // true
System.out.println(nullable.string());    // null
```

### 14. Validation

Validate types:

```java
Dyn.of("test").validate(String.class); // No exception

try {
    Dyn.of(123).validate(String.class);
} catch (IllegalStateException e) {
    System.out.println("Validation failed: " + e.getMessage()); 
    // Dyn does not contain a value of type String
}
```

### 15. Type Checking

Check types easily:

```java
System.out.println($("test").isString()); // true
System.out.println($(123).isNumber());    // true
System.out.println(Dyn.list().isList());  // true
```

## Advanced Usage

### Custom Type Conversion

You can register custom type converters to handle specific conversion scenarios:

```java
Dyn.registerConverter(LocalDate.class, String.class, date -> date.format(DateTimeFormatter.ISO_DATE));
Dyn.registerConverter(String.class, LocalDate.class, str -> LocalDate.parse(str, DateTimeFormatter.ISO_DATE));

Dyn date = $(LocalDate.of(2023, 1, 15));
System.out.println(date.string()); // 2023-01-15
```

### Event Listeners

Register listeners to be notified of value changes:

```java
Dyn value = $("initial");
value.addChangeListener((oldVal, newVal) -> 
    System.out.println("Value changed from " + oldVal + " to " + newVal));
value.set("updated"); // Triggers the listener
```

## Best Practices

- Use `$()` for concise object creation
- Prefer method chaining for cleaner code
- Use appropriate getter methods (e.g., `intValue()`, `string()`) for type-safe access
- Use validation when type safety is crucial
- Use `debug()` to inspect complex Dyn objects



## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Project Status

This project is actively maintained and open for contributions. Bug reports, feature requests, and pull requests are welcome!

---

