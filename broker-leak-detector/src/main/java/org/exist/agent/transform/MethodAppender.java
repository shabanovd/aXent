package org.exist.agent.transform;

import org.kohsuke.asm5.MethodVisitor;

import static org.kohsuke.asm5.Opcodes.*;

public abstract class MethodAppender extends MethodTransformSpec {

    public MethodAppender(String name, String signature, boolean skip) {
        super(name, signature, skip);
    }

    /**
     * Generates code to be appended right before the return statement.
     */
    protected abstract void append(CodeGenerator g);

    @Override
    public MethodVisitor newAdapter(MethodVisitor base, int access, String name, String desc, String signature, String[] exceptions) {
        final CodeGenerator cg = new CodeGenerator(base);
        return new MethodVisitor(ASM5,base) {
            @Override
            public void visitInsn(int opcode) {
                if(opcode==RETURN)
                    append(cg);
                super.visitInsn(opcode);
            }
        };
    }
}
