package com.mobecker.instancio.jpa.api;

import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import org.instancio.GeneratorSpecProvider;
import org.instancio.InstancioApi;
import org.instancio.Model;
import org.instancio.OnCompleteCallback;
import org.instancio.Result;
import org.instancio.TargetSelector;
import org.instancio.documentation.ExperimentalApi;
import org.instancio.generator.Generator;
import org.instancio.generator.GeneratorSpec;
import org.instancio.settings.Settings;

abstract class AbstractInstancioJpaApiImpl<T, E, D extends InstancioApi<T>> implements InstancioJpaApi<T> {

    private final EntityManager entityManager;

    private final Model<E> model;
    private D delegate;

    public AbstractInstancioJpaApiImpl(Model<E> model, EntityManager entityManager) {
        this.model = model;
        this.entityManager = entityManager;
    }

    protected void init() {
        this.delegate = createDelegate(model);
    }

    protected abstract D createDelegate(Model<E> model);

    protected D getDelegate() {
        return delegate;
    }

    @Override
    public T persist() {
        return persist(entityManager);
    }

    @Override
    public T create() {
        return delegate.create();
    }

    @Override
    public Result<T> asResult() {
        return delegate.asResult();
    }

    @Override
    public Stream<T> stream() {
        return delegate.stream();
    }

    @Override
    public Model<T> toModel() {
        return delegate.toModel();
    }

    public InstancioJpaApi<T> ignore(TargetSelector selector) {
        delegate.ignore(selector);
        return this;
    }

    public InstancioJpaApi<T> withNullable(TargetSelector selector) {
        delegate.withNullable(selector);
        return this;
    }

    public <V> InstancioJpaApi<T> set(TargetSelector selector, V value) {
        delegate.set(selector, value);
        return this;
    }

    public <V> InstancioJpaApi<T> supply(TargetSelector selector, Supplier<V> supplier) {
        delegate.supply(selector, supplier);
        return this;
    }

    public <V> InstancioJpaApi<T> supply(TargetSelector selector, Generator<V> generator) {
        delegate.supply(selector, generator);
        return this;
    }

    public <V> InstancioJpaApi<T> generate(TargetSelector selector,
                                           GeneratorSpecProvider<V> generatorSpecProvider) {
        delegate.generate(selector, generatorSpecProvider);
        return this;
    }

    @ExperimentalApi
    public <V> InstancioJpaApi<T> generate(TargetSelector selector, GeneratorSpec<V> generatorSpec) {
        delegate.generate(selector, generatorSpec);
        return this;
    }

    public <V> InstancioJpaApi<T> onComplete(TargetSelector selector, OnCompleteCallback<V> callback) {
        delegate.onComplete(selector, callback);
        return this;
    }

    public InstancioJpaApi<T> subtype(TargetSelector selector, Class<?> klass) {
        delegate.subtype(selector, klass);
        return this;
    }

    public InstancioJpaApi<T> withMaxDepth(int maxDepth) {
        delegate.withMaxDepth(maxDepth);
        return this;
    }

    public InstancioJpaApi<T> withSettings(Settings settings) {
        delegate.withSettings(settings);
        return this;
    }

    public InstancioJpaApi<T> withSeed(long seed) {
        delegate.withSeed(seed);
        return this;
    }

    @Override
    public InstancioJpaApi<T> lenient() {
        delegate.lenient();
        return this;
    }
}
