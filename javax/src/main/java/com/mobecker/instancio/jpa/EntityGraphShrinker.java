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

import static com.mobecker.instancio.jpa.util.JpaMetamodelUtil.resolveAttributeValue;
import static com.mobecker.instancio.jpa.util.JpaMetamodelUtil.setAttributeValue;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.BASIC;
import static javax.persistence.metamodel.Type.PersistenceType.EMBEDDABLE;
import static javax.persistence.metamodel.Type.PersistenceType.ENTITY;
import static javax.persistence.metamodel.Type.PersistenceType.MAPPED_SUPERCLASS;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

public class EntityGraphShrinker {

    private final Metamodel metamodel;

    public EntityGraphShrinker(Metamodel metamodel) {
        this.metamodel = metamodel;
    }

    public void shrink(Object entity) {
        Set<Object> visited = new HashSet<>();
        shrink0(entity, visited);
        if (!isValid(entity)) {
            throw new RuntimeException("Cannot shrink object graph to a persistable entity graph");
        }
    }

    private void shrink0(Object node, Set<Object> visited) {
        if (node == null || visited.contains(node)) {
            return;
        }
        visited.add(node);
        ManagedType<?> managedType = metamodel.managedType(node.getClass());
        managedType.getAttributes().forEach(attr -> {
            Object attrValue = resolveAttributeValue(node, attr);
            if (attr instanceof SingularAttribute<?, ?> && attr.getPersistentAttributeType() != BASIC) {
                shrink0(attrValue, visited);
                if (attrValue != null && !isValid((SingularAttribute<?, ?>) attr, attrValue)) {
                    setAttributeValue(node, attr, null);
                }
            } else if (attr instanceof PluralAttribute<?, ?, ?>) {
                PluralAttribute<?, ?, ?> pluralAttribute = (PluralAttribute<?, ?, ?>) attr;
                if (pluralAttribute.getCollectionType() == PluralAttribute.CollectionType.MAP) {
                    Map<?, ?> attrMap = (Map<?, ?>) attrValue;
                    if (attrMap != null) {
                        Iterator<?> iterator = attrMap.values().iterator();
                        while (iterator.hasNext()) {
                            Object attrMapValue = iterator.next();
                            shrink0(attrMapValue, visited);
                            if (!isValid(attrMapValue)) {
                                iterator.remove();
                            }
                        }
                    }
                } else if (pluralAttribute.getElementType().getPersistenceType() == ENTITY
                    || pluralAttribute.getElementType().getPersistenceType() == EMBEDDABLE) {
                    Collection<?> attrCollection = (Collection<?>) attrValue;
                    if (attrCollection != null) {
                        Iterator<?> iterator = attrCollection.iterator();
                        while (iterator.hasNext()) {
                            Object attrCollectionElement = iterator.next();
                            shrink0(attrCollectionElement, visited);
                            if (!isValid(attrCollectionElement)) {
                                iterator.remove();
                            }
                        }
                    }
                } else if (pluralAttribute.getElementType().getPersistenceType() == MAPPED_SUPERCLASS) {
                    throw new IllegalStateException("Unexpected persistence type '" + MAPPED_SUPERCLASS + "'.");
                }
            }

        });
        visited.remove(node);
    }

    // TODO: we could maybe optimize isValid to not always traverse the whole tree
    private boolean isValid(SingularAttribute<?, ?> attribute, Object attributeValue) {
        if (attribute.getPersistentAttributeType() != BASIC && attributeValue != null) {
            return isValid(attributeValue);
        }
        return attribute.isOptional();
    }

    private boolean isValid(Object node) {
        Set<Object> visited = new HashSet<>();
        return isValid0(node, visited);
    }

    private boolean isValid0(SingularAttribute<?, ?> attribute, Object attributeValue, Set<Object> visited) {
        if (attribute.getPersistentAttributeType() != BASIC && attributeValue != null) {
            if (visited.contains(attributeValue)) {
                return true;
            }
            visited.add(attributeValue);
            boolean isValid = isValid0(attributeValue, visited);
            visited.remove(attributeValue);
            return isValid;
        }
        return attributeValue != null || attribute.isId() || attribute.isOptional();
    }

    private boolean isValid0(Object node, Set<Object> visited) {
        if (visited.contains(node)) {
            return true;
        }
        visited.add(node);
        ManagedType<?> managedType = metamodel.managedType(node.getClass());
        boolean isValid = managedType.getAttributes().stream().allMatch(attr -> {
            Object attrValue = resolveAttributeValue(node, attr);
            if (attr instanceof SingularAttribute<?, ?>) {
                return isValid0((SingularAttribute<?, ?>) attr, attrValue, visited);
            } else {
                return true;
            }
        });
        visited.remove(node);
        return isValid;
    }
}
