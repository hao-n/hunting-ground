package net.bytebuddy.agent;

import net.bytebuddy.dynamic.ClassFileLocator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class VirtualMachineAttachmentTest {

    private static final String FOO = "foo";

    private File agent;

    @Before
    public void setUp() throws Exception {
        agent = File.createTempFile("testagent", ".jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        manifest.getMainAttributes().putValue("Agent-Class", SampleAgent.class.getName());
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(agent), manifest);
        try {
            outputStream.putNextEntry(new JarEntry(SampleAgent.class.getName().replace('.', '/') + ".class"));
            outputStream.write(ClassFileLocator.ForClassLoader.read(SampleAgent.class));
            outputStream.closeEntry();
        } finally {
            outputStream.close();
        }
    }

    @After
    public void tearDown() throws Exception {
        SampleAgent.argument = null;
    }

    @Test(timeout = 10000L)
    public void testAttachment() throws Exception {
        assertThat(SampleAgent.argument, nullValue(String.class));
        VirtualMachine virtualMachine = (VirtualMachine) VirtualMachine.Resolver.INSTANCE.run()
                .getMethod("attach", String.class)
                .invoke(null, ByteBuddyAgent.ProcessProvider.ForCurrentVm.INSTANCE.resolve());
        try {
            virtualMachine.loadAgent(agent.getAbsolutePath(), FOO);
        } finally {
            virtualMachine.detach();
        }
        assertThat(SampleAgent.argument, is(FOO));
    }

    @Test(timeout = 10000L)
    public void testSystemProperties() throws Exception {
        VirtualMachine virtualMachine = (VirtualMachine) VirtualMachine.Resolver.INSTANCE.run()
                .getMethod("attach", String.class)
                .invoke(null, ByteBuddyAgent.ProcessProvider.ForCurrentVm.INSTANCE.resolve());
        Properties properties;
        try {
            properties = virtualMachine.getSystemProperties();
        } finally {
            virtualMachine.detach();
        }
        assertThat(properties.size(), not(0));
    }

    @Test(timeout = 10000L)
    public void testAgentProperties() throws Exception {
        VirtualMachine virtualMachine = (VirtualMachine) VirtualMachine.Resolver.INSTANCE.run()
                .getMethod("attach", String.class)
                .invoke(null, ByteBuddyAgent.ProcessProvider.ForCurrentVm.INSTANCE.resolve());
        Properties properties;
        try {
            properties = virtualMachine.getAgentProperties();
        } finally {
            virtualMachine.detach();
        }
        assertThat(properties.size(), not(0));
    }

    @Test(timeout = 10000L)
    public void testMultipleProperties() throws Exception {
        VirtualMachine virtualMachine = (VirtualMachine) VirtualMachine.Resolver.INSTANCE.run()
                .getMethod("attach", String.class)
                .invoke(null, ByteBuddyAgent.ProcessProvider.ForCurrentVm.INSTANCE.resolve());
        Properties agentProperties, systemProperties;
        try {
            agentProperties = virtualMachine.getAgentProperties();
            systemProperties = virtualMachine.getSystemProperties();
        } finally {
            virtualMachine.detach();
        }
        assertThat(agentProperties, not(systemProperties));
    }
}
