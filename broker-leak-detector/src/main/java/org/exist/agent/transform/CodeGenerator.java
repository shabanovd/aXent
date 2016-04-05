package org.exist.agent.transform;

import org.kohsuke.asm5.Label;
import org.kohsuke.asm5.MethodVisitor;
import org.kohsuke.asm5.Type;

import static org.kohsuke.asm5.Opcodes.*;

public class CodeGenerator extends MethodVisitor {

    CodeGenerator(MethodVisitor mv) {
        super(ASM5,mv);
    }

    public void println(String msg) {
        super.visitFieldInsn(GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;");
        ldc(msg);
        super.visitMethodInsn(INVOKEVIRTUAL,"java/io/PrintStream","println","(Ljava/lang/String;)V");
    }

    void _null() {
        super.visitInsn(ACONST_NULL);
    }

    void newArray(String type, int size) {
        iconst(size);
        super.visitTypeInsn(ANEWARRAY,type);
    }

    void iconst(int i) {
        if(i<=5)
            super.visitInsn(ICONST_0+i);
        else
            super.visitLdcInsn(i);
    }

    void dup() {
        super.visitInsn(DUP);
    }

    void aastore() {
        super.visitInsn(AASTORE);
    }

    void aload(int i) {
        super.visitIntInsn(ALOAD,i);
    }

    void pop() {
        super.visitInsn(POP);
    }

    void ldc(Object o) {
        if(o.getClass()==Class.class)
            o = Type.getType((Class)o);
        super.visitLdcInsn(o);
    }

    void invokeVirtual(String owner, String name, String desc) {
        super.visitMethodInsn(INVOKEVIRTUAL, owner, name, desc);
    }

    public void invokeAppStatic(Class userClass, String userMethodName, Class[] argTypes, int[] localIndex) {
        invokeAppStatic(userClass.getName(),userMethodName,argTypes,localIndex);
    }

    void invokeAppStatic(String userClassName, String userMethodName, Class[] argTypes, int[] localIndex) {
        Label s = new Label();
        Label e = new Label();
        Label h = new Label();
        Label tail = new Label();
        visitTryCatchBlock(s,e,h,"java/lang/Exception");
        visitLabel(s);
        // [RESULT] m = ClassLoader.getSystemClassLoadeR().loadClass($userClassName).getDeclaredMethod($userMethodName,[...]);
        visitMethodInsn(INVOKESTATIC,"java/lang/ClassLoader","getSystemClassLoader","()Ljava/lang/ClassLoader;");
        ldc(userClassName);
        invokeVirtual("java/lang/ClassLoader","loadClass","(Ljava/lang/String;)Ljava/lang/Class;");
        ldc(userMethodName);
        newArray("java/lang/Class",argTypes.length);
        for (int i = 0; i < argTypes.length; i++)
            storeConst(i, argTypes[i]);

        invokeVirtual("java/lang/Class","getDeclaredMethod","(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");

        // [RESULT] m.invoke(null,new Object[]{this,file})
        _null();
        newArray("java/lang/Object",argTypes.length);

        for (int i = 0; i < localIndex.length; i++) {
            dup();
            iconst(i);
            aload(localIndex[i]);
            aastore();
        }

        visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
        pop();
        _goto(tail);

        visitLabel(e);
        visitLabel(h);

        // [RESULT] catch(e) { e.printStackTrace(System.out); }
        visitFieldInsn(GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;");
        invokeVirtual("java/lang/Exception","printStackTrace","(Ljava/io/PrintStream;)V");

        visitLabel(tail);
    }

    /**
     * When the stack top is an array, store a constant to the known index of the array.
     *
     * ..., array => ..., array
     */
    private void storeConst(int idx, Object type) {
        dup();
        iconst(idx);
        ldc(type);
        aastore();
    }

    void _goto(Label l) {
        visitJumpInsn(GOTO,l);
    }

    public void ifFalse(Label label) {
        visitJumpInsn(IFEQ,label);
    }

    public void athrow() {
        visitInsn(ATHROW);
    }
}
