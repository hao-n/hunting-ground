package net.bytebuddy.matcher;

import org.junit.Test;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class NullMatcherTest extends AbstractElementMatcherTest<NullMatcher<?>> {

    @SuppressWarnings("unchecked")
    public NullMatcherTest() {
        super((Class<NullMatcher<?>>) (Object) NullMatcher.class, "isNull");
    }

    @Test
    public void testMatch() throws Exception {
        assertThat(new NullMatcher<Object>().matches(null), is(true));
    }

    @Test
    public void testPositiveToNegative() throws Exception {
        assertThat(new NullMatcher<Object>().matches(new Object()), is(false));
    }

    @Test
    public void testSingletonIsEquivalentToNewInstance() {
        assertThat(NullMatcher.make(), hasPrototype((ElementMatcher.Junction<Object>) new NullMatcher<Object>()));
    }
}
