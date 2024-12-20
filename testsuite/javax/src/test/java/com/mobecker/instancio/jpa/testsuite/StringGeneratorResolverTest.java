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
package com.mobecker.instancio.jpa.testsuite;

import static com.mobecker.instancio.jpa.InstancioJpa.jpaModel;
import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Persistence;
import lombok.Getter;
import lombok.Setter;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StringGeneratorResolverTest {

    private static EntityManagerFactory emf;

    @BeforeAll
    static void createEmf() {
        emf = Persistence.createEntityManagerFactory("StringGeneratorResolverTestPu");
    }

    @Test
    void respectStringMaxLengthFromSettings() {
        // When
        Order order = Instancio.create(jpaModel(Order.class, emf.getMetamodel())
                .withSettings(Settings.create().set(Keys.STRING_MAX_LENGTH, 20))
                .build());

        // Then
        assertThat(order.getBigData())
            .isNotNull()
            .hasSizeLessThanOrEqualTo(20);
    }

    @Entity
    @Getter
    @Setter
    public static class Order {
        @Id
        private Long id;
        @Lob
        @Column(length = Integer.MAX_VALUE)
        private String bigData;
    }
}
