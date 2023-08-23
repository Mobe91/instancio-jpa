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

package com.mobecker.instancio.jpa.util;

import com.blazebit.reflection.ReflectionUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.SingularAttribute;

/**
 * A utility class with helper methods related to navigating and manipulating object graphs based on the JPA
 * metamodel.
 *
 * @since 1.0.0
 */
public final class JpaMetamodelUtil {

    private JpaMetamodelUtil() { }

    /**
     * Resolves the value of {@code attribute} against the {@code entity}.
     *
     * @param entity JPA entity
     * @param attribute JPA attribute
     * @return the attribute value
     * @since 1.0.0
     */
    @Nullable
    public static Object resolveAttributeValue(Object entity, Attribute<?, ?> attribute) {
        Method getter = ReflectionUtils.getGetter(entity.getClass(), attribute.getName());
        if (getter == null) {
            try {
                Field field = attribute.getDeclaringType().getJavaType().getDeclaredField(attribute.getName());
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                return field.get(entity);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                if (!getter.isAccessible()) {
                    getter.setAccessible(true);
                }
                return getter.invoke(entity);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Sets the provided {@code value} for the {@code attribute} on the target {@code entity}.
     *
     * @param target the target JPA entity
     * @param attribute the JPA attribute to set
     * @param value the attribute value to set
     * @since 1.0.0
     */
    public static void setAttributeValue(Object target, Attribute<?, ?> attribute, @Nullable Object value) {
        Method setter = ReflectionUtils.getSetter(target.getClass(), attribute.getName());
        if (setter == null) {
            try {
                Field field = attribute.getDeclaringType().getJavaType().getDeclaredField(attribute.getName());
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                field.set(target, value);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                if (!setter.isAccessible()) {
                    setter.setAccessible(true);
                }
                setter.invoke(target, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Resolves the value of any {@code mappedBy} annotation attribute from the provided {@code member}.
     *
     * @param member The java member to read the annotation from
     * @return The value of any {@code mappedBy} annotation attribute on the member, or {@code null} if no matching
     *     annotation attribute could be found
     * @since 1.0.0
     */
    public static String resolveMappedBy(Member member) {
        if (member instanceof Field) {
            return resolveMappedBy((Field) member);
        } else if (member instanceof Method) {
            return resolveMappedBy((Method) member);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static String resolveMappedBy(Field field) {
        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
        if (oneToOne != null) {
            return nullIfEmpty(oneToOne.mappedBy());
        }
        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        if (oneToMany != null) {
            return nullIfEmpty(oneToMany.mappedBy());
        }
        ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
        return manyToMany == null ? null : nullIfEmpty(manyToMany.mappedBy());
    }

    private static String resolveMappedBy(Method method) {
        OneToOne oneToOne = method.getAnnotation(OneToOne.class);
        if (oneToOne != null) {
            return nullIfEmpty(oneToOne.mappedBy());
        }
        OneToMany oneToMany = method.getAnnotation(OneToMany.class);
        if (oneToMany != null) {
            return nullIfEmpty(oneToMany.mappedBy());
        }
        ManyToMany manyToMany = method.getAnnotation(ManyToMany.class);
        return manyToMany == null ? null : nullIfEmpty(manyToMany.mappedBy());
    }

    private static String nullIfEmpty(String s) {
        return s.isEmpty() ? null : s;
    }

    /**
     * Resolve annotation from JPA attribute by annotation type. The method uses reflection on the java member of
     * the JPA attribute to retrieve annotations.
     *
     * @param attr the JPA attribute
     * @param annotationClass the annotation type to resolve
     * @param <T> the annotation type
     * @return the annotation, or {@code null} if no annotation of the specified type exists for {@code attr}
     * @since 1.0.0
     */
    public static <T extends Annotation> T getAnnotation(Attribute<?, ?> attr, Class<T> annotationClass) {
        Member member = attr.getJavaMember();
        if (member instanceof Field) {
            return ((Field) member).getAnnotation(annotationClass);
        } else if (member instanceof Method) {
            return ((Method) member).getAnnotation(annotationClass);
        } else {
            return null;
        }
    }

    /**
     * Resolves a JPA id attribute by entity type and attribute name.
     *
     * @param entityType the JPA entity type
     * @param attributeName the JPA attribute name
     * @return the JPA attribute matching the specified attribute name, or {@code null} if either no attribute with
     *     the given name exists for the entity type or the matching attribute is not an id attribute.
     * @since 1.0.0
     */
    public static SingularAttribute<?, ?> resolveIdAttribute(IdentifiableType<?> entityType, String attributeName) {
        if (entityType.hasSingleIdAttribute()) {
            SingularAttribute<?, ?> attr = getSingleIdAttribute(entityType);
            return attr.getName().equals(attributeName) ? attr : null;
        } else {
            return entityType.getIdClassAttributes().stream()
                .filter(attr -> attr.getName().equals(attributeName))
                .findAny()
                .orElse(null);
        }
    }

    /**
     * Resolves the single JPA id attribute of the given entity type.
     *
     * @param entityType the JPA entity type
     * @return the single JPA id attribute
     * @throws IllegalArgumentException if id attribute of the given
     *     type is not present in the entity type or if
     *     the entity type has an id class
     * @since 1.0.0
     */
    public static SingularAttribute<?, ?> getSingleIdAttribute(IdentifiableType<?> entityType) {
        return entityType.getId(entityType.getIdType().getJavaType());
    }

    /**
     * Checks if a JPA attribute is insertable according to its mapping annotations.
     *
     * @param attribute the JPA attribute to check.
     * @return true if the attribute is insertable, else false.
     */
    public static boolean isInsertable(Attribute<?, ?> attribute) {
        Column column = getAnnotation(attribute, Column.class);
        JoinColumn joinColumn = getAnnotation(attribute, JoinColumn.class);
        return (column == null || column.insertable()) && (joinColumn == null || joinColumn.insertable());
    }
}
