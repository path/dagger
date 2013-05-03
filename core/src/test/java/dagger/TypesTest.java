/*
 * Copyright (C) 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dagger;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

@RunWith(JUnit4.class)
public final class TypesTest {
  @Test public void providePrimitive() {
    class EntryPoint {
      @Inject int i;
    }

    @Module(injects = EntryPoint.class)
    class TestModule {
      @Provides int provideInt() {
        return 1;
      }
    }

    EntryPoint entryPoint = new EntryPoint();
    ObjectGraph.create(new TestModule()).inject(entryPoint);
    assertThat(entryPoint.i).isEqualTo(1);
  }

  @Test public void getInstanceOfPrimitive() {
    @Module(injects = int.class)
    class TestModule {
      @Provides int provideInt() {
        return 1;
      }
    }

    ObjectGraph graph = ObjectGraph.create(new TestModule());
    assertThat(graph.get(int.class)).isEqualTo(1);
  }

  @Test public void providePrimitiveArray() {
    class EntryPoint {
      @Inject int[] intArray;
    }

    @Module(injects = EntryPoint.class)
    class TestModule {
      @Provides int[] provideIntArray() {
        return new int[0];
      }
    }

    EntryPoint entryPoint = new EntryPoint();
    ObjectGraph.create(new TestModule()).inject(entryPoint);
    assertThat(entryPoint.intArray).isNotNull();
  }

  @Test public void getInstanceOfArray() {
    @Module(injects = int[].class)
    class TestModule {
      @Provides int[] provideIntArray() {
        return new int[0];
      }
    }

    ObjectGraph graph = ObjectGraph.create(new TestModule());
    assertThat(graph.get(int[].class)).isNotNull();
  }

  @Test public void provideParameterizedType() {
    class EntryPoint {
      @Inject List<String> listOfString;
    }

    @Module(injects = EntryPoint.class)
    class TestModule {
      @Provides List<String> provideListOfString() {
        return Arrays.asList();
      }
    }

    EntryPoint entryPoint = new EntryPoint();
    ObjectGraph.create(new TestModule()).inject(entryPoint);
    assertThat(entryPoint.listOfString).isNotNull();
  }

  @Test public void provideWithTypeParameter() {
    @Module
    class TestModule {
      @Provides <T> List<T> provideListOfString() {
        throw new AssertionError();
      }
    }

    try {
      ObjectGraph.create(new TestModule());
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected.getMessage()).contains("TestModule.provideListOfString");
    }
  }

  @Test public void provideTypeWithWildcard() {
    @Module
    class TestModule {
      @Provides List<?> provideListOfAny() {
        return Arrays.<Object>asList();
      }
    }

    try {
      ObjectGraph.create(new TestModule());
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected.getMessage()).contains("TestModule.provideListOfAny");
    }
  }

  static class Parameterized<T> {
    @Inject String string;
  }

  @Test public void noConstructorInjectionsForClassesWithTypeParameters() {
    class TestEntryPoint {
      @Inject Parameterized<Long> parameterized;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides String provideString() {
        return "injected";
      }
    }

    ObjectGraph graph = ObjectGraph.create(new TestModule());
    try {
      graph.validate();
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected.getMessage()).contains("No binding for " + Parameterized.class.getName());
    }
  }

  @Test public void supertypeIsParameterized() {
    class Supertype<T> {
      @Inject String s;
    }

    class Subtype extends Supertype<Integer> {
    }

    @Module(injects = Subtype.class)
    class TestModule {
      @Provides String provideString() {
        return "a";
      }
    }

    Subtype subtype = new Subtype();
    ObjectGraph.create(new TestModule()).inject(subtype);
    assertThat(subtype.s).isEqualTo("a");
  }

  @Test public void injectWithTypeParameter() {
    class Supertype<T> {
      @Inject T t;
    }

    class Subtype extends Supertype<String> {
    }

    @Module(injects = Subtype.class)
    class TestModule {
      @Provides String provideString() {
        throw new AssertionError();
      }
    }

    ObjectGraph graph = ObjectGraph.create(new TestModule()); // Deferred error!
    try {
      graph.inject(new Subtype());
      fail();
    } catch (IllegalStateException expected) {
      assertThat(expected.getMessage()).contains("Uninjectable type T").contains("Supertype.t");
    }
  }
}
