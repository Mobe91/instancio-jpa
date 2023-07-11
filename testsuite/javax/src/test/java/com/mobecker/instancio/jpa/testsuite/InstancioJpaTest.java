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

import static com.mobecker.instancio.jpa.InstancioJpa.jpaModel;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.Persistence;
import lombok.Getter;
import lombok.Setter;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InstancioJpaTest {

    private static EntityManagerFactory emf;

    @BeforeAll
    static void createEmf() {
        emf = Persistence.createEntityManagerFactory("InstancioJpaTestPu");
    }

    @Test
    void createStream() {
        // When
        Stream<Order> orderStream = Instancio.stream(jpaModel(Order.class, emf.getMetamodel()).build())
            .limit(2);

        // Then
        assertThat(orderStream).doesNotContainNull().hasSize(2);
    }

    @Test
    void createList() {
        // When
        List<Order> orderList = Instancio.ofList(jpaModel(Order.class, emf.getMetamodel()).build())
            .size(2)
            .create();

        // Then
        assertThat(orderList).doesNotContainNull().hasSize(2);
    }


    @Test
    void createSet() {
        // When
        Set<Order> orderSet = Instancio.ofSet(jpaModel(Order.class, emf.getMetamodel()).build())
            .size(2)
            .create();

        // Then
        assertThat(orderSet).doesNotContainNull().hasSize(2);
    }

    @Test
    void onComplete_single() {
        // When
        long commonId = 23L;
        Order order = Instancio.create(jpaModel(Order.class, emf.getMetamodel())
                .onComplete(o -> o.setId(commonId)).build());

        // Then
        assertThat(order.getId()).isEqualTo(commonId);
    }

    @Test
    void onComplete_multi() {
        // When
        long commonId = 23L;
        Set<Order> orderSet = Instancio.ofSet(jpaModel(Order.class, emf.getMetamodel())
                .onComplete(order -> order.setId(commonId)).build())
            .size(2)
            .create();

        // Then
        assertThat(orderSet).allSatisfy(order -> assertThat(order.getId()).isEqualTo(commonId));
    }

    @Entity
    @Getter
    @Setter
    public static class Order {
        @Id
        private Long id;
    }
}
