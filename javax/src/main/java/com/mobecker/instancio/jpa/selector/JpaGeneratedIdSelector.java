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

package com.mobecker.instancio.jpa.selector;

import static com.mobecker.instancio.jpa.util.JpaMetamodelUtil.getAnnotation;
import static com.mobecker.instancio.jpa.util.JpaMetamodelUtil.resolveIdAttribute;

import java.util.function.Function;
import java.util.function.Predicate;
import javax.persistence.GeneratedValue;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;
import org.instancio.internal.nodes.InternalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Instancio selector that selects JPA generated id attributes.
 *
 * @since 1.0.0
 */
public final class JpaGeneratedIdSelector extends PredicateSelectorImpl {
    private static final Logger LOG = LoggerFactory.getLogger(JpaGeneratedIdSelector.class);

    private static final Function<Metamodel, Predicate<InternalNode>> JPA_GENERATED_ID_PREDICATE
        = metamodel -> node -> {
            InternalNode parent = node.getParent();
            if (parent != null && parent.getTargetClass() != null && node.getField() != null) {
                try {
                    EntityType<?> entityType = metamodel.entity(parent.getTargetClass());
                    SingularAttribute<?, ?> idAttr = resolveIdAttribute(entityType, node.getField().getName());
                    return idAttr != null && getAnnotation(idAttr, GeneratedValue.class) != null;
                } catch (IllegalArgumentException e) {
                    LOG.trace(null, e);
                    return false;
                }
            }
            return false;
        };

    private JpaGeneratedIdSelector(final Predicate<InternalNode> nodePredicate, final String apiInvocationDescription) {
        super(nodePredicate, apiInvocationDescription);
    }

    /**
     * Creates new {@link JpaGeneratedIdSelector}.
     *
     * @param metamodel JPA metamodel
     * @return selector that selects JPA generated id attributes
     * @see JpaGeneratedIdSelector
     */
    public static JpaGeneratedIdSelector jpaGeneratedId(Metamodel metamodel) {
        return new JpaGeneratedIdSelector(JPA_GENERATED_ID_PREDICATE.apply(metamodel), "jpaGeneratedId()");
    }
}
