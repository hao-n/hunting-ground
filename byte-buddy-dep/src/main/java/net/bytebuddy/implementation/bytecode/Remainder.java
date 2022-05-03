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
package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.implementation.Implementation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack manipulation that computes the remainder two numbers on the operand stack.
 */
public enum Remainder implements StackManipulation {

    /**
     * Computes the remainder of two integers or integer-compatible values.
     */
    INTEGER(Opcodes.IREM, StackSize.SINGLE),

    /**
     * Computes the remainder of two longs.
     */
    LONG(Opcodes.LREM, StackSize.DOUBLE),

    /**
     * Computes the remainder of two floats.
     */
    FLOAT(Opcodes.FREM, StackSize.SINGLE),

    /**
     * Computes the remainder of two doubles.
     */
    DOUBLE(Opcodes.DREM, StackSize.DOUBLE);

    /**
     * The opcode to apply.
     */
    private final int opcode;

    /**
     * The stack size of the remainder primitive.
     */
    private final StackSize stackSize;

    /**
     * Creates a new remainder type.
     *
     * @param opcode    The opcode to apply.
     * @param stackSize The stack size of the remainder primitive.
     */
    Remainder(int opcode, StackSize stackSize) {
        this.opcode = opcode;
        this.stackSize = stackSize;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValid() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitInsn(opcode);
        return stackSize.toDecreasingSize();
    }
}
