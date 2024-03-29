/*
 * Copyright 2023 - 2024 Moritz Becker.
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

package com.mobecker.instancio.jpa.selector;

import java.util.Collections;
import java.util.function.Predicate;
import org.instancio.internal.nodes.InternalNode;

abstract class PredicateSelectorImpl extends org.instancio.internal.selectors.PredicateSelectorImpl {
    private static final int PRIORITY = Integer.MAX_VALUE;

    PredicateSelectorImpl(
        final Predicate<InternalNode> nodePredicate, final String apiInvocationDescription
    ) {
        super(PRIORITY, nodePredicate, Collections.emptyList(), null, true, apiInvocationDescription, new Throwable());
    }
}
