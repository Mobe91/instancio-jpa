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

import com.mobecker.instancio.jpa.EntityGraphAssociationFixer;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Persistence;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EntityGraphAssociationFixerTest {

    private static EntityManagerFactory emf;
    private static EntityGraphAssociationFixer entityGraphAssociationFixer;

    @BeforeAll
    static void setup() {
        emf = Persistence.createEntityManagerFactory("EntityGraphAssociationFixerTestPu");
        entityGraphAssociationFixer = new EntityGraphAssociationFixer(emf.getMetamodel());
    }

    @AfterAll
    static void tearDownEmf() {
        emf.close();
    }

    @Test
    void oneToMany() {
        // Given
        Order order1 = new Order();
        Order order2 = new Order();
        OrderItem orderItem = new OrderItem();
        order1.getOrderItems().add(orderItem);
        order1.getOrderItemsById().put(orderItem.getId(), orderItem);
        orderItem.setOrderForSet(order2);
        orderItem.setOrderForMap(order2);

        // When
        entityGraphAssociationFixer.fixAssociations(order1);

        // Then
        assertThat(order1.getOrderItems()).containsExactly(orderItem);
        assertThat(order1.getOrderItemsById()).containsExactly(new AbstractMap.SimpleEntry<>(orderItem.getId(), orderItem));
        assertThat(orderItem.getOrderForSet()).isEqualTo(order1);
        assertThat(orderItem.getOrderForMap()).isEqualTo(order1);
    }

    @Test
    void oneToMany_oneSided() {
        // Given
        Order order1 = new Order();
        Order order2 = new Order();
        OrderItem orderItem = new OrderItem();
        order1.getOneSidedOrderItems().add(orderItem);
        order1.getOneSidedOrderItemsById().put(orderItem.getId(), orderItem);
        orderItem.setOrderForSet(order2);

        // When
        entityGraphAssociationFixer.fixAssociations(order1);

        // Then
        assertThat(order1.getOneSidedOrderItems()).containsExactly(orderItem);
        assertThat(order1.getOneSidedOrderItemsById()).containsExactly(new AbstractMap.SimpleEntry<>(orderItem.getId(), orderItem));
        assertThat(orderItem.getOrderForSet()).isEqualTo(order2);
    }

    @Test
    void oneToOne_ownedSide() {
        // Given
        Order order1 = new Order();
        Order order2 = new Order();
        order1.setPrevious(order2);

        // When
        entityGraphAssociationFixer.fixAssociations(order1);

        // Then
        assertThat(order1.getPrevious()).isEqualTo(order2);
        assertThat(order2.getNext()).isEqualTo(order1);
    }

    @Test
    void oneToOne_owningSide() {
        // Given
        Order order1 = new Order();
        Order order2 = new Order();
        order1.setNext(order2);

        // When
        entityGraphAssociationFixer.fixAssociations(order1);

        // Then
        assertThat(order1.getNext()).isEqualTo(order2);
        assertThat(order2.getPrevious()).isEqualTo(order1);
    }

    @Test
    void manyToOne() {
        // Given
        OrderItem orderItem1 = new OrderItem(1L);
        OrderItem orderItem2 = new OrderItem(2L);
        Order order = new Order(3L);
        orderItem1.setOrderForSet(order);
        order.getOrderItems().add(orderItem2);

        // When
        entityGraphAssociationFixer.fixAssociations(orderItem1);

        // Then
        assertThat(orderItem1.getOrderForSet()).isEqualTo(order);
        assertThat(order.getOrderItems()).containsExactlyInAnyOrder(orderItem1, orderItem2);
    }

    @Test
    void manyToMany_owningSide() {
        // Given
        Order order1 = new Order(1L);
        Order order2 = new Order(2L);
        Person person = new Person(3L);
        order1.getContacts().add(person);
        order1.getContactsById().put(person.getId(), person);
        person.getOrders().add(order2);
        person.getOrdersByIdOwnedByContacts().put(order2.getId(), order2);
        person.getOrdersByIdOwnedByContactsById().put(order2.getId(), order2);

        // When
        entityGraphAssociationFixer.fixAssociations(order1);

        // Then
        assertThat(order1.getContacts()).containsExactly(person);
        assertThat(person.getOrders()).containsExactlyInAnyOrder(order1, order2);
        assertThat(person.getOrdersByIdOwnedByContacts()).containsExactly(
            new AbstractMap.SimpleEntry<>(order1.getId(), order1),
            new AbstractMap.SimpleEntry<>(order2.getId(), order2));
        assertThat(person.getOrdersByIdOwnedByContactsById()).containsExactly(
            new AbstractMap.SimpleEntry<>(order1.getId(), order1),
            new AbstractMap.SimpleEntry<>(order2.getId(), order2));
    }

    @Test
    void manyToMany_ownedSide() {
        // Given
        Person person1 = new Person(1L);
        Person person2 = new Person(2L);
        Order order = new Order(3L);
        person1.getOrders().add(order);
        person1.getOrdersByIdOwnedByContactsById().put(order.getId(), order);
        person1.getOrdersByIdOwnedByContacts().put(order.getId(), order);
        order.getContacts().add(person2);
        order.getContactsById().put(person2.getId(), person2);

        // When
        entityGraphAssociationFixer.fixAssociations(person1);

        // Then
        assertThat(person1.getOrders()).containsExactly(order);
        assertThat(order.getContacts()).containsExactlyInAnyOrder(person1, person2);
        assertThat(person1.getOrdersByIdOwnedByContacts()).containsExactly(new AbstractMap.SimpleEntry<>(order.getId(), order));
        assertThat(person1.getOrdersByIdOwnedByContactsById()).containsExactly(new AbstractMap.SimpleEntry<>(order.getId(), order));
    }

    @Test
    void oneToMany_mapKey() {
        // Given
        MapWithMapKeyHolder holder = new MapWithMapKeyHolder(1L);
        MapWithMapKeyValue mapValue1 = new MapWithMapKeyValue(2L);
        mapValue1.setKey("key1");
        mapValue1.setHolder(holder);

        // When
        entityGraphAssociationFixer.fixAssociations(mapValue1);

        // Then
        assertThat(holder.getMap()).containsExactly(new AbstractMap.SimpleEntry<>(mapValue1.getKey(), mapValue1));
    }

    @Test
    void oneToMany_mapKeyJoinColumn() {
        // Given
        MapWithMapKeyJoinColumnHolder holder = new MapWithMapKeyJoinColumnHolder(1L);
        MapWithMapKeyJoinColumnValue mapValue1 = new MapWithMapKeyJoinColumnValue(2L);
        Person person = new Person(3L);
        mapValue1.setPerson(person);
        mapValue1.setHolder(holder);

        // When
        entityGraphAssociationFixer.fixAssociations(mapValue1);

        // Then
        assertThat(holder.getMap()).containsExactly(new AbstractMap.SimpleEntry<>(mapValue1.getPerson(), mapValue1));
    }

    @Test
    void multipleOneToManyWithSameMappedByButDifferentTypes() {
        // Given
        MultiOneToManyHolder holder = new MultiOneToManyHolder();
        holder.assoc1Set.add(new MultiOneToManyHolderAssoc1());
        holder.assoc2Set.add(new MultiOneToManyHolderAssoc2());

        // When
        entityGraphAssociationFixer.fixAssociations(holder);

        // Then
        assertThat(holder.assoc1Set)
            .hasSize(1)
            .allSatisfy(assoc1 -> assertThat(assoc1).isExactlyInstanceOf(MultiOneToManyHolderAssoc1.class));
        assertThat(holder.assoc2Set)
            .hasSize(1)
            .allSatisfy(assoc2 -> assertThat(assoc2).isExactlyInstanceOf(MultiOneToManyHolderAssoc2.class));
    }

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    @ToString(onlyExplicitlyIncluded = true)
    public static class Order {
        @Id
        @ToString.Include
        private Long id;
        @OneToMany(mappedBy = "orderForSet")
        private Set<OrderItem> orderItems = new HashSet<>(0);
        @OneToMany
        private Set<OrderItem> oneSidedOrderItems = new HashSet<>(0);
        @OneToMany(mappedBy = "orderForMap")
        private Map<Long, OrderItem> orderItemsById = new HashMap<>(0);
        @OneToMany
        private Map<Long, OrderItem> oneSidedOrderItemsById = new HashMap<>(0);
        @OneToOne
        private Order next;
        @OneToOne(mappedBy = "next")
        private Order previous;
        @ManyToMany
        private Set<Person> contacts = new HashSet<>(0);
        @ManyToMany
        private Map<Long, Person> contactsById = new HashMap<>(0);

        public Order(Long id) {
            this.id = id;
        }
    }

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    @ToString(onlyExplicitlyIncluded = true)
    public static class OrderItem {
        @Id
        @ToString.Include
        private Long id;
        @ManyToOne
        private Order orderForSet;
        @ManyToOne
        private Order orderForMap;

        public OrderItem(Long id) {
            this.id = id;
        }
    }

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    @ToString(onlyExplicitlyIncluded = true)
    public static class Person {
        @Id
        @ToString.Include
        private Long id;
        @ManyToMany(mappedBy = "contacts")
        private Set<Order> orders = new HashSet<>(0);

        @ManyToMany(mappedBy = "contacts")
        private Map<Long, Order> ordersByIdOwnedByContacts = new HashMap<>(0);

        @ManyToMany(mappedBy = "contactsById")
        private Map<Long, Order> ordersByIdOwnedByContactsById = new HashMap<>(0);

        public Person(Long id) {
            this.id = id;
        }
    }

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    @ToString(onlyExplicitlyIncluded = true)
    public static class MapWithMapKeyHolder {
        @Id
        @ToString.Include
        private Long id;

        @OneToMany(mappedBy = "holder")
        @MapKey(name = "key")
        private Map<String, MapWithMapKeyValue> map = new HashMap<>(0);

        public MapWithMapKeyHolder(Long id) {
            this.id = id;
        }
    }

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    @ToString(onlyExplicitlyIncluded = true)
    public static class MapWithMapKeyValue {
        @Id
        @ToString.Include
        private Long id;
        private String key;
        @ManyToOne
        private MapWithMapKeyHolder holder;

        public MapWithMapKeyValue(Long id) {
            this.id = id;
        }
    }

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    @ToString(onlyExplicitlyIncluded = true)
    public static class MapWithMapKeyJoinColumnHolder {
        @Id
        @ToString.Include
        private Long id;

        @OneToMany(mappedBy = "holder")
        @MapKeyJoinColumn(name = "person_id")
        private Map<Person, MapWithMapKeyJoinColumnValue> map = new HashMap<>(0);

        public MapWithMapKeyJoinColumnHolder(Long id) {
            this.id = id;
        }
    }

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    @ToString(onlyExplicitlyIncluded = true)
    public static class MapWithMapKeyJoinColumnValue {
        @Id
        @ToString.Include
        private Long id;
        @ManyToOne
        private MapWithMapKeyJoinColumnHolder holder;
        @ManyToOne
        @JoinColumn(name = "person_id")
        private Person person;

        public MapWithMapKeyJoinColumnValue(Long id) {
            this.id = id;
        }
    }

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    @ToString(onlyExplicitlyIncluded = true)
    public static class MultiOneToManyHolder {
        @Id
        @ToString.Include
        private Long id;
        @OneToMany(mappedBy = "holder")
        private Set<MultiOneToManyHolderAssoc1> assoc1Set = new HashSet<>(0);
        @OneToMany(mappedBy = "holder")
        private Set<MultiOneToManyHolderAssoc2> assoc2Set = new HashSet<>(0);
    }

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    @ToString(onlyExplicitlyIncluded = true)
    public static class MultiOneToManyHolderAssoc1 {
        @Id
        @ToString.Include
        private Long id;
        @ManyToOne
        private MultiOneToManyHolder holder;
    }

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    @ToString(onlyExplicitlyIncluded = true)
    public static class MultiOneToManyHolderAssoc2 {
        @Id
        @ToString.Include
        private Long id;
        @ManyToOne
        private MultiOneToManyHolder holder;
    }
}
