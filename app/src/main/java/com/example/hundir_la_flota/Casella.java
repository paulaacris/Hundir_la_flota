package com.example.hundir_la_flota;

import java.util.Objects;

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

    // para que se pueda comparar si dos casillas son iguales
    // para poder usarlo como clave del mapping
    @Override
    public boolean equals (Object o) {
        if (this == o) return true; // si son el mismo objeto (misma posicion en la memoria) son iguales
        if (o == null || getClass() != o.getClass()) return false; // si el objeto no existe, o no son ni de la misma clase, no son iguales
        // aqui ya sabemos que o es una Casella
        Casella casella = (Casella) o;
        return i == casella.i && j == casella.j; // si dos casillas tienen la misma i,j son la misma
    }

    @Override
    public int hashCode() {
        return Objects.hash(i, j);
    }
}
