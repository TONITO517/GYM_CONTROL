package modelo.comando;

import util.PilaGenerica;
import modelo.Validador;

public final class GestorComandos {
    private final PilaGenerica<Comando> deshacer = new PilaGenerica<>();
    private final PilaGenerica<Comando> rehacer = new PilaGenerica<>();

    public void ejecutar(Comando comando) {
        comando.ejecutar();
        deshacer.apilar(comando);
        rehacer.limpiar();
    }

    public boolean puedeDeshacer() { return !deshacer.vacia(); }
    public boolean puedeRehacer() { return !rehacer.vacia(); }

    public String deshacer() {
        Validador.exigir(puedeDeshacer(), "No hay cambios que deshacer.");
        Comando comando = deshacer.cima();
        comando.deshacer();
        rehacer.apilar(deshacer.desapilar());
        return comando.descripcion();
    }

    public String rehacer() {
        Validador.exigir(puedeRehacer(), "No hay cambios que rehacer.");
        Comando comando = rehacer.cima();
        comando.ejecutar();
        deshacer.apilar(rehacer.desapilar());
        return comando.descripcion();
    }

    public void descartarRehacer() {
        rehacer.limpiar();
    }

    public void limpiar() {
        deshacer.limpiar();
        rehacer.limpiar();
    }
}
