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
package com.mobecker.instancio.jpa.testsuite;

import static org.assertj.core.api.Assertions.assertThat;

import com.mobecker.instancio.jpa.setting.JpaKeys;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.Persistence;
import lombok.Getter;
import lombok.Setter;
import org.instancio.Instancio;
import org.instancio.junit.InstancioExtension;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(InstancioExtension.class)
class InstancioJpaServiceProviderTest {

    private static EntityManagerFactory emf;

    @BeforeAll
    static void setup() {
        emf = Persistence.createEntityManagerFactory("InstancioJpaServiceProviderTestPu");
    }

    @Test
    void longId() {
        // Given
        Settings settings = Settings.create()
            .set(JpaKeys.METAMODEL, emf.getMetamodel())
            .set(JpaKeys.ENABLE_GENERATOR_PROVIDERS, true);

        // When
        List<OrderWithLongId> orders = Instancio.ofList(Instancio.of(OrderWithLongId.class)
            .withSettings(settings)
            .toModel()).size(2).create();

        // Then
        assertThat(orders.get(0).getId()).isEqualTo(1L);
        assertThat(orders.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void integerId() {
        // Given
        Settings settings = Settings.create()
            .set(JpaKeys.METAMODEL, emf.getMetamodel())
            .set(JpaKeys.ENABLE_GENERATOR_PROVIDERS, true);

        // When
        List<OrderWithIntegerId> orders = Instancio.ofList(Instancio.of(OrderWithIntegerId.class)
            .withSettings(settings)
            .toModel()).size(2).create();

        // Then
        assertThat(orders.get(0).getId()).isEqualTo(1L);
        assertThat(orders.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void stringId() {
        // Given
        Settings settings = Settings.create()
            .set(JpaKeys.METAMODEL, emf.getMetamodel())
            .set(JpaKeys.ENABLE_GENERATOR_PROVIDERS, true);

        // When
        List<OrderWithStringId> orders = Instancio.ofList(Instancio.of(OrderWithStringId.class)
            .withSettings(settings)
            .toModel()).size(2).create();

        // Then
        assertThat(orders.get(0).getId()).isEqualTo("1");
        assertThat(orders.get(1).getId()).isEqualTo("2");
    }

    @Test
    void uniqueString() {
        // Given
        Settings settings = Settings.create()
            .set(JpaKeys.METAMODEL, emf.getMetamodel())
            .set(JpaKeys.ENABLE_GENERATOR_PROVIDERS, true);

        // When
        List<OrderWithUniqueString> orders = Instancio.ofList(Instancio.of(OrderWithUniqueString.class)
            .withSettings(settings)
            .toModel()).size(2).create();

        // Then
        assertThat(orders.get(0).getUniqueValue()).isEqualTo("1");
        assertThat(orders.get(1).getUniqueValue()).isEqualTo("2");
    }

    @Test
    void columnLength() {
        // Given
        Settings settings = Settings.create()
            .set(JpaKeys.METAMODEL, emf.getMetamodel())
            .set(JpaKeys.ENABLE_GENERATOR_PROVIDERS, true);

        // When
        OrderWithColumnLength order = Instancio.create(Instancio.of(OrderWithColumnLength.class)
            .withSettings(settings)
            .toModel());

        // Then
        assertThat(order.getName()).hasSizeLessThanOrEqualTo(OrderWithColumnLength.COLUMN_LENGTH);
    }

    @Test
    void disableInstancioServiceProvider() {
        // Given
        Settings baseSettings = JpaKeys.defaults(emf.getMetamodel());

        // When
        OrderWithColumnLength orderWithGeneratorProviders = Instancio.create(Instancio.of(OrderWithColumnLength.class)
            .withSettings(Settings.from(baseSettings)
                .set(Keys.STRING_MIN_LENGTH, OrderWithColumnLength.COLUMN_LENGTH + 1))
            .toModel());
        OrderWithColumnLength orderWithoutGeneratorProviders = Instancio.create(Instancio.of(OrderWithColumnLength.class)
            .withSettings(Settings.from(baseSettings)
                .set(Keys.STRING_MIN_LENGTH, OrderWithColumnLength.COLUMN_LENGTH + 1)
                .set(JpaKeys.ENABLE_GENERATOR_PROVIDERS, false))
            .toModel());
        assertThat(orderWithGeneratorProviders.getName()).hasSizeLessThanOrEqualTo(OrderWithColumnLength.COLUMN_LENGTH);
        assertThat(orderWithoutGeneratorProviders.getName()).hasSizeGreaterThan(OrderWithColumnLength.COLUMN_LENGTH);
    }

    @Entity
    @Getter
    @Setter
    public static class OrderWithLongId {
        @Id
        private Long id;
    }

    @Entity
    @Getter
    @Setter
    public static class OrderWithIntegerId {
        @Id
        private Integer id;
    }

    @Entity
    @Getter
    @Setter
    public static class OrderWithStringId {
        @Id
        private String id;
    }

    @Entity
    @Getter
    @Setter
    public static class OrderWithUniqueString {
        @Id
        private Long id;
        @Column(unique = true)
        private String uniqueValue;
    }

    @Entity
    @Getter
    @Setter
    public static class OrderWithColumnLength {
        private static final int COLUMN_LENGTH = 2;

        @Id
        private Long id;
        @Column(length = COLUMN_LENGTH)
        private String name;
    }
}
