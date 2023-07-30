package com.mobecker.instancio.jpa.api;

import com.mobecker.instancio.jpa.EntityGraphPersister;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import org.instancio.GeneratorSpecProvider;
import org.instancio.InstancioApi;
import org.instancio.OnCompleteCallback;
import org.instancio.TargetSelector;
import org.instancio.documentation.ExperimentalApi;
import org.instancio.generator.Generator;
import org.instancio.generator.GeneratorSpec;
import org.instancio.settings.Settings;

public interface InstancioJpaApi<T> extends InstancioApi<T> {

    T persist();

    default T persist(EntityManager entityManager) {
        T entity = create();
        EntityGraphPersister persister = new EntityGraphPersister(entityManager);
        persister.persist(entity);
        return entity;
    }
    public InstancioJpaApi<T> ignore(TargetSelector selector);

    InstancioJpaApi<T> withNullable(TargetSelector selector);

    <V> InstancioJpaApi<T> set(TargetSelector selector, V value);

    <V> InstancioJpaApi<T> supply(TargetSelector selector, Supplier<V> supplier);

    <V> InstancioJpaApi<T> supply(TargetSelector selector, Generator<V> generator);

    <V> InstancioJpaApi<T> generate(TargetSelector selector, GeneratorSpecProvider<V> generatorSpecProvider);

    @ExperimentalApi
    <V> InstancioJpaApi<T> generate(TargetSelector selector, GeneratorSpec<V> generatorSpec);

    <V> InstancioJpaApi<T> onComplete(TargetSelector selector, OnCompleteCallback<V> callback);

    InstancioJpaApi<T> subtype(TargetSelector selector, Class<?> klass);

    InstancioJpaApi<T> withMaxDepth(int maxDepth);

    InstancioJpaApi<T> withSettings(Settings settings);

    InstancioJpaApi<T> withSeed(long seed);

    @Override
    InstancioJpaApi<T> lenient();
}
