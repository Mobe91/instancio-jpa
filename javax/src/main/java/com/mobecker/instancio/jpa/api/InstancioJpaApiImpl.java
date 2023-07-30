package com.mobecker.instancio.jpa.api;

import javax.persistence.EntityManager;
import org.instancio.InstancioApi;
import org.instancio.Model;
import org.instancio.internal.ApiImpl;

public class InstancioJpaApiImpl<T> extends AbstractInstancioJpaApiImpl<T, T, InstancioApi<T>> {
    public InstancioJpaApiImpl(Model<T> model, EntityManager entityManager) {
        super(model, entityManager);
        init();
    }

    @Override
    protected InstancioApi<T> createDelegate(Model<T> model) {
        return new ApiImpl<>(model);
    }
}
