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

import com.blazebit.reflection.ReflectionUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.persistence.Transient;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import org.instancio.internal.nodes.InternalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JpaTransientAttributeSelectorBuilder extends PredicateSelectorImpl {
    private static final Logger LOG = LoggerFactory.getLogger(JpaTransientAttributeSelectorBuilder.class);

    private static final Function<Metamodel, Predicate<InternalNode>> JPA_TRANSIENT_PREDICATE = metamodel -> node -> {
        InternalNode parent = node.getParent();
        if (parent != null && parent.getTargetClass() != null && node.getField() != null) {
            try {
                ManagedType<?> managedType = metamodel.managedType(parent.getTargetClass());
                return node.getField().getAnnotation(Transient.class) != null
                    || hasTransientGetter(managedType, node.getField());
            } catch (IllegalArgumentException e) {
                LOG.trace(null, e);
                return false;
            }
        }
        return false;
    };

    private JpaTransientAttributeSelectorBuilder(Predicate<InternalNode> nodePredicate,
                                                   String apiInvocationDescription) {
        super(nodePredicate, apiInvocationDescription);
    }

    private static boolean hasTransientGetter(ManagedType<?> managedType, Field field) {
        Method getter = ReflectionUtils.getGetter(managedType.getJavaType(), field.getName());
        return getter != null && getter.getAnnotation(Transient.class) != null;
    }

    public static JpaTransientAttributeSelectorBuilder jpaTransient(Metamodel metamodel) {
        return new JpaTransientAttributeSelectorBuilder(JPA_TRANSIENT_PREDICATE.apply(metamodel), "jpaTransient()");
    }
}
