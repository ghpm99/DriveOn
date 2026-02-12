package com.driveon;

// Simples container de dados
public class TouchEvent {
    public float x, y;
    public int action;

    // Pequeno pool para evitar Garbage Collection excessivo no evento de touch
    // (Opcional, mas recomendado para o Tab 2)
}