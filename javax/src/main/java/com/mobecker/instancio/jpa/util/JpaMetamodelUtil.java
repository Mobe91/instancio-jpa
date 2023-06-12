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
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;

public final class JpaMetamodelUtil {

    private JpaMetamodelUtil() { }

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
            return oneToOne.mappedBy();
        }
        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        if (oneToMany != null) {
            return oneToMany.mappedBy();
        }
        ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
        return manyToMany == null || manyToMany.mappedBy().isEmpty() ? null : manyToMany.mappedBy();
    }

    private static String resolveMappedBy(Method method) {
        OneToOne oneToOne = method.getAnnotation(OneToOne.class);
        if (oneToOne != null) {
            return oneToOne.mappedBy();
        }
        OneToMany oneToMany = method.getAnnotation(OneToMany.class);
        if (oneToMany != null) {
            return oneToMany.mappedBy();
        }
        ManyToMany manyToMany = method.getAnnotation(ManyToMany.class);
        return manyToMany == null || manyToMany.mappedBy().isEmpty() ? null : manyToMany.mappedBy();
    }

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

    public static SingularAttribute<?, ?> resolveIdAttribute(EntityType<?> entityType, String attributeName) {
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

    public static SingularAttribute<?, ?> getSingleIdAttribute(EntityType<?> entityType) {
        return entityType.getId(entityType.getIdType().getJavaType());
    }
}
