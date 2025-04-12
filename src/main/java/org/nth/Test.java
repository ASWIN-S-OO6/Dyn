package org.nth;

import java.util.List;

import static org.nth.Dyn.$;

public class Test {
    public static void main(String[] args) {
        // 1. Multiple Value Representations
        Dyn agent = $("James Bond");
        agent.set(007);
        System.out.println(agent.string()); // James Bond
        System.out.println(agent.intValue()); // 007
        agent.debug(); // Dyn{primaryType=Integer, values={String=James Bond, Integer=7}, ...}

        // 2. Basic Usage
        Dyn str = $("Hello");
        Dyn num = $(42);
        Dyn bool = $(true);

        System.out.println(str.string()); // Hello
        System.out.println(num.intValue()); // 42
        System.out.println(bool.bool()); // true

        // 3. Type Changing
        str.set(123);
        System.out.println(str.intValue()); // 123
        System.out.println(str.string()); // Hello (keeps original)

        // 4. Arithmetic Operations
        Dyn x = $(10);
        Dyn y = $(3);
        System.out.println(x.add(y).intValue()); // 13
        System.out.println(x.subtract(y).intValue()); // 7
        System.out.println(x.multiply(y).intValue()); // 30
        System.out.println(x.divide(y).number()); // 3.3333333333

        // 5. String Operations
        Dyn text = $("Hello World");
        System.out.println(text.concat($("!")).string()); // Hello World!
        System.out.println(text.substring(6, 11).string()); // World
        System.out.println(text.toUpperCase().string()); // HELLO WORLD

        // 6. Collection Operations
        Dyn list = Dyn.list(1, 2, 3);
        list.addElement(4);
        System.out.println(list.list(Integer.class)); // [1, 2, 3, 4]
        list.removeElement(Dyn.of(2));
        System.out.println(list.list(Integer.class)); // [1, 3, 4]

        // 7. Map and JSON
        Dyn map = Dyn.map("name", "Alice", "age", 30);
        System.out.println(map.getJson("name").string()); // Alice
        System.out.println(map.getJson("age").intValue()); // 30

        String jsonStr = "{\"name\":\"Bob\",\"age\":25}";
        Dyn json = Dyn.fromJson(jsonStr);
        System.out.println(json.getJson("name").string()); // Bob
        System.out.println(json.asJsonString()); // {"name":"Bob","age":25}



        Person person = json.fromJson(Person.class);
        System.out.println(person.getName()); // Bob

        // 8. Method Invocation
        Dyn text2 = $("Hello World");
        Dyn length = text2.call("length");
        System.out.println(length.intValue()); // 11

        // 9. Exception Handling
        Dyn.of(10).tryCatch(
                d -> System.out.println(d.divide(Dyn.of(0)).number()),
                (d, e) -> System.out.println("Error: " + e.getMessage())
        ); // Error: Division by zero

        // 10. Stream Support
        Dyn numbers = Dyn.list(1, 2, 3, 4, 5);
        numbers.stream()
                .map(d -> d.intValue())
                .filter(n -> n % 2 == 0)
                .forEach(System.out::println); // 2 4

        // 11. Type Conversion
        Dyn val = $("123");
        System.out.println(val.to(Integer.class)); // 123
        System.out.println(val.to(Double.class)); // 123.0

        // 12. Immutable Dyn
        Dyn immutable = Dyn.immutable("Fixed");
        try {
            immutable.set("Changed");
        } catch (UnsupportedOperationException e) {
            System.out.println("Immutable: " + e.getMessage()); // Cannot modify immutable Dyn
        }
        System.out.println(immutable.string()); // Fixed

        // 13. Null Safety
        Dyn nullable = Dyn.optional(null);
        System.out.println(nullable.isNullSafe); // true
        System.out.println(nullable.string()); // null

        // 14. Validation
        Dyn.of("test").validate(String.class);
        try {
            Dyn.of(123).validate(String.class);
        } catch (IllegalStateException e) {
            System.out.println("Validation failed: " + e.getMessage()); // Dyn does not contain a value of type String
        }

        // 15. Type Checking
        System.out.println($("test").isString()); // true
        System.out.println($(123).isNumber()); // true
        System.out.println(Dyn.list().isList()); // true
    }
    class Person {
        private String name;
        private int age;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
}