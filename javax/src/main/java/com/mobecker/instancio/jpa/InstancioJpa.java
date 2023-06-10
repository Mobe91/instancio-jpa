package com.mobecker.instancio.jpa;

import static org.instancio.Select.root;

import com.mobecker.instancio.jpa.selector.JpaGeneratedIdSelector;
import com.mobecker.instancio.jpa.selector.JpaOptionalAttributeSelector;
import com.mobecker.instancio.jpa.selector.JpaTransientAttributeSelectorBuilder;
import com.mobecker.instancio.jpa.setting.JpaKeys;
import javax.persistence.metamodel.Metamodel;
import org.instancio.Instancio;
import org.instancio.InstancioApi;
import org.instancio.Model;
import org.instancio.settings.Settings;

public final class InstancioJpa {

    private InstancioJpa() { }

    public static <T> Builder<T> jpaModel(Class<T> entityClass, Metamodel metamodel) {
        return new Builder<>(entityClass, metamodel);
    }

    public static final class Builder<T> {

        private final Class<T> entityClass;
        private final Metamodel metamodel;
        private Settings settings;

        public Builder(Class<T> entityClass, Metamodel metamodel) {
            this.entityClass = entityClass;
            this.metamodel = metamodel;
        }

        public Builder<T> withSettings(Settings settings) {
            this.settings = settings;
            return this;
        }

        public Model<T> build() {
            Settings settings = this.settings == null
                ? Settings.defaults().merge(JpaKeys.defaults(metamodel)) : this.settings;
            InstancioApi<T> instancioApi = Instancio.of(entityClass)
                // TODO: Register selectors only when needed to avoid lenient() at this point
                .lenient()
                .set(JpaTransientAttributeSelectorBuilder.jpaTransient(metamodel), null)
                .set(JpaGeneratedIdSelector.jpaGeneratedId(metamodel), null)
                .withSettings(settings.set(JpaKeys.METAMODEL, metamodel));

            if (settings.get(JpaKeys.USE_JPA_NULLABILITY)) {
                instancioApi.withNullable(JpaOptionalAttributeSelector.jpaOptionalAttribute(metamodel));
            }

            EntityGraphMinDepthPredictor entityGraphMinDepthPredictor =
                new EntityGraphMinDepthPredictor(metamodel);
            int minDepth = entityGraphMinDepthPredictor.predictRequiredDepth(entityClass);
            instancioApi.withMaxDepth(minDepth);

            return instancioApi
                .onComplete(root(), (root) -> {
                    new EntityGraphShrinker(metamodel).shrink(root);
                    new EntityGraphAssociationFixer(metamodel).fixAssociations(root);
                })
                .toModel();
        }
    }
}
