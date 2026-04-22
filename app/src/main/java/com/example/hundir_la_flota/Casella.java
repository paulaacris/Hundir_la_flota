package com.example.hundir_la_flota;

// per poder retornal les coordenades calculades
public class Casella {
    public int i, j; // fila, columna

    public Casella(int j, int i) {
        this.j = j;
        this.i = i;
    }

    @Override
    public String toString() {
        return "(" + i + ", " + j + ")";
    }
}
