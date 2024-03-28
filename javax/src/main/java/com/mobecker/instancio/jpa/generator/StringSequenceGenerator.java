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

package com.mobecker.instancio.jpa.generator;

import org.instancio.Random;
import org.instancio.internal.generator.AbstractGenerator;
import org.instancio.internal.generator.sequence.LongSequenceGenerator;
import org.instancio.support.Global;

/**
 * A generator for {@link String} sequences based on {@link LongSequenceGenerator}.
 *
 * @since 1.1.0
 */
public class StringSequenceGenerator extends AbstractGenerator<String> {
    private final LongSequenceGenerator longSequenceGenerator = new LongSequenceGenerator();

    /**
     * Creates a new {@link StringSequenceGenerator}.
     */
    public StringSequenceGenerator() {
        super(Global.generatorContext());
    }

    @Override
    public String apiMethod() {
        return "stringSeq()";
    }

    @Override
    protected String tryGenerateNonNull(Random random) {
        Long nextVal = longSequenceGenerator.generate(random);
        return nextVal == null ? null : nextVal.toString();
    }
}
