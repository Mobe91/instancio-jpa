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

package com.mobecker.instancio.jpa;

import static org.instancio.Select.root;

import com.mobecker.instancio.jpa.selector.JpaGeneratedIdSelector;
import com.mobecker.instancio.jpa.selector.JpaOptionalAttributeSelector;
import com.mobecker.instancio.jpa.selector.JpaTransientAttributeSelector;
import com.mobecker.instancio.jpa.setting.JpaKeys;
import javax.persistence.metamodel.Metamodel;
import org.instancio.Instancio;
import org.instancio.InstancioApi;
import org.instancio.Model;
import org.instancio.OnCompleteCallback;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;

/**
 * Instancio-jpa extension for creating persistable JPA entities based using the Instancio API.
 *
 * @since 1.0.0
 */
public final class InstancioJpa {

    private InstancioJpa() { }

    /**
     * Creates a builder for constructing an Instancio model that produces persistable JPA entities.
     *
     * <p/>Example:
     * <pre>{@code
     *   Model<Person> simpsons = jpaModel(Person.class, metamodel)
     *       .set(field(Person::getLastName), "Simpson")
     *       .set(field(Address::getCity), "Springfield")
     *       .generate(field(Person::getAge), gen -> gen.ints().range(40, 50))
     *       .build();
     *
     *   Person homer = Instancio.of(simpsons)
     *       .set(field(Person::getFirstName), "Homer")
     *       .set(all(Gender.class), Gender.MALE)
     *       .create();
     *
     *   Person marge = Instancio.of(simpsons)
     *       .set(field(Person::getFirstName), "Marge")
     *       .set(all(Gender.class), Gender.FEMALE)
     *       .create();
     * }</pre>
     *
     * @param entityClass JPA entity class for which an Instancio model should be created
     * @param metamodel reference to the JPA metamodel
     * @param <T> JPA entity type to create
     * @return InstancioJpa builder reference
     * @since 1.0.0
     */
    public static <T> Builder<T> jpaModel(Class<T> entityClass, Metamodel metamodel) {
        return new Builder<>(entityClass, metamodel);
    }

    /**
     * Builder for constructing an Instancio model that produces persistable JPA entities.
     *
     * @param <T> JPA entity type to create
     */
    public static final class Builder<T> {

        private final Class<T> entityClass;
        private final Metamodel metamodel;
        private final EntityGraphShrinker entityGraphShrinker;
        private final EntityGraphAssociationFixer entityGraphAssociationFixer;
        private Settings settings;
        private OnCompleteCallback<T> onCompleteCallback;

        private Builder(Class<T> entityClass, Metamodel metamodel) {
            this.entityClass = entityClass;
            this.metamodel = metamodel;
            this.entityGraphShrinker = new EntityGraphShrinker(metamodel);
            this.entityGraphAssociationFixer = new EntityGraphAssociationFixer(metamodel);
        }

        /**
         * Override default {@link Settings} for generating values.
         * The {@link Settings} class supports various parameters, such as
         * collection sizes, string lengths, numeric ranges, and so on.
         * For a list of overridable settings, refer to the {@link Keys} and {@link JpaKeys} class.
         *
         * @param settings to use
         * @return InstancioJpa builder reference
         * @see Keys
         * @see JpaKeys
         * @since 1.0.0
         */
        public Builder<T> withSettings(Settings settings) {
            this.settings = settings;
            return this;
        }

        /**
         * A callback that gets invoked after an object has been fully populated.
         *
         * <p/>
         * Example:
         * <pre>{@code
         *     // Sets name field on all instances of Person to the specified value
         *     Model<Person> personModel = jpaModel(Person.class, metamodel).onComplete().build();
         *     Person person = Instancio.of(personModel)
         *             .onComplete(newPerson -> newPerson.setName("John"))
         *             .create();
         * }</pre>
         *
         * @param callback to invoke after object has been populated
         * @return InstancioJpa builder reference
         * @since 1.0.0
         */
        public Builder<T> onComplete(OnCompleteCallback<T> callback) {
            this.onCompleteCallback = callback;
            return this;
        }

        /**
         * Creates a model containing all the information for populating entities in a persistable way.
         *
         * @return a model that can be used as a template for creating persistable JPA entities
         * @since 1.0.0
         */
        public Model<T> build() {
            Settings settings;
            if (this.settings == null) {
                settings = JpaKeys.defaults(metamodel)
                    .set(JpaKeys.ENABLE_GENERATOR_PROVIDERS, true);
            } else {
                settings = Settings.from(this.settings).set(JpaKeys.METAMODEL, metamodel);
                setJpaKeyDefaults(settings);
            }
            InstancioApi<T> instancioApi = Instancio.of(entityClass)
                // TODO: Register selectors only when needed to avoid lenient() at this point
                .lenient()
                .set(JpaTransientAttributeSelector.jpaTransient(metamodel), null)
                .set(JpaGeneratedIdSelector.jpaGeneratedId(metamodel), null)
                .withSettings(settings);

            if (settings.get(JpaKeys.USE_JPA_NULLABILITY)) {
                instancioApi.withNullable(JpaOptionalAttributeSelector.jpaOptionalAttribute(metamodel));
            }

            EntityGraphMinDepthPredictor entityGraphMinDepthPredictor =
                new EntityGraphMinDepthPredictor(metamodel);
            int minDepth = entityGraphMinDepthPredictor.predictRequiredDepth(entityClass);
            instancioApi.withMaxDepth(minDepth);

            return instancioApi
                .onComplete(root(), (root) -> {
                    if (root instanceof Iterable<?>) {
                        ((Iterable<?>) root).forEach(entityGraphShrinker::shrink);
                        ((Iterable<?>) root).forEach(entityGraphAssociationFixer::fixAssociations);
                        if (onCompleteCallback != null) {
                            ((Iterable<T>) root).forEach(onCompleteCallback::onComplete);
                        }
                    } else {
                        entityGraphShrinker.shrink(root);
                        entityGraphAssociationFixer.fixAssociations(root);
                        if (onCompleteCallback != null) {
                            onCompleteCallback.onComplete((T) root);
                        }
                    }
                })
                .toModel();
        }

        private static void setJpaKeyDefaults(Settings settings) {
            if (settings.get(JpaKeys.ENABLE_GENERATOR_PROVIDERS) == null) {
                settings.set(JpaKeys.ENABLE_GENERATOR_PROVIDERS,
                    JpaKeys.ENABLE_GENERATOR_PROVIDERS.defaultValue());
            }
            if (settings.get(JpaKeys.USE_JPA_NULLABILITY) == null) {
                settings.set(JpaKeys.USE_JPA_NULLABILITY, JpaKeys.USE_JPA_NULLABILITY.defaultValue());
            }
        }
    }
}
