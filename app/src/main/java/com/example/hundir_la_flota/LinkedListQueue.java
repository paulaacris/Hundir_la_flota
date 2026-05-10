package com.example.hundir_la_flota;

public class LinkedListQueue <E> {
    private class Node {
        private E elem;
        private Node next;

        public Node(E elem, Node next) {
            this.elem = elem;
            this.next = next;
        }
    }
    private Node q, p;

    public LinkedListQueue() {
        this.q = null;
        this.p = null;
    }

    /* añade un elemento a la cola
    devuelve true si se ha podido añadir ele elemento */
    public boolean put (E item) {
        try {
            Node r = new Node(item, null);

            // caso particular de cola vacia
            if (isEmpty()) {
                // el primero y el ultimo apuntan al mismo nodo (el unico)
                q = r;
                p = r;
            } else {
                p.next = r; // el siguiente del anterior es el nuevo
                p = r; // el nuevo ahora es el ultimo
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* elimina el primer elemento de la cola
    devuelve true si se ha podido eliminar */
    public boolean removeFirst () {
        if (isEmpty()) {
            // si la cola esta vacia no hay nada que borrar
            return false;
        } else {
            q = q.next; // el nuevo primero es el siguiente
            if (isEmpty()) { // si quitando el primer elemento la pila se ha quedado vacia
                p = null;
            }
            return true;
        }
    }

    /* devuelve el primer elemento de la cola, si hay, o null */
    public E getFirst () {
        if (isEmpty()) {
            // si la cola esta vacia no hay nada que devolver
            return null;
        } else {
            return q.elem;
        }
    }

    /* comprueba si un elemento ya esta en la cola */
    public boolean contains (E item) {
        Node actual = q; // empezamos por el primer nodo

        while (actual != null) { // recorremos hasta el final de la cadena
            if (actual.elem.equals(item)) {
                return true; // lo hemos encontrado
            }
            actual = actual.next; // pasamos al siguiente
        }
        return false;
    }

    /* devuelve si la cola esta vacia */
    public boolean isEmpty () {
        return q == null;
    }

    /* vacia la cola */
    public void empty () {
        q = null;
        p = null;
    }
}