package modelo.estrategia;

import java.time.LocalDate;
import modelo.Membresia;

public interface EstrategiaPrecio {
    final class Contexto {
        private final int precioBase;
        private final boolean primeraMembresia;
        private final Membresia anterior;
        private final LocalDate hoy;

        public Contexto(int precioBase, boolean primeraMembresia, Membresia anterior, LocalDate hoy) {
            this.precioBase = precioBase;
            this.primeraMembresia = primeraMembresia;
            this.anterior = anterior;
            this.hoy = hoy;
        }

        public int getPrecioBase() {
            return precioBase;
        }

        public boolean esPrimeraMembresia() {
            return primeraMembresia;
        }

        public Membresia getAnterior() {
            return anterior;
        }

        public LocalDate getHoy() {
            return hoy;
        }
    }

    String nombre();
    int calcular(Contexto contexto);
}
