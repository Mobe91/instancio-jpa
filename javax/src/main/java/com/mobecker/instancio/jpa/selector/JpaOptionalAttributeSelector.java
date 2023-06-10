/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.function.Function;
import java.util.function.Predicate;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;
import org.instancio.internal.nodes.InternalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JpaOptionalAttributeSelector extends PredicateSelectorImpl {
    private static final Logger LOG = LoggerFactory.getLogger(JpaOptionalAttributeSelector.class);

    private static final Function<Metamodel, Predicate<InternalNode>> JPA_OPTIONAL_ATTRIBUTE_PREDICATE
        = metamodel -> node -> {
            InternalNode parent = node.getParent();
            if (parent != null && parent.getTargetClass() != null && node.getField() != null) {
                try {
                    ManagedType<?> managedType = metamodel.managedType(parent.getTargetClass());
                    Attribute<?, ?> attr = managedType.getAttribute(node.getField().getName());
                    return attr.isCollection() || ((SingularAttribute<?, ?>) attr).isOptional();
                } catch (IllegalArgumentException e) {
                    LOG.trace(null, e);
                    return false;
                }
            }
            return false;
        };

    private JpaOptionalAttributeSelector(
        final Predicate<InternalNode> nodePredicate, final String apiInvocationDescription
    ) {
        super(nodePredicate, apiInvocationDescription);
    }


    public static JpaOptionalAttributeSelector jpaOptionalAttribute(Metamodel metamodel) {
        return new JpaOptionalAttributeSelector(
            JPA_OPTIONAL_ATTRIBUTE_PREDICATE.apply(metamodel), "jpaOptionalAttribute()");
    }
}
