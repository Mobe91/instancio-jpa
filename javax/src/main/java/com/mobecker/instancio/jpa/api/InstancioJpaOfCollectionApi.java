package com.mobecker.instancio.jpa.api;

import org.instancio.InstancioOfCollectionApi;

public interface InstancioJpaOfCollectionApi<C> extends InstancioJpaApi<C>, InstancioOfCollectionApi<C> {

    @Override
    InstancioJpaOfCollectionApi<C> size(int i);
}
