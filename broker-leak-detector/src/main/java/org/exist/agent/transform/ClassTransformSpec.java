package org.exist.agent.transform;

import java.util.HashMap;
import java.util.Map;

public class ClassTransformSpec {

    final String name;
    Map<String,MethodTransformSpec> methodSpecs = new HashMap<String,MethodTransformSpec>();

    public <T extends MethodTransformSpec> ClassTransformSpec(String name, T... methodSpecs) {
        this.name = name;
        for (T s : methodSpecs) {
            this.methodSpecs.put(s.method_name + s.method_signature, s);
        }
    }
}
