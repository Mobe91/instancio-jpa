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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.mobecker.instancio.jpa.EntityGraphShrinker;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
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

class EntityGraphShrinkerTest {

    private static EntityManagerFactory emf;
    private static EntityGraphShrinker entityGraphShrinker;

    @BeforeAll
    static void setup() {
        emf = Persistence.createEntityManagerFactory("EntityGraphShrinkerTestPu");
        entityGraphShrinker = new EntityGraphShrinker(emf.getMetamodel(), null);
    }

    @AfterAll
    static void tearDownEmf() {
        emf.close();
    }

    @Test
    void singular() {
        // Given
        Order order1 = new Order();
        Order order2 = new Order();
        order2.setDescription(null);
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order1);
        orderItem.setOptionalOrder(order2);

        // When
        entityGraphShrinker.shrink(orderItem);

        // Then
        assertThat(orderItem.getOptionalOrder()).isNull();
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
    void mandatoryManyToOne_set() {
        // Given
        Order order = new Order();
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);

        // When / Then
        assertThatNoException().isThrownBy(() -> entityGraphShrinker.shrink(orderItem));
        assertThat(orderItem.getOrder()).isEqualTo(order);
    }

    @Test
    void oneToMany_mapWithInvalidElements() {
        // Given
        Order order = new Order();
        OrderItem orderItem1 = new OrderItem();
        OrderItem orderItem2 = new OrderItem();
        orderItem2.setOrder(order);
        order.getOrderItems().add(orderItem1);
        order.getOrderItems().add(orderItem2);

        // When
        entityGraphShrinker.shrink(order);

        // Then
        assertThat(order.getOrderItems()).containsExactly(orderItem2);
    }

    @Test
    void oneToMany_setWithInvalidElements() {
        // Given
        Order order = new Order();
        OrderItem orderItem1 = new OrderItem();
        OrderItem orderItem2 = new OrderItem();
        orderItem2.setOrder(order);
        order.getOrderItemsById().put(orderItem1.getId(), orderItem1);
        order.getOrderItemsById().put(orderItem2.getId(), orderItem2);

        // When
        entityGraphShrinker.shrink(order);

        // Then
        assertThat(order.getOrderItemsById()).containsExactly(new AbstractMap.SimpleEntry<>(orderItem2.getId(), orderItem2));
    }

    @Test
    void elementCollections() {
        // Given
        Set<String> set = Collections.singleton("setElement");
        List<String> list = Collections.singletonList("listElement");
        Map<String, String> map = Collections.singletonMap("mapKey", "mapValue");
        EntityWithElementCollections entity = new EntityWithElementCollections();
        entity.setSet(new HashSet<>(set));
        entity.setList(new ArrayList<>(list));
        entity.setMap(new HashMap<>(map));

        // When
        entityGraphShrinker.shrink(entity);

        // Then
        assertThat(entity.getSet()).isEqualTo(set);
        assertThat(entity.getList()).isEqualTo(list);
        assertThat(entity.getMap()).isEqualTo(map);
    }

    @Test
    void nonInsertableNullValue() {
        // Given
        EntityWithMandatoryNonInsertableValue entity = new EntityWithMandatoryNonInsertableValue();
        entity.setId(1L);

        // When / Then
        assertThatNoException().isThrownBy(() -> entityGraphShrinker.shrink(entity));
        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getNonInsertable()).isNull();
    }

    @Test
    void stopShrinkingAtDepth() {
        // Given
        Order order = new Order();
        order.setId(1L);
        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        order.getOrderItems().add(orderItem);
        EntityGraphShrinker entityGraphShrinker = new EntityGraphShrinker(emf.getMetamodel(), 0);

        // When
        entityGraphShrinker.shrink(order);

        // Then
        assertThat(order.getOrderItems()).hasSize(1);
    }

    @Entity
    @Getter
    @Setter
    public static class Order {
        @Id
        private Long id;
        @OneToMany
        private Set<OrderItem> orderItems = new HashSet<>(0);
        @OneToMany
        private Map<Long, OrderItem> orderItemsById = new HashMap<>(0);
        @Column(nullable = false)
        private String description = "some description";
    }

    @Entity
    @Getter
    @Setter
    public static class OrderItem {
        @Id
        private Long id;
        @ManyToOne(optional = false)
        private Order order;
        @ManyToOne
        private Order optionalOrder;
    }

    @Entity
    @Getter
    @Setter
    public static class EntityWithElementCollections {
        @Id
        private Long id;
        @ElementCollection
        private Set<String> set = new HashSet<>(0);
        @ElementCollection
        private List<String> list = new ArrayList<>(0);
        @ElementCollection
        private Map<String, String> map = new HashMap<>(0);
    }

    @Entity
    @Getter
    @Setter
    public static class EntityWithMandatoryNonInsertableValue {
        @Id
        private Long id;
        @Column(nullable = false, insertable = false)
        private String nonInsertable;
    }
}
