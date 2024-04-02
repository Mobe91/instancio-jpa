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

import static com.mobecker.instancio.jpa.util.JpaMetamodelUtil.isHibernateTenantId;
import static com.mobecker.instancio.jpa.util.JpaMetamodelUtil.isInsertable;
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
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starting from an entity root, traverses the entity graph depth-first and recursively prunes associations
 * until it arrives at a persistable entity graph that is maximal relative to the original graph. I.e. it
 * prunes as little as possible but as much as needed. In this context "persistable" means that based on the
 * information provided by the JPA metamodel there are no JPA attribute values that would prevent a successful JPA
 * persist operation on any entity in the graph.
 *
 * @since 1.0.0
 */
public class EntityGraphShrinker {

    private static final Logger LOG = LoggerFactory.getLogger(EntityGraphShrinker.class);

    private final Metamodel metamodel;
    private final Integer stopShrinkingAtDepth;

    /**
     * Create new {@link EntityGraphShrinker}.
     *
     * @param metamodel JPA metamodel
     * @param stopShrinkingAtDepth depth at which to stop the shrinking algorithm. This can be used
     *                             to override the default behavior of traversing the whole object graph.
     */
    public EntityGraphShrinker(Metamodel metamodel, @Nullable Integer stopShrinkingAtDepth) {
        this.metamodel = metamodel;
        this.stopShrinkingAtDepth = stopShrinkingAtDepth;
    }

    /**
     * See {@link EntityGraphShrinker}.
     *
     * @param entity JPA entity
     * @throws NullPointerException if the JPA entity is null
     */
    public void shrink(Object entity) {
        Objects.requireNonNull(entity, "Entity must not be null");
        Set<Object> visited = new HashSet<>();
        shrink0(entity, visited, 0);
        if (!isValid(entity)) {
            throw new RuntimeException("Cannot shrink object graph to a persistable entity graph");
        }
    }

    private void shrink0(Object node, Set<Object> visited, int currentDepth) {
        if (visited.contains(node) || stopShrinkingAtDepth(currentDepth)) {
            return;
        }
        visited.add(node);
        ManagedType<?> managedType = metamodel.managedType(node.getClass());
        managedType.getAttributes().forEach(attr -> {
            Object attrValue = resolveAttributeValue(node, attr);
            if (attrValue == null) {
                return;
            }
            if (attr instanceof SingularAttribute<?, ?> && attr.getPersistentAttributeType() != BASIC) {
                shrink0(attrValue, visited, currentDepth + 1);
                if (!isValid((SingularAttribute<?, ?>) attr, attrValue)) {
                    LOG.debug("Assigning null to {} for node {}", attr, node);
                    setAttributeValue(node, attr, null);
                }
            } else if (attr instanceof PluralAttribute<?, ?, ?>
                && attr.getPersistentAttributeType() != Attribute.PersistentAttributeType.ELEMENT_COLLECTION) {
                PluralAttribute<?, ?, ?> pluralAttribute = (PluralAttribute<?, ?, ?>) attr;
                if (pluralAttribute.getCollectionType() == PluralAttribute.CollectionType.MAP) {
                    Map<?, ?> attrMap = (Map<?, ?>) attrValue;
                    Iterator<?> iterator = attrMap.values().iterator();
                    while (iterator.hasNext()) {
                        Object attrMapValue = iterator.next();
                        shrink0(attrMapValue, visited, currentDepth + 1);
                        if (!isValid(attrMapValue)) {
                            LOG.debug("Removing value {} from map {} at node {}", attrMapValue, attrMap, node);
                            iterator.remove();
                        }
                    }
                } else if (pluralAttribute.getElementType().getPersistenceType() == ENTITY
                    || pluralAttribute.getElementType().getPersistenceType() == EMBEDDABLE) {
                    Collection<?> attrCollection = (Collection<?>) attrValue;
                    Iterator<?> iterator = attrCollection.iterator();
                    while (iterator.hasNext()) {
                        Object attrCollectionElement = iterator.next();
                        shrink0(attrCollectionElement, visited, currentDepth + 1);
                        if (!isValid(attrCollectionElement)) {
                            LOG.debug("Removing element {} from collection {} at node {}",
                                attrCollectionElement, pluralAttribute, node);
                            iterator.remove();
                        }
                    }
                } else if (pluralAttribute.getElementType().getPersistenceType() == MAPPED_SUPERCLASS) {
                    throw new IllegalStateException("Unexpected persistence type '" + MAPPED_SUPERCLASS + "'.");
                }
            }

        });
        visited.remove(node);
    }

    private boolean isValid(SingularAttribute<?, ?> attribute, Object attributeValue) {
        if (attribute.getPersistentAttributeType() != BASIC && attributeValue != null) {
            return isValid(attributeValue);
        }

        return isValueValidForSingularAttribute(attribute, attributeValue);
    }

    private boolean isValid(Object node) {
        ManagedType<?> managedType = metamodel.managedType(node.getClass());
        return managedType.getAttributes().stream()
            .filter(attr -> attr instanceof SingularAttribute<?, ?>)
            .map(SingularAttribute.class::cast)
            .allMatch(attr -> {
                Object attrValue = resolveAttributeValue(node, attr);
                return isValid0(attr, attrValue);
            });
    }

    private static boolean isValid0(SingularAttribute<?, ?> attribute, Object attributeValue) {
        if (attribute.getPersistentAttributeType() != BASIC && attributeValue != null) {
            // We return true here and do not perform a deep validity check on the attribute value. This is
            // sufficient because the shrinking algorithm works backwards from the "leaves" of the graph to the
            // root and checks the validity of attribute values at each level. So the validity of a non-null
            // attributeValue will already have been checked at this point.
            return true;
        }

        return isValueValidForSingularAttribute(attribute, attributeValue);
    }

    private static boolean isValueValidForSingularAttribute(SingularAttribute<?, ?> attribute, Object attributeValue) {
        return attributeValue != null
            || attribute.isId()
            || attribute.isOptional()
            || !isInsertable(attribute)
            || isHibernateTenantId(attribute);
    }

    private boolean stopShrinkingAtDepth(int depth) {
        return stopShrinkingAtDepth != null && depth >= stopShrinkingAtDepth;
    }
}
