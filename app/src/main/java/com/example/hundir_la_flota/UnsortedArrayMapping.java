package com.example.hundir_la_flota;

public class UnsortedArrayMapping <K, V> {
    private K[] claus;
    private V[] valors;
    private int n;

    // Prepara els arrays per emmagatzemar el mapping
    public UnsortedArrayMapping(int max){
        this.claus = (K[]) new Object[max];
        this.valors = (V[]) new Object[max];
        this.n = 0;
    }

    // Consultar el valor associat a la clau
    public V get(K key) {
        for (int i = 0; i < n; i++) {
            if (claus[i].equals(key)) {
                return valors[i];
            }
        }
        return null; // si la clave no existe
    }

    // Afegir una parella clau-valor
    // Retorna el valor anterior associat a la clau, si n’hi havia, o null
    public V put(K key, V value) {
        // busca si la clave ya existia para actualizarle el valor
        for (int i = 0; i < n; i++) {
            if (claus[i].equals(key)) {
                V valorAnterior = valors[i];
                valors[i] = value;
            }
        }

        // si la clave no existia comprovamos que haya espacio
        if (n >= claus.length) {
            return null;
        }

        // añadimos l anueva clave-valor al final
        claus[n] = key;
        valors[n] = value;
        n++;

        return null; // como no habia valor anterior devolvemos null
    }

    // Eliminar l’associació d’una clau
    // Retorna el valor associat a la clau, si n’hi havia, o null
    public V remove(K key) {
        for (int i = 0; i < n; i++) {
            if (claus[i].equals(key)) {
                V valorAnterior = valors[i];

                // para no tener que hacer un desplazamiento
                // ponemos el ultimo elemento en la posicion que vamos a eliminar
                claus[i] = claus[n-1];
                valors[i] = valors[n-1];

                // limpiamos la ultima posicion
                claus[n-1] = null;
                valors[n-1] = null;

                n--;
                return valorAnterior;
            }
        }
        return null; // si no habia clave devuelve null
    }

    // Consultar si el mapping es troba buit
    public boolean isEmpty() {
        return n == 0;
    }

    /**
     * Classe Iterator per poder recorrer les claus del mapping
     */
    public Iterator iterator() {
        return new IteratorUnsortedArrayMapping();
    }

    private class IteratorUnsortedArrayMapping implements Iterator {
        private int idxIterator;

        private IteratorUnsortedArrayMapping() {
            idxIterator = 0; // Empezamos en la posición 0
        }

        @Override
        public boolean hasNext() {
            // Mientras el índice sea menor que la cantidad de elementos 'n' de tu mapping
            return idxIterator < n;
        }

        @Override
        public Object next() {
            idxIterator++;
            // A diferencia del Set que devolvía 'array', aquí devolvemos el array 'claus'
            return claus[idxIterator - 1];
        }
    }
}
