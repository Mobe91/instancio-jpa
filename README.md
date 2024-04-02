# What is it?

Instancio-jpa is an extension on top of the [Instancio](https://github.com/instancio/instancio) library that enables 
the creation and population of JPA entities including the subsequent persistence of these entities using JPA for the 
purpose of test data generation in integration tests. The extension uses the JPA metamodel to yield a persistable 
object graph. It further provides JPA based utilities that allows for straight forward persistence of the object graph.

# Maven coordinates

To use the extension in your tests, include one of the following dependency depending on whether your project uses
the javax or the jakarta namespace.

```xml
<properties>
    <version.instancio-jpa>1.0.1</version.instancio-jpa>
</properties>
```

For javax:
```xml
<dependency>
    <groupId>com.mobecker</groupId>
    <artifactId>instancio-jpa-javax</artifactId>
    <version>${version.instancio-jpa}</version>
    <scope>test</scope>
</dependency>
```

For jakarta:
```xml
<dependency>
    <groupId>com.mobecker</groupId>
    <artifactId>instancio-jpa-jakarta</artifactId>
    <version>${version.instancio-jpa}</version>
    <scope>test</scope>
</dependency>
```

# Getting started

The core of the extension is the static `jpaModel` method that accepts an entity class and a JPA metamodel instance, and
returns an `org.instancio.Model` that is usable with the Instancio APIs.

The extension can be used like this:

```java
EntityManager entityManager = /* Retrieve EntityManager. */;
EntityGraphPersister persister = new EntityGraphPersister(entityManager);
Model<MyEntity> myEntityModel = jpaModel(MyEntity.class, em.getMetamodel()).build();
// Use normal Instancio API in next line to shape how the objects are populated.
MyEntity myEntity = Instancio.of(myEntityModel).create();
// Persist the created object graph using the provided EntityGraphPersister.
persister.persist(myEntity);
```

# Instancio compatibility

No compatibility testing between releases of Instancio and instancio-jpa are currently performed. Users are 
recommended to stick to the Instancio dependency version as declared in the root pom.xml of instancio-jpa for a 
particular instance-jpa release.

# Configuration

Just like Instancio, instancio-jpa supports programmatic and properties file based configuration.

For the programmatic configuration combine Instancio native settings with instancio-jpa configuration keys:
```java
Model<Cat> catModel = jpaModel(Cat.class, entityManager.getMetamodel()).withSettings(Settings.defaults()
        .set(Keys.ASSIGNMENT_TYPE, AssignmentType.METHOD)
        .set(JpaKeys.USE_JPA_NULLABILITY, false))
    .build();
Cat cat = Instancio.of(catModel);
```

For the properties file based configuration, add instancio-jpa specific configuration keys to the standard
`instancio.properties`.
```properties
assignment.type=METHOD
jpa.useNullability=false
```

See `com.mobecker.instancio.jpa.setting.JpaKeys` for available configurations.

# Limitations

## Overriding Instancio nullability 

By default, instancio-jpa configures Instancio model fields to be nullable according to the JPA metamodel. That means
if an entity attribute is nullable / optional the Instancio model will also regard it as nullable and the field will
occasionally be `null`.

Instancio currently does not allow to override the nullability of fields defined by the model.

Example:
```java
@Entity
public class Cat {
    @Id
    private Long id;
    private String name; // nullable
    
    // getters and setters skipped
}

Model<Cat> catModel = jpaModel(Cat.class, jpaMetamodel).build();
Cat cat = Instancio.of(catModel)
    .set(fields(Cat::getName), "harry")
    .create();
// cat.getName() may still be null!
```

In above example, even though `set(fields(Cat::getName), "harry")` is used to set the `name` field to a constant 
non-null value, Instancio will still set the field to `null` at times. This is because the field has been marked as 
nullable by the `jpaModel` method.

As a workaround you can set the configuration option `USE_JPA_NULLABILITY` to `false`:
```java
Model<Cat> catModel = jpaModel(Cat.class, jpaMetamodel)
    .withSettings(Settings.defaults().set(JpaKeys.USE_JPA_NULLABILITY, false))
    .build();
```

## Interoperability with Instancio's bean validation support

Instancio has [experimental support for bean validation](https://www.instancio.org/user-guide/#bean-validation). While
it is possible to enable Instancio's bean validation support users must be aware that the 
`org.instancio.spi.InstancioServiceProvider.GeneratorProvider`s contributed by instancio-jpa will take precedence over
Instancio's bean validation handling. This can result in bean validation annotations not being considered by Instancio
in certain situations. For example:

```
@Entity
public class Cat {
    @Id
    private Long id;
    @Size(min = 8, max = 50)
    @Column(length = 50)
    private String name;
}
```

In above example the column length handling of instancio-jpa will take precedence and the `@Size` annotation will be
disregarded. This can result in names being generated that are shorter than 8 characters. Users may use the following
workarounds in these situations:

* Provide a custom `org.instancio.spi.InstancioServiceProvider.GeneratorProvider` to override instancio-jpa and to
  manually generated valid values for a property.
* Use the `jpa.generatorProviderExclusions` configuration key supported by instancio-jpa to exclude a specific 
  properties from being handled by instancio-jpa.

  `instancio.properties`.
  ```properties
  jpa.generatorProviderExclusions=fqn.Cat#name,\
    fqn.Book#author
  ```


## Using Instancio onComplete callbacks

Instancio-jpa uses Instancio's onComplete callback in combination with the `org.instancio.Select.root()` TargetSelector 
as a hook to add custom logic. 
Since Instancio can only register a single onComplete callback per TargetSelector at the moment the use of Instancio's 
native onComplete callbacks for the `org.instancio.Select.root()` TargetSelector in combination with instancio-jpa is 
not supported.
```java
Model<Cat> catModel = jpaModel(Cat.class, jpaMetamodel).build();
Cat cat = Instancio.of(catModel)
    .onComplete(root(), cat -> /* do some cat action */) // This is currently not supported!
    .create();
```
As a workaround you can register an onComplete callback on the builder returned by the `jpaModel` method.
```java
Model<Cat> catModel = jpaModel(Cat.class, jpaMetamodel)
    .onComplete(cat -> /* do some cat action */) // This works
    .build();
Cat cat = Instancio.of(catModel)
    .create();
```

## Using Instancio maxDepth

Instancio-jpa's `jpaModel` builder provides a way to manually specify the maxDepth of the entity graph that will be
generated.

```java
Model<Cat> catModel = jpaModel(Cat.class, jpaMetamodel).withMaxDepth(4).build();
```

If no maxDepth is specified instancio-jpa will attempt to detect the minimum depth required to yield a persistable 
entity graph and will use this value as maxDepth.

While this API is similar to Instancio's `withMaxDepth` it must not be confused. The usage of Instancio's native
`withMaxDepth` in conjunction with instancio-jpa is not supported. In consequence  this means that overriding the 
maxDepth from an instancio-jpa created Instancio `org.instancio.Model` is also not supported.

## Correctness of the JPA metamodel

The JPA metamodel implementation of Hibernate turns out to be buggy and indeterministic when it comes to the nullability
of fields in mapped superclasses and embeddables. Therefore, the current version of instancio-jpa ignores the 
provided nullability information in these contexts. This leads to non-minimal object graphs being created in such cases.

## Supported JPA providers

Instancio-jpa is tested with Hibernate 5.6.x and Hibernate 6.2.x. However, there are no provider specific dependencies, 
and therefore it might also work with other providers.

# Feedback

Feedback and bug reports are greatly appreciated. Please submit an 
[issue](https://github.com/Mobe91/instancio-jpa/issues) to report a bug, or if you have a question or a suggestion.
