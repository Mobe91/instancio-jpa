package com.mobecker.instancio.jpa.selector;

import java.util.function.Predicate;
import org.instancio.internal.nodes.InternalNode;

public abstract class PredicateSelectorImpl extends org.instancio.internal.selectors.PredicateSelectorImpl {
    private static final int PRIORITY = Integer.MAX_VALUE;

    protected PredicateSelectorImpl(
        final Predicate<InternalNode> nodePredicate, final String apiInvocationDescription
    ) {
        super(PRIORITY, nodePredicate, null, apiInvocationDescription, new Throwable());
    }
}
