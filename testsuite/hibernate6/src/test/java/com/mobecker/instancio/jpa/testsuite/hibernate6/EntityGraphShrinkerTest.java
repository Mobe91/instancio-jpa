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
package com.mobecker.instancio.jpa.testsuite.hibernate6;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;

import com.mobecker.instancio.jpa.EntityGraphShrinker;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Persistence;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class EntityGraphShrinkerTest {

    private static EntityManagerFactory emf;
    private static EntityGraphShrinker entityGraphShrinker;

    @BeforeAll
    static void setup() {
        emf = Persistence.createEntityManagerFactory("EntityGraphShrinkerTestPu");
        entityGraphShrinker = new EntityGraphShrinker(emf.getMetamodel());
    }

    @Test
    void mandatoryTenantIdWithNullValue() {
        // Given
        EntityWithMandatoryTenantId entity = new EntityWithMandatoryTenantId();
        entity.setId(1L);

        // When / Then
        assertThatNoException().isThrownBy(() -> entityGraphShrinker.shrink(entity));
        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getTenantId()).isNull();
    }

    @Entity
    @Getter
    @Setter
    public static class EntityWithMandatoryTenantId {
        @Id
        private Long id;
        @TenantId
        @Column(nullable = false)
        private Long tenantId;
    }
}
