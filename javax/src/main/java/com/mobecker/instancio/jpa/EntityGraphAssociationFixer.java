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


import static com.mobecker.instancio.jpa.util.JpaMetamodelUtil.resolveAttributeValue;
import static com.mobecker.instancio.jpa.util.JpaMetamodelUtil.resolveMappedBy;
import static com.mobecker.instancio.jpa.util.JpaMetamodelUtil.setAttributeValue;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.MANY_TO_MANY;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.MANY_TO_ONE;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.ONE_TO_MANY;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.ONE_TO_ONE;

import com.mobecker.instancio.jpa.util.JpaMetamodelUtil;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.JoinColumn;
import javax.persistence.MapKey;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starting from an entity root, traverses the "wild" entity graph and fixes all associations in the graph to yield
 * a "sound" entity graph. For example, it alters the owning attribute of a @OneToMany/@ManyToOne association to point
 * back to the container of the @OneToMany association.
 *
 * @since 1.0.0
 */
public class EntityGraphAssociationFixer {

    private static final Logger LOG = LoggerFactory.getLogger(EntityGraphAssociationFixer.class);
    private final Metamodel metamodel;

    /**
     * Create new {@link EntityGraphAssociationFixer}.
     *
     * @param metamodel JPA metamodel
     */
    public EntityGraphAssociationFixer(Metamodel metamodel) {
        this.metamodel = metamodel;
    }

    /**
     * See {@link EntityGraphAssociationFixer}.
     *
     * @param entity JPA entity
     */
    public void fixAssociations(Object entity) {
        Set<Object> visited = new HashSet<>();
        fixAssociations0(entity, visited);
    }

    private void fixAssociations0(Object entity, Set<Object> visited) {
        if (visited.contains(entity)) {
            return;
        }
        visited.add(entity);
        EntityType<?> entityType = metamodel.entity(entity.getClass());
        entityType.getAttributes().stream()
            .filter(Attribute::isAssociation)
            .forEach(attr -> {
                LOG.trace("Process attribute {} of entity {}", attr, entity);
                if (attr.getPersistentAttributeType() == MANY_TO_ONE) {
                    // we need to add the "entity" to the corresponding OneToMany
                    fixManyToOneAssociation(entity, (SingularAttribute<?, ?>) attr);
                } else if (attr.getPersistentAttributeType() == ONE_TO_ONE) {
                    // we need to point the other association side to "entity"
                    fixOneToOneAssociation(entity, attr);
                } else if (attr.getPersistentAttributeType() == ONE_TO_MANY) {
                    // we need to point other association side to "entity"
                    fixOneToManyAssociation(entity, (PluralAttribute<?, ?, ?>) attr);
                } else if (attr.getPersistentAttributeType() == MANY_TO_MANY) {
                    // we need to add the "entity" to all the corresponding ManyToMany
                    fixManyToManyAssociation(entity, (PluralAttribute<?, ?, ?>) attr);
                } else {
                    throw new IllegalStateException("Unknown persistent attribute type '"
                        + attr.getPersistentAttributeType() + "'.");
                }
                Object attributeValue = resolveAttributeValue(entity, attr);
                if (attributeValue != null) {
                    if (attr.isCollection()) {
                        PluralAttribute<?, ?, ?> pluralAttr = (PluralAttribute<?, ?, ?>) attr;
                        if (pluralAttr.getCollectionType() == PluralAttribute.CollectionType.MAP) {
                            ((Map<?, ?>) attributeValue).forEach((key, value) -> fixAssociations0(value, visited));
                        } else {
                            ((Collection<?>) attributeValue).forEach(collectionElement
                                -> fixAssociations0(collectionElement, visited));
                        }
                    } else {
                        fixAssociations0(attributeValue, visited);
                    }
                }
            });
        visited.remove(entity);
    }

    private <X, Y> void fixManyToOneAssociation(Object associationStartValue, SingularAttribute<X, Y> manyToOneAttr) {
        Object associationEndValue = resolveAttributeValue(associationStartValue, manyToOneAttr);
        if (associationEndValue != null) {
            EntityType<Y> associationEndType = metamodel.entity(manyToOneAttr.getJavaType());
            findReflectiveAttributesForManyToOne(associationEndType, manyToOneAttr)
                .forEach(associationEnd -> {
                    populateCollectionOrMap(associationEndValue, associationEnd, associationStartValue);
                });
        }
    }

    private <X, Y> void fixOneToOneAssociation(Object associationStartValue, Attribute<X, Y> associationStart) {
        Object associationEndValue = resolveAttributeValue(associationStartValue, associationStart);
        if (associationEndValue != null) {
            String mappedByOnStartSide = resolveMappedBy(associationStart.getJavaMember());
            EntityType<Y> associationEndType =
                metamodel.entity(associationStart.getJavaType());
            if (mappedByOnStartSide != null) {
                findOneToOneWithAttributeName(associationEndType, mappedByOnStartSide)
                    .ifPresent(associationEnd ->
                        setAttributeValue(associationEndValue, associationEnd, associationStartValue));
            } else {
                findOneToOneWithMappedBy(associationEndType, associationStart.getName())
                    .forEach(associationEnd ->
                        setAttributeValue(associationEndValue, associationEnd, associationStartValue));
            }
        }
    }

    private <X, Y, E> void fixOneToManyAssociation(
        Object associationStartValue, PluralAttribute<X, Y, E> associationStart
    ) {
        if (associationStart.getCollectionType() == PluralAttribute.CollectionType.MAP) {
            Map<?, ?> associationEndMap = (Map<?, ?>) resolveAttributeValue(associationStartValue, associationStart);
            if (associationEndMap != null) {
                String mappedByOnStartSide = resolveMappedBy(associationStart.getJavaMember());
                if (mappedByOnStartSide != null) {
                    LOG.trace("Fixing oneToMany for owned side map attribute {} in entity {}",
                        associationStart,
                        associationStartValue);
                    EntityType<E> associationEndType = metamodel.entity(
                        associationStart.getElementType().getJavaType());
                    findManyToOneWithAttributeName(associationEndType, mappedByOnStartSide)
                        .ifPresent(associationEnd ->
                            associationEndMap.forEach((associationEndElementKey, associationEndElementValue) ->
                                setAttributeValue(associationEndElementValue, associationEnd, associationStartValue)));
                }
            }
        } else {
            Collection<?> associationEndCollection = (Collection<?>) resolveAttributeValue(
                associationStartValue, associationStart);
            if (associationEndCollection != null) {
                String mappedByOnStartSide = resolveMappedBy(associationStart.getJavaMember());
                if (mappedByOnStartSide != null) {
                    LOG.trace("Fixing oneToMany for owned side collection attribute {} in entity {}",
                        associationStart,
                        associationStartValue);
                    EntityType<E> associationEndType = metamodel.entity(
                        associationStart.getElementType().getJavaType());
                    findManyToOneWithAttributeName(associationEndType, mappedByOnStartSide)
                        .ifPresent(associationEnd ->
                            associationEndCollection.forEach(associationEndElementValue ->
                                setAttributeValue(associationEndElementValue, associationEnd, associationStartValue)));
                }
            }
        }
    }

    private <X, Y, E> void fixManyToManyAssociation(
        Object associationStartValue, PluralAttribute<X, Y, E> associationStart
    ) {
        if (associationStart.getCollectionType() == PluralAttribute.CollectionType.MAP) {
            Map<?, ?> associationEndMap = (Map<?, ?>) resolveAttributeValue(associationStartValue, associationStart);
            if (associationEndMap != null) {
                String mappedByOnStartSide = resolveMappedBy(associationStart.getJavaMember());
                EntityType<E> associationEndType = metamodel.entity(associationStart.getElementType().getJavaType());
                if (mappedByOnStartSide != null) {
                    LOG.trace("Fixing manyToMany for owned side map attribute {} in entity {}",
                        associationStart,
                        associationStartValue);
                    findManyToManyWithAttributeName(associationEndType, mappedByOnStartSide)
                        .ifPresent(associationEnd ->
                            associationEndMap.forEach((associationEndElementKey, associationEndElementValue) ->
                                populateCollectionOrMap(
                                    associationEndElementValue, associationEnd, associationStartValue)));
                } else {
                    LOG.trace("Fixing manyToMany for owning side map attribute {} in entity {}",
                        associationStart,
                        associationStartValue);
                    findManyToManyWithMappedBy(associationEndType, associationStart.getName())
                        .forEach(associationEnd ->
                            associationEndMap.values().forEach(associationEndElementValue ->
                                populateCollectionOrMap(
                                    associationEndElementValue, associationEnd, associationStartValue)));
                }
            }
        } else {
            Collection<?> associationEndCollection = (Collection<?>) resolveAttributeValue(
                associationStartValue, associationStart);
            if (associationEndCollection != null) {
                String mappedByOnStartSide = resolveMappedBy(associationStart.getJavaMember());
                EntityType<E> associationEndType = metamodel.entity(associationStart.getElementType().getJavaType());
                if (mappedByOnStartSide != null) {
                    LOG.trace("Fixing manyToMany for owned side collection attribute {} in entity {}",
                        associationStart,
                        associationStartValue);
                    findManyToManyWithAttributeName(associationEndType, mappedByOnStartSide)
                        .ifPresent(associationEnd ->
                            associationEndCollection.forEach(associationEndElementValue ->
                                populateCollectionOrMap(
                                    associationEndElementValue, associationEnd, associationStartValue)));
                } else {
                    LOG.trace("Fixing manyToMany for owning side collection attribute {} in entity {}",
                        associationStart,
                        associationStartValue);
                    findManyToManyWithMappedBy(associationEndType, associationStart.getName())
                        .forEach(associationEnd ->
                            associationEndCollection.forEach(associationEndElementValue ->
                                populateCollectionOrMap(
                                    associationEndElementValue, associationEnd, associationStartValue)));
                }
            }
        }
    }

    private static Collection<?> initializeCollection(Object entity, PluralAttribute<?, ?, ?> attribute) {
        Collection<?> newCollection;
        switch (attribute.getCollectionType()) {
            case SET:
                newCollection = new HashSet<>(0);
                break;
            case LIST:
            case COLLECTION:
                newCollection = new ArrayList<>(0);
                break;
            default: throw new IllegalStateException("Unknown collection type '"
                + attribute.getCollectionType() + "'.");
        }
        setAttributeValue(entity, attribute, newCollection);
        return newCollection;
    }

    /**
     * Adds newElement to the given collection attribute on the given entity.
     */
    private void populateCollectionOrMap(
        Object entity, PluralAttribute<?, ?, ?> attribute, Object newElement
    ) {
        if (attribute.getCollectionType() == PluralAttribute.CollectionType.MAP) {
            Map<Object, Object> reverseAssociationStartValue
                = (Map<Object, Object>) resolveAttributeValue(entity, attribute);
            if (reverseAssociationStartValue == null) {
                reverseAssociationStartValue = new HashMap<>(0);
            }
            Object mapKey = extractMapKey((MapAttribute<?, ?, ?>) attribute, newElement);
            if (mapKey == null) {
                LOG.debug("Map key resolved to null for map value {}", newElement);
            } else {
                LOG.debug("Put ({}, {}) to map attribute {} in entity {}", mapKey, newElement, attribute, entity);
                reverseAssociationStartValue.put(mapKey, newElement);
            }
        } else {
            Collection<Object> reverseAssociationStartValue
                = (Collection<Object>) resolveAttributeValue(entity, attribute);
            if (reverseAssociationStartValue == null) {
                reverseAssociationStartValue = (Collection<Object>) initializeCollection(entity, attribute);
            }
            if (!reverseAssociationStartValue.contains(newElement)) {
                LOG.debug("Add {} to collection attribute {} in entity {}", newElement, attribute, entity);
                reverseAssociationStartValue.add(newElement);
            }
        }
    }

    private Object extractMapKey(MapAttribute<?, ?, ?> attribute, Object mapValue) {
        // we need to extract based on @MapKeyJoinColumn - for that we need to match column names.
        EntityType<?> mapValueEntityType = metamodel.entity(mapValue.getClass());
        MapKeyJoinColumn mapKeyJoinColumn = JpaMetamodelUtil.getAnnotation(attribute, MapKeyJoinColumn.class);
        if (mapKeyJoinColumn != null) {
            String mapKeyJoinColumnName = mapKeyJoinColumn.name() == null
                ? attribute.getName() + "_KEY" : mapKeyJoinColumn.name();
            // @MapKeyJoinColumn requires us to map colum names. This would require a deeper integration with the
            // JPA provider, and so we rely on @JoinColumn annotation matching for now.
            return mapValueEntityType.getAttributes().stream()
                .filter(attr -> {
                    JoinColumn joinColumn = JpaMetamodelUtil.getAnnotation(attr, JoinColumn.class);
                    return joinColumn != null && mapKeyJoinColumnName.equals(joinColumn.name());
                })
                .filter(attr -> attr.getJavaType().equals(attribute.getKeyJavaType()))
                .findAny()
                .map(keyAttribute -> resolveAttributeValue(mapValue, keyAttribute))
                .orElse(null);
        }
        MapKey mapKey = JpaMetamodelUtil.getAnnotation(attribute, MapKey.class);
        if (mapKey != null && !mapKey.name().isEmpty()) {
            return mapValueEntityType.getAttributes().stream().filter(attr -> mapKey.name().equals(attr.getName()))
                .findAny()
                .map(keyAttribute -> resolveAttributeValue(mapValue, keyAttribute))
                .orElse(null);
        }
        if (mapValueEntityType.hasSingleIdAttribute()) {
            SingularAttribute<?, ?> idAttribute = JpaMetamodelUtil.getSingleIdAttribute(mapValueEntityType);
            return resolveAttributeValue(mapValue, idAttribute);
        } else {
            return initializeIdClass(mapValueEntityType, mapValue);
        }
    }

    private Object initializeIdClass(EntityType<?> entityType, Object entity) {
        Object idClassInstance;
        try {
            idClassInstance = entityType.getIdType().getJavaType().getDeclaredConstructor().newInstance();
        } catch (InstantiationException
                 | IllegalAccessException
                 | InvocationTargetException
                 | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        entityType.getIdClassAttributes().forEach(attr ->
            setAttributeValue(idClassInstance, attr, resolveAttributeValue(entity, attr)));
        return idClassInstance;
    }

    private static <T, E> Iterable<PluralAttribute<? super T, ?, E>> findReflectiveAttributesForManyToOne(
        ManagedType<T> managedType, SingularAttribute<? super E, T> manyToOneAttr
    ) {
        return managedType.getPluralAttributes().stream()
            .filter(attr -> attr.getPersistentAttributeType() == ONE_TO_MANY)
            .map(attr -> (PluralAttribute<? super T, ?, E>) attr)
            .filter(attr -> manyToOneAttr.getName().equals(resolveMappedBy(attr.getJavaMember())))
            .filter(attr -> manyToOneAttr.getDeclaringType().equals(attr.getElementType()))
            .collect(Collectors.toList());
    }

    private static <T> Iterable<Attribute<? super T, ?>> findOneToOneWithMappedBy(
        ManagedType<T> managedType, String mappedBy
    ) {
        return managedType.getAttributes().stream()
            .filter(attr -> attr.getPersistentAttributeType() == ONE_TO_ONE)
            .filter(attr -> mappedBy.equals(resolveMappedBy(attr.getJavaMember())))
            .collect(Collectors.toList());
    }

    private static <T> Optional<Attribute<? super T, ?>> findOneToOneWithAttributeName(
        ManagedType<T> managedType, String attributeName
    ) {
        return managedType.getAttributes().stream()
            .filter(attr -> attr.getPersistentAttributeType() == ONE_TO_ONE)
            .filter(attr -> attributeName.equals(attr.getName()))
            .findAny();
    }

    private static <T> Optional<Attribute<? super T, ?>> findManyToOneWithAttributeName(
        ManagedType<T> managedType, String attributeName
    ) {
        return managedType.getAttributes().stream()
            .filter(attr -> attr.getPersistentAttributeType() == MANY_TO_ONE)
            .filter(attr -> attributeName.equals(attr.getName()))
            .findAny();
    }

    private static <T> Iterable<PluralAttribute<? super T, ?, ?>> findManyToManyWithMappedBy(
        ManagedType<T> managedType, String mappedBy
    ) {
        return managedType.getPluralAttributes().stream()
            .filter(attr -> attr.getPersistentAttributeType() == MANY_TO_MANY)
            .filter(attr -> mappedBy.equals(resolveMappedBy(attr.getJavaMember())))
            .collect(Collectors.toList());
    }

    private static <T> Optional<PluralAttribute<? super T, ?, ?>> findManyToManyWithAttributeName(
        ManagedType<T> managedType, String attributeName
    ) {
        return managedType.getPluralAttributes().stream()
            .filter(attr -> attr.getPersistentAttributeType() == MANY_TO_MANY)
            .filter(attr -> attributeName.equals(attr.getName()))
            .findAny();
    }
}
