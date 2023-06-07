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

import static com.mobecker.instancio.jpa.InstancioJpa.jpaModel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.fields;

import com.mobecker.instancio.jpa.EntityGraphPersister;
import com.mobecker.instancio.jpa.setting.JpaKeys;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Persistence;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.junit.InstancioExtension;
import org.instancio.junit.Seed;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(InstancioExtension.class)
public class EntityGraphPersisterTest {

    private static EntityManagerFactory emf;
    private EntityManager entityManager;
    private EntityGraphPersister entityGraphPersister;

    @BeforeAll
    static void createEmf() {
        emf = Persistence.createEntityManagerFactory("EntityGraphPersisterTestPu");
    }

    @BeforeEach
    void setup() {
        entityManager = emf.createEntityManager();
        entityGraphPersister = new EntityGraphPersister(entityManager);
    }

    @Test
    public void testFlatOrder() {
        // Given
        FlatOrder order = Instancio.of(jpaModel(FlatOrder.class, emf.getMetamodel()).build()).create();

        // When
        doInTransaction(() -> {
            entityGraphPersister.persist(order);
        });

        // Then
        FlatOrder actualOrder = doInTransaction(() -> entityManager.find(FlatOrder.class, order.getId()));
        assertThat(actualOrder).isNotNull();
    }

    @Test
    public void testOrder() {
        // Given
        Order order = Instancio.of(jpaModel(Order.class, emf.getMetamodel()).build()).create();

        // When
        doInTransaction(() -> {
            entityGraphPersister.persist(order);
        });

        // Then
        Order actualOrder = doInTransaction(() -> entityManager.find(Order.class, order.getId()));
        assertThat(actualOrder).isNotNull();
    }

    @Test
    public void testOrderWithDescription() {
        // Given
        OrderWithDescription1 order = Instancio.of(jpaModel(OrderWithDescription1.class, emf.getMetamodel()).build())
            .set(fields().named("id"), null)
            .create();

        // When
        doInTransaction(() -> {
            entityGraphPersister.persist(order);
        });

        // Then
        OrderWithDescription1
            actualOrder = doInTransaction(() -> entityManager.find(OrderWithDescription1.class, order.getId()));
        assertThat(actualOrder).isNotNull();
    }

    @Test
    @Seed(4407964936668837108L)
    public void useJpaNullabilitySetting() {
        // Given
        Model<OrderWithDescription> model = jpaModel(OrderWithDescription.class, emf.getMetamodel())
                .withSettings(Settings.defaults().set(JpaKeys.USE_JPA_NULLABILITY, false))
                .build();

        // When
        OrderWithDescription order = Instancio.of(model).create();

        // Then
        assertThat(order.getDescription()).isNotNull();
    }

    private <V> V doInTransaction(Callable<V> callable) {
        EntityTransaction tx = null;
        try {
            tx = entityManager.getTransaction();
            tx.begin();
            V result = callable.call();
            tx.commit();
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        }
    }

    private void doInTransaction(Runnable runnable) {
        doInTransaction(() -> {
            runnable.run();
            return null;
        });
    }

    @Entity
    @Table(name = "service_order")
    @Getter
    @Setter
    public static class FlatOrder {

        @Id
        @GeneratedValue
        private Long id;

        @OneToMany
        private Set<OrderItem> orderItems = new HashSet<>(0);
    }

    @Entity
    @Table(name = "service_order")
    public static class Order extends AbstractEntity {

        private Set<OrderItem> orderItems = new HashSet<>(0);

        @Id
        @GeneratedValue
        @Override
        public Long getId() {
            return super.getId();
        }

        @OneToMany
        public Set<OrderItem> getOrderItems() {
            return orderItems;
        }

        public void setOrderItems(Set<OrderItem> orderItems) {
            this.orderItems = orderItems;
        }
    }

    public static abstract class AbstractEntity {

        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    @Entity
    @Table(name = "order_item")
    public static class OrderItem {
        @Id
        @GeneratedValue
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    @Entity
    @Getter
    @Setter
    public static class OrderWithDescription {
        @Id
        private Long id;
        private String description;
    }

    @MappedSuperclass
    public static abstract class AbstractOrder extends AbstractEntity {

        private String description;

        @Id
        @GeneratedValue
        @Override
        public Long getId() {
            return super.getId();
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    @Entity
    @AttributeOverride(name = "description", column = @Column(nullable = false))
    public static class OrderWithDescription1 extends AbstractOrder { }

    @Entity
    public static class OrderWithDescription2 extends AbstractOrder { }
}
