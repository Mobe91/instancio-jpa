/*
 * Copyright 2023 - 2023 Moritz Becker.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobecker.instancio.jpa;

import static com.mobecker.instancio.jpa.util.JpaMetamodelUtil.getAnnotation;
import static com.mobecker.instancio.jpa.util.JpaMetamodelUtil.resolveIdAttribute;

import com.mobecker.instancio.jpa.generator.StringSequenceGenerator;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SingularAttribute;
import org.instancio.Node;
import org.instancio.generator.Generator;
import org.instancio.generators.Generators;
import org.instancio.internal.generator.sequence.IntegerSequenceGenerator;
import org.instancio.internal.generator.sequence.LongSequenceGenerator;

/**
 * Resolves applicable id generators for a JPA attribute.
 *
 * @since 1.1.0
 */
public class UniqueValueGeneratorResolver implements JpaAttributeGeneratorResolver {

    private static final Map<Class<?>, Class<? extends Generator<?>>> SEQ_GENERATORS;

    static {
        Map<Class<?>, Class<? extends Generator<?>>> sequenceGenerators = new HashMap<>(1);
        sequenceGenerators.put(Long.class, LongSequenceGenerator.class);
        sequenceGenerators.put(Integer.class, IntegerSequenceGenerator.class);
        sequenceGenerators.put(String.class, StringSequenceGenerator.class);
        SEQ_GENERATORS = Collections.unmodifiableMap(sequenceGenerators);
    }

    @Override
    public Generator<?> getGenerator(
        Node node, Generators generators, Attribute<?, ?> attribute, GeneratorResolverContext context) {
        if (isBasicUnique(attribute) || isNonGeneratedIdAttribute(attribute)) {
            return resolveIdGenerator(context.getContextualGenerators(), node, attribute.getJavaType());
        }
        return null;
    }

    private static boolean isBasicUnique(Attribute<?, ?> attribute) {
        if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC) {
            Column column = getAnnotation(attribute, Column.class);
            return column != null && column.unique();
        }
        return false;
    }

    private static boolean isNonGeneratedIdAttribute(Attribute<?, ?> attribute) {
        ManagedType<?> declaringType = attribute.getDeclaringType();
        if (declaringType instanceof IdentifiableType) {
            SingularAttribute<?, ?> idAttr = resolveIdAttribute(
                (IdentifiableType<?>) declaringType, attribute.getName());
            return Objects.equals(attribute, idAttr) && getAnnotation(idAttr, GeneratedValue.class) == null;
        }
        return false;
    }

    private Generator<?> resolveIdGenerator(
        Map<Node, Generator<?>> contextualGenerators, Node node, Class<?> fieldType
    ) {
        Generator<?> generator = contextualGenerators.get(node);
        if (generator != null) {
            return generator;
        }
        generator = instantiateIdGenerator(fieldType);
        if (generator != null) {
            contextualGenerators.put(node, generator);
        }
        return generator;
    }

    private static Generator<?> instantiateIdGenerator(Class<?> fieldType) {
        Class<?> generatorClass = SEQ_GENERATORS.get(fieldType);
        if (generatorClass != null) {
            try {
                Constructor<?> constructor = generatorClass.getConstructor();
                return (Generator<?>) constructor.newInstance();
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
                     | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
