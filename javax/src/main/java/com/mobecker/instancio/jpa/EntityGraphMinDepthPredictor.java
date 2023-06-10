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

package com.mobecker.instancio.jpa;

import com.mobecker.instancio.jpa.util.JpaProviderVersionUtil;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

public class EntityGraphMinDepthPredictor {

    private final Metamodel metamodel;

    public EntityGraphMinDepthPredictor(Metamodel metamodel) {
        this.metamodel = metamodel;
    }

    public int predictRequiredDepth(Class<?> entityClass) {
        Set<Class<?>> visited = new HashSet<>();
        return predictRequiredMaxDepth0(entityClass, visited);
    }

    private int predictRequiredMaxDepth0(Class<?> entityClass, Set<Class<?>> visited) {
        if (visited.contains(entityClass)) {
            return 0;
        }
        visited.add(entityClass);

        ManagedType<?> managedType = metamodel.managedType(entityClass);
        int maxDepth = managedType.getAttributes().stream()
            .filter(attr -> ignoreAttributeNullability(managedType)
                || attr instanceof SingularAttribute && !((SingularAttribute<?, ?>) attr).isOptional())
            .mapToInt(attr -> {
                int depth;
                switch (attr.getPersistentAttributeType()) {
                    case ONE_TO_ONE:
                    case MANY_TO_ONE:
                        depth = 1 + predictRequiredMaxDepth0(attr.getJavaType(), visited);
                        break;
                    case EMBEDDED:
                        depth = 1 + predictRequiredMaxDepth0(attr.getJavaType(), visited);
                        break;
                    case BASIC:
                        depth = 1;
                        break;
                    default:
                        depth = 0;
                }
                return depth;
            }).max().orElse(0);

        visited.remove(entityClass);
        return maxDepth;
    }

    private static boolean ignoreAttributeNullability(ManagedType<?> attributeContainer) {
        return JpaProviderVersionUtil.isHibernate5OrOlder()
            // The nullability information for attributes in embeddables is not always correct
            // (at least for Hibernate 5)
            && (attributeContainer.getPersistenceType() == Type.PersistenceType.EMBEDDABLE
            // Hibernate 5's metamodel is buggy / indeterministic when it comes to attributes of mapped superclasses:
            //
            // Assuming I have a MappedSuperclass A with a nullable attribute p0, and two entities B and C that
            // inherit from it.
            // B overrides the nullability of p0 using @AttributeOverride with nullable=false.
            // C does not override the nullability, therefore effectively nullable=true.
            // In this case, it depends on the order in which B and C are processed during the construction of the
            // metamodel whether metamodel.managedType(A.class).getAttribute("p0").isOptional() returns true or false.
            || attributeContainer.getPersistenceType() == Type.PersistenceType.MAPPED_SUPERCLASS);
    }
}
