/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.utility;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.utility.privilege.GetSystemPropertyAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.security.PrivilegedAction;

/**
 * A {@link ClassReader} that does not apply a class file version check if the {@code net.bytebuddy.experimental} property is set.
 */
public class OpenedClassReader {

    /**
     * Indicates that Byte Buddy should not validate the maximum supported class file version.
     */
    public static final String EXPERIMENTAL_PROPERTY = "net.bytebuddy.experimental";

    /**
     * {@code true} if Byte Buddy is executed in experimental mode.
     */
    public static final boolean EXPERIMENTAL;

    /**
     * Indicates the ASM API version that is used throughout Byte Buddy.
     */
    public static final int ASM_API;

    /*
     * Checks the experimental property.
     */
    static {
        boolean experimental;
        try {
            experimental = Boolean.parseBoolean(doPrivileged(new GetSystemPropertyAction(EXPERIMENTAL_PROPERTY)));
        } catch (Exception ignored) {
            experimental = false;
        }
        EXPERIMENTAL = experimental;
        ASM_API = Opcodes.ASM9;
    }

    /**
     * Not intended for construction.
     */
    private OpenedClassReader() {
        throw new UnsupportedOperationException("This class is a utility class and not supposed to be instantiated");
    }

    /**
     * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
     *
     * @param action The action to execute from a privileged context.
     * @param <T>    The type of the action's resolved value.
     * @return The action's resolved value.
     */
    @AccessControllerPlugin.Enhance
    private static <T> T doPrivileged(PrivilegedAction<T> action) {
        return action.run();
    }

    /**
     * Creates a class reader for the given binary representation of a class file.
     *
     * @param binaryRepresentation The binary representation of a class file to read.
     * @return An appropriate class reader.
     */
    public static ClassReader of(byte[] binaryRepresentation) {
        ClassFileVersion classFileVersion = ClassFileVersion.ofClassFile(binaryRepresentation), latest = ClassFileVersion.latest();
        if (classFileVersion.isGreaterThan(latest)) {
            if (EXPERIMENTAL) {
                binaryRepresentation[6] = (byte) (latest.getMajorVersion() >>> 8);
                binaryRepresentation[7] = (byte) latest.getMajorVersion();
                ClassReader classReader = new ClassReader(binaryRepresentation);
                binaryRepresentation[6] = (byte) (classFileVersion.getMajorVersion() >>> 8);
                binaryRepresentation[7] = (byte) classFileVersion.getMajorVersion();
                return classReader;
            } else {
                throw new IllegalArgumentException(classFileVersion
                        + " is not supported by the current version of Byte Buddy which officially supports " + latest
                        + " - update Byte Buddy or set " + EXPERIMENTAL_PROPERTY + " as a VM property");
            }
        } else {
            return new ClassReader(binaryRepresentation);
        }
    }
}
