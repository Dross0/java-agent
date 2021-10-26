package ru.gaidamaka.java.agent;

import ru.gaidamaka.java.agent.transform.ClassTransformer;

import java.lang.instrument.Instrumentation;

public class Agent {
    public static void premain(String args, Instrumentation inst){
        System.out.println("Agent started");
        inst.addTransformer(new ClassTransformer());
    }
}
