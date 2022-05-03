package net.bytebuddy.build.gradle;

import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.FieldVisitor;
import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.utility.OpenedClassReader;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyPluginTest {

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    private static final String FOO = "foo";

    private File folder;

    @Before
    public void setUp() throws Exception {
        folder = File.createTempFile("byte-buddy-gradle-plugin", "");
        assertThat(folder.delete(), is(true));
        assertThat(folder.mkdir(), is(true));
    }

    @After
    public void tearDown() throws Exception {
        delete(folder);
    }

    private static void delete(File folder) {
        File[] file = folder.listFiles();
        if (file != null) {
            for (File aFile : file) {
                if (aFile.isDirectory()) {
                    delete(aFile);
                } else {
                    assertThat(aFile.delete(), is(true));
                }
            }
        }
        assertThat(folder.delete(), is(true));
    }

    @Test
    @IntegrationRule.Enforce
    public void testPluginExecution() throws Exception {
        write("build.gradle",
                "plugins {",
                "  id 'java'",
                "  id 'net.bytebuddy.byte-buddy-gradle-plugin'",
                "}",
                "",
                "import net.bytebuddy.build.Plugin;",
                "import net.bytebuddy.description.type.TypeDescription;",
                "import net.bytebuddy.dynamic.ClassFileLocator;",
                "import net.bytebuddy.dynamic.DynamicType;",
                "",
                "class SamplePlugin implements Plugin {",
                "  @Override boolean matches(TypeDescription target) {",
                "    return target.getSimpleName().equals(\"SampleClass\");",
                "  }",
                "  @Override DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, " +
                        "TypeDescription typeDescription, " +
                        "ClassFileLocator classFileLocator) {",
                "    return builder.defineField(\"" + FOO + "\", Void.class);",
                "  }",
                "  @Override void close() { }",
                "}",
                "",
                "byteBuddy {",
                "  transformation {",
                "    plugin = SamplePlugin.class",
                "  }",
                "}");
        write("src/main/java/sample/SampleClass.java", "public class SampleClass { }");
        BuildResult result = GradleRunner.create()
                .withProjectDir(folder)
                .withArguments("build",
                        "-D" + ByteBuddyPlugin.LEGACY + "=true",
                        "-Dorg.gradle.unsafe.configuration-cache=true")
                .withPluginClasspath()
                .build();
        BuildTask task = result.task(":byteBuddy");
        assertThat(task, notNullValue(BuildTask.class));
        assertThat(task.getOutcome(), is(TaskOutcome.SUCCESS));
        assertResult("SampleClass.class", FOO);
        assertThat(result.task(":byteBuddyTest"), nullValue(BuildTask.class));
    }

    @Test
    @IntegrationRule.Enforce
    public void testPluginWithArgumentsExecution() throws Exception {
        write("build.gradle",
                "plugins {",
                "  id 'java'",
                "  id 'net.bytebuddy.byte-buddy-gradle-plugin'",
                "}",
                "",
                "import net.bytebuddy.build.Plugin;",
                "import net.bytebuddy.description.type.TypeDescription;",
                "import net.bytebuddy.dynamic.ClassFileLocator;",
                "import net.bytebuddy.dynamic.DynamicType;",
                "",
                "class SamplePlugin implements Plugin {",
                "  private final String value;",
                "  SamplePlugin(String value) { this.value = value; }",
                "  @Override boolean matches(TypeDescription target) {",
                "    return target.getSimpleName().equals(\"SampleClass\");",
                "  }",
                "  @Override DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, " +
                        "TypeDescription typeDescription, " +
                        "ClassFileLocator classFileLocator) {",
                "    return builder.defineField(value, Void.class);",
                "  }",
                "  @Override void close() { }",
                "}",
                "",
                "byteBuddy {",
                "  transformation {",
                "    plugin = SamplePlugin.class",
                "    argument {",
                "      value = '" + FOO + "'",
                "    }",
                "  }",
                "}");
        write("src/main/java/sample/SampleClass.java", "public class SampleClass { }");
        BuildResult result = GradleRunner.create()
                .withProjectDir(folder)
                .withArguments("build",
                        "-D" + ByteBuddyPlugin.LEGACY + "=true",
                        "-Dorg.gradle.unsafe.configuration-cache=true")
                .withPluginClasspath()
                .build();
        BuildTask task = result.task(":byteBuddy");
        assertThat(task, notNullValue(BuildTask.class));
        assertThat(task.getOutcome(), is(TaskOutcome.SUCCESS));
        assertResult("SampleClass.class", FOO);
        assertThat(result.task(":byteBuddyTest"), nullValue(BuildTask.class));
    }

    private File create(List<String> segments) {
        File folder = this.folder;
        for (String segment : segments.subList(0, segments.size() - 1)) {
            folder = new File(folder, segment);
            assertThat(folder.mkdir() || folder.isDirectory(), is(true));
        }
        return new File(folder, segments.get(segments.size() - 1));
    }

    private void write(String path, String... line) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(create(Arrays.asList(path.split("/")))));
        try {
            for (String aLine : line) {
                writer.println(aLine);
            }
            writer.println();
        } finally {
            writer.close();
        }
    }

    private void assertResult(String name, final String expectation) throws IOException {
        File jar = new File(folder, "build/libs/" + folder.getName() + ".jar");
        assertThat(jar.isFile(), is(true));
        JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jar));
        try {
            JarEntry entry = jarInputStream.getNextJarEntry();
            assertThat(entry, notNullValue(JarEntry.class));
            assertThat(entry.getName(), is(name));
            new ClassReader(jarInputStream).accept(new ClassVisitor(OpenedClassReader.ASM_API) {

                private boolean found;

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    assertThat(name, is(expectation));
                    found = true;
                    return null;
                }

                @Override
                public void visitEnd() {
                    assertThat(found, is(true));
                }
            }, ClassReader.SKIP_CODE);
            jarInputStream.closeEntry();
            assertThat(jarInputStream.getNextJarEntry(), nullValue(JarEntry.class));
        } finally {
            jarInputStream.close();
        }
    }
}