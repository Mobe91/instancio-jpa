/*
 * Copyright 2022-2023 the original author or authors.
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.mobecker.instancio.jpa.EntityGraphShrinker;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
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

    @AfterAll
    static void tearDownEmf() {
        emf.close();
    }

    @Test
    void oneToMany_unset() {
        // Given
        Order order = new Order();

        // When / Then
        assertThatNoException().isThrownBy(() -> entityGraphShrinker.shrink(order));
    }

    @Test
    void mandatoryManyToOne_unset() {
        // Given
        OrderItem orderItem = new OrderItem();

        // When / Then
        assertThatThrownBy(() -> entityGraphShrinker.shrink(orderItem)).hasMessageContaining("Cannot shrink");
    }

    @Test
    public void mandatoryManyToOne_set() {
        // Given
        Order order = new Order();
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);

        // When / Then
        assertThatNoException().isThrownBy(() -> entityGraphShrinker.shrink(orderItem));
        assertThat(orderItem.getOrder()).isEqualTo(order);
    }

    @Entity
    public static class Order {
        @Id
        private Long id;
        @OneToMany
        private Set<OrderItem> orderItems = new HashSet<>(0);

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Set<OrderItem> getOrderItems() {
            return orderItems;
        }

        public void setOrderItems(Set<OrderItem> orderItems) {
            this.orderItems = orderItems;
        }
    }

    @Entity
    public static class OrderItem {
        @Id
        private Long id;
        @ManyToOne(optional = false)
        private Order order;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Order getOrder() {
            return order;
        }

        public void setOrder(Order order) {
            this.order = order;
        }
    }
}
