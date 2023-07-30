package com.mobecker.instancio.jpa.api;

import java.util.Collection;
import javax.persistence.EntityManager;
import org.instancio.InstancioOfCollectionApi;
import org.instancio.Model;
import org.instancio.internal.OfCollectionApiImpl;

public class InstancioJpaOfCollectionApiImpl<T, C extends Collection<T>>
    extends AbstractInstancioJpaApiImpl<C, T, InstancioOfCollectionApi<C>>
    implements InstancioJpaOfCollectionApi<C> {

    private final Class<C> collectionType;

    public InstancioJpaOfCollectionApiImpl(Class<C> collectionType, Model<T> model, EntityManager entityManager) {
        super(model, entityManager);
        this.collectionType = collectionType;
        init();
    }

    @Override
    protected InstancioOfCollectionApi<C> createDelegate(Model<T> model) {
        return OfCollectionApiImpl.fromElementModel(collectionType, model);
    }

    @Override
    public InstancioJpaOfCollectionApi<C> size(int i) {
        getDelegate().size(i);
        return this;
    }
}
