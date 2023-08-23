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
package com.mobecker.instancio.jpa.testsuite;

import static org.assertj.core.api.Assertions.assertThat;

import com.mobecker.instancio.jpa.util.JpaMetamodelUtil;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class JpaMetamodelUtilTest {

    private static List<Member> owningMembers() {
        List<Member> members = new ArrayList<>();
        try {
            members.add(Members.class.getMethod("getOneToOneField"));
            members.add(Members.class.getMethod("getOneToManyField"));
            members.add(Members.class.getMethod("getManyToManyField"));

            members.add(Members.class.getDeclaredField("oneToOneField"));
            members.add(Members.class.getDeclaredField("oneToManyField"));
            members.add(Members.class.getDeclaredField("manyToManyField"));
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return members;
    }

    private static List<Member> ownedMembers() {
        List<Member> members = new ArrayList<>();
        try {
            members.add(Members.class.getMethod("getOwnedOneToOneField"));
            members.add(Members.class.getMethod("getOwnedOneToManyField"));
            members.add(Members.class.getMethod("getOwnedManyToManyField"));

            members.add(Members.class.getDeclaredField("ownedOneToOneField"));
            members.add(Members.class.getDeclaredField("ownedOneToManyField"));
            members.add(Members.class.getDeclaredField("ownedManyToManyField"));
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return members;
    }

    @ParameterizedTest
    @MethodSource("owningMembers")
    void resolveMappedBy(Member member) {
        // When
        String resolvedMappedBy = JpaMetamodelUtil.resolveMappedBy(member);

        // Then
        assertThat(resolvedMappedBy).isNull();
    }

    @ParameterizedTest
    @MethodSource("ownedMembers")
    void resolveOwnedMappedBy(Member member) {
        // When
        String resolvedMappedBy = JpaMetamodelUtil.resolveMappedBy(member);

        // Then
        assertThat(resolvedMappedBy).isEqualTo(Members.OWNER);
    }

    private static class Members {
        private static final String OWNER = "owner";

        @OneToOne
        private String oneToOneField;
        @OneToMany
        private String oneToManyField;
        @ManyToMany
        private String manyToManyField;

        @OneToOne(mappedBy = OWNER)
        private String ownedOneToOneField;
        @OneToMany(mappedBy = OWNER)
        private String ownedOneToManyField;
        @ManyToMany(mappedBy = OWNER)
        private String ownedManyToManyField;

        @OneToOne
        public String getOneToOneField() {
            return oneToOneField;
        }

        @OneToMany
        public String getOneToManyField() {
            return oneToManyField;
        }

        @ManyToMany
        public String getManyToManyField() {
            return manyToManyField;
        }

        @OneToOne(mappedBy = OWNER)
        public String getOwnedOneToOneField() {
            return ownedOneToOneField;
        }

        @OneToMany(mappedBy = OWNER)
        public String getOwnedOneToManyField() {
            return ownedOneToManyField;
        }

        @ManyToMany(mappedBy = OWNER)
        public String getOwnedManyToManyField() {
            return ownedManyToManyField;
        }
    }
}
