package modelo.estrategia;

import java.time.LocalDate;
import modelo.Membresia;

// Strategy: interfaz de precios
public interface EstrategiaPrecio {
    // Datos para el cálculo de la tarifa
    final class Contexto {
        private final int precioBase;
        private final boolean primeraMembresia;
        private final Membresia anterior;
        private final LocalDate hoy;

        // Constructor del contexto
        public Contexto(int precioBase, boolean primeraMembresia, Membresia anterior, LocalDate hoy) {
            this.precioBase = precioBase;
            this.primeraMembresia = primeraMembresia;
            this.anterior = anterior;
            this.hoy = hoy;
        }

        // Getters del contexto
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

    // Métodos de las estrategias
    String nombre();
    int calcular(Contexto contexto);
}
