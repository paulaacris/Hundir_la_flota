package com.example.hundir_la_flota;

public class Vaixell {
    int mida;
    int orientacio; // 0: horizontal, 1: vertical
    int color;
    int id;
    int jugador; // 1 o 2

    public Vaixell(int mida, int orientacio, int color, int id, int jugador) {
        this.mida = mida;
        this.orientacio = orientacio;
        this.color = color;
        this.id = id;
        this.jugador = jugador;
    }
}
