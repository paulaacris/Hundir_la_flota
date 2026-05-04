package com.example.hundir_la_flota;

/**
 * Implementació d'un conjunt com un array d’elements no ordenats
 */
public class UnsortedAarraySet <E> {
    private E[] array;
    private int n;

    /**
     * Prepara l’array per emmagatzemar el conjunt.
     * Reserva memòria per un array de longitud max i inicialitza n a 0.
     * @param max
     */
    public UnsortedAarraySet(int max){
        array = (E[]) new Object[max];
        n = 0;
    }

    /**
     * Consultar si el conjunt conté l’element.
     * Costa O(n): cerca lineal
     * @param elem
     * @return
     */
    public boolean contains(E elem) {
        int i = 0;
        boolean trobat = false;
        while (!trobat && i < n) {
            trobat = elem.equals(array[i]);
            i++;
        }
        return trobat;
    }

    /**
     * Afegir un element al conjunt (si encara no es troba present).
     * Costa O(n): abans d’afegir, comprova que la clau no existeix dins el conjunt (cerca lineal).
     * @param elem
     * @return true si s'ha canviat el conjunt
     */
    public boolean add(E elem) {
        if (n < array.length && !contains(elem)) {
            array[n] = elem;
            n++;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Eliminar un element del conjunt (si es troba present).
     * Costa O(n): cal cercar la clau dins el conjunt (cerca lineal) i el darrer element ha de
     * situar-se a la posició alliberada.
     * @param elem
     * @return true si s'ha canviat el conjunt
     */
    public boolean remove(E elem) {
        int i = 0;
        boolean trobat = false;
        while (!trobat && i < n) {
            trobat = elem.equals(array[i]);
            i++;
        }
        if (trobat) {
            array[i-1] = array[n-1];
            n--;
        }
        return trobat;
    }

    /**
     * Consultar si el conjunt es troba buit.
     * Costa O(1).
     * @return
     */
    public boolean isEmpty() {
        return n == 0;
    }

    /**
     * Classe Iterator per poder recorrer el conjunt
     * @return
     */
    public Iterator iterator() {
        Iterator it = new IteratorUnsortedArraySet();
        return it;
    }

    private class IteratorUnsortedArraySet implements Iterator {
        private int idxIterator;

        private IteratorUnsortedArraySet() {
            idxIterator = 0;
        }

        @Override
        public boolean hasNext() {
            return idxIterator < n;
        }

        @Override
        public Object next() {
            idxIterator++;
            return array[idxIterator-1];
        }
    }
}