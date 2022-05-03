package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class ClassFilePostProcessorTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassFileTransformer classFileTransformer;

    @Test
    public void testNoOpPostProcessor() throws Exception {
        byte[] transformed = ClassFilePostProcessor.NoOp.INSTANCE.transform(null, "foo.Bar", null, new byte[]{1, 2, 3});
        assertThat(Arrays.equals(transformed, new byte[]{1, 2, 3}), is(true));
    }

    @Test
    public void testClassFileTransformerPostProcessor() throws Exception {
        when(classFileTransformer.transform(null, "foo/Bar", null, ClassFilePostProcessor.ForClassFileTransformer.ALL_PRIVILEGES, new byte[]{1, 2, 3})).thenReturn(new byte[]{4, 5, 6});
        byte[] transformed = new ClassFilePostProcessor.ForClassFileTransformer(classFileTransformer).transform(null, "foo.Bar", null, new byte[]{1, 2, 3});
        assertThat(Arrays.equals(transformed, new byte[]{4, 5, 6}), is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void testClassFileTransformerPostProcessorWithException() throws Exception {
        when(classFileTransformer.transform(null, "foo/Bar", null, ClassFilePostProcessor.ForClassFileTransformer.ALL_PRIVILEGES, new byte[]{1, 2, 3})).thenThrow(new IllegalClassFormatException());
        new ClassFilePostProcessor.ForClassFileTransformer(classFileTransformer).transform(null, "foo.Bar", null, new byte[]{1, 2, 3});
    }
}
