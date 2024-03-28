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

import static org.assertj.core.api.Assertions.assertThat;

import com.mobecker.instancio.jpa.EntityGraphMinDepthPredictor;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Persistence;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EntityGraphMinDepthPredictorTest {

    private static EntityManagerFactory emf;
    private static EntityGraphMinDepthPredictor entityGraphMinDepthPredictor;

    @BeforeAll
    static void setup() {
        emf = Persistence.createEntityManagerFactory("EntityGraphMinDepthPredictorTestPu");
        entityGraphMinDepthPredictor = new EntityGraphMinDepthPredictor(emf.getMetamodel());
    }

    @AfterAll
    static void tearDownEmf() {
        emf.close();
    }

    @Test
    void noAssociations() {
        // When
        int predictedMaxDepth = entityGraphMinDepthPredictor.predictRequiredDepth(SingleLevelEntity.class);

        // Then
        assertThat(predictedMaxDepth).isEqualTo(1);
    }

    @Test
    void oneToMany() {
        // When
        int predictedMaxDepth = entityGraphMinDepthPredictor.predictRequiredDepth(Order.class);

        // Then
        assertThat(predictedMaxDepth).isEqualTo(1);
    }

    @Test
    void manyToOne() {
        // When
        int predictedMaxDepth = entityGraphMinDepthPredictor.predictRequiredDepth(OrderItem.class);

        // Then
        assertThat(predictedMaxDepth).isEqualTo(2);
    }

    @Test
    void manyToOne_optional() {
        // When
        int predictedMaxDepth = entityGraphMinDepthPredictor.predictRequiredDepth(OrderItemWithOptionalOrder.class);

        // Then
        assertThat(predictedMaxDepth).isEqualTo(1);
    }

    @Test
    void embeddable_optionalComponent() {
        // When
        int predictedMaxDepth = entityGraphMinDepthPredictor.predictRequiredDepth(EmbeddableParent1.class);

        // Then
        assertThat(predictedMaxDepth).isEqualTo(1);
    }

    @Test
    void embeddable_mandatoryComponent() {
        // When
        int predictedMaxDepth = entityGraphMinDepthPredictor.predictRequiredDepth(EmbeddableParent2.class);

        // Then
        assertThat(predictedMaxDepth).isEqualTo(2);
    }

    @Test
    void embeddable_nestedOptionalComponentByOverride() {
        // When
        int predictedMaxDepth = entityGraphMinDepthPredictor.predictRequiredDepth(EmbeddableParent4.class);

        // Then
        assertThat(predictedMaxDepth).isEqualTo(3);
    }

    @Test
    void embeddable_nestedMandatoryComponentByOverride() {
        // When
        int predictedMaxDepth = entityGraphMinDepthPredictor.predictRequiredDepth(EmbeddableParent5.class);

        // Then
        assertThat(predictedMaxDepth).isEqualTo(1);
    }

    @Entity
    @Getter
    @Setter
    public static class SingleLevelEntity {
        @Id
        private Long id;
    }

    @Entity
    @Getter
    @Setter
    public static class Order {
        @Id
        private Long id;
        @OneToMany
        private Set<OrderItem> orderItems = new HashSet<>(0);
    }

    @Entity
    @Getter
    @Setter
    public static class OrderItem {
        @Id
        private Long id;
        @ManyToOne(optional = false)
        private Order order;
    }

    @Entity
    @Getter
    @Setter
    public static class OrderItemWithOptionalOrder {
        @Id
        private Long id;
        @ManyToOne
        private Order order;
    }

    @Entity
    @Getter
    @Setter
    public static class EmbeddableParent1 {
        @Id
        private Long id;
        @Embedded
        private EmbeddableWithOptionalComponent embeddable;
    }

    @Embeddable
    @Getter
    @Setter
    public static class EmbeddableWithOptionalComponent {
        private String name;
    }

    @Entity
    @Getter
    @Setter
    public static class EmbeddableParent2 {
        @Id
        private Long id;
        @Embedded
        private EmbeddableWithMandatoryComponent embeddable;
    }

    @Embeddable
    @Getter
    @Setter
    public static class EmbeddableWithMandatoryComponent {
        @Column(nullable = false)
        private String name;
    }

    @Embeddable
    @Getter
    @Setter
    public static class EmbeddableWithNestedEmbeddableWithMandatoryComponent {
        @Embedded
        private EmbeddableWithMandatoryComponent nested;
    }

    @Embeddable
    @Getter
    @Setter
    public static class EmbeddableWithNestedEmbeddableWithOptionalComponent {
        @Embedded
        private EmbeddableWithOptionalComponent nested;
    }

    @Entity
    @Getter
    @Setter
    public static class EmbeddableParent4 {
        @Id
        private Long id;
        @Embedded
        @AttributeOverride(name = "nested.name", column = @Column(nullable = false))
        private EmbeddableWithNestedEmbeddableWithOptionalComponent embeddable;
    }

    @Entity
    @Getter
    @Setter
    public static class EmbeddableParent5 {
        @Id
        private Long id;
        @Embedded
        @AttributeOverride(name = "nested.name", column = @Column(nullable = true))
        private EmbeddableWithNestedEmbeddableWithMandatoryComponent embeddable;
    }
}
