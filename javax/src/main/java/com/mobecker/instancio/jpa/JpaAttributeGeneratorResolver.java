/*
 * Copyright 2023 - 2024 Moritz Becker.
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

import java.util.Map;
import javax.annotation.Nullable;
import javax.persistence.metamodel.Attribute;
import org.instancio.Node;
import org.instancio.generator.Generator;
import org.instancio.generator.GeneratorSpec;
import org.instancio.generators.Generators;

/**
 * Resolves an applicable {@link GeneratorSpec} for a given JPA attribute.
 *
 * @since 1.1.0
 */
public interface JpaAttributeGeneratorResolver {

    /**
     * Resolves a {@link GeneratorSpec} that should be used by Instancio for generating values for the given
     * JPA attribute.
     *
     * @param node the instancio node representing the JPA attribute.
     * @param generators provides access to Instancio built-in generators.
     * @param attribute the JPA attribute to resolve a {@link GeneratorSpec} for.
     * @param context the {@link GeneratorResolverContext}.
     * @return a {@link GeneratorSpec} or {@code null} if this resolver cannot resolve a {@link GeneratorSpec}.
     */
    @Nullable
    GeneratorSpec<?> getGenerator(
        Node node, Generators generators, Attribute<?, ?> attribute, GeneratorResolverContext context);

    /**
     * Provides access to previously returned generators in the same invocation context.
     * This is required for stateful generators like
     * {@link org.instancio.internal.generator.sequence.LongSequenceGenerator} where the same instance needs to be
     * reused across invocations for the same Instancio node.
     */
    interface GeneratorResolverContext {

        /**
         * Provides access to a {@link Map} that associates Instancio nodes with generators that have previously
         * been created for these nodes.
         *
         * @return a modifiable {@link Map} associating Instancio nodes with generators. The caller may use the
         *     {@link Map} to add new associations.
         */
        Map<Node, Generator<?>> getContextualGenerators();
    }
}
