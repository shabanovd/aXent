package org.exist.agent.transform;

import org.kohsuke.asm5.MethodVisitor;

abstract class MethodTransformSpec {

    final String method_name;

    final String method_signature;

    final boolean skip;

    MethodTransformSpec(String name, String signature, boolean skip) {
        this.method_name = name;
        this.method_signature = signature;

        this.skip = skip;
    }

    public abstract MethodVisitor newAdapter(MethodVisitor base, int access, String name, String desc, String signature, String[] exceptions);
}
