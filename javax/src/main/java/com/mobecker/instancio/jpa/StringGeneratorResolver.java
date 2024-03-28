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

import static com.mobecker.instancio.jpa.util.JpaMetamodelUtil.getAnnotation;

import javax.persistence.Column;
import javax.persistence.metamodel.Attribute;
import org.instancio.Node;
import org.instancio.generator.GeneratorSpec;
import org.instancio.generators.Generators;

/**
 * A generator for {@link String} typed JPA attributes that respects the {@link Column#length()}.
 *
 * @since 1.1.0
 */
public class StringGeneratorResolver implements JpaAttributeGeneratorResolver {

    @Override
    public GeneratorSpec<?> getGenerator(
        Node node, Generators generators, Attribute<?, ?> attribute, GeneratorResolverContext context) {
        Integer columnLength;
        if (isStringTyped(attribute) && (columnLength = getColumnLength(attribute)) != null) {
            return generators.string().maxLength(columnLength);
        }
        return null;
    }

    private static boolean isStringTyped(Attribute<?, ?> attribute) {
        return String.class.equals(attribute.getJavaType());
    }

    private static Integer getColumnLength(Attribute<?, ?> attribute) {
        Column column = getAnnotation(attribute, Column.class);
        return column == null ? null : column.length();
    }
}
