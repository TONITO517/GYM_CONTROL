package vista;

import controlador.GymControlador;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import modelo.EstadoGym;
import modelo.Membresia;

final class PanelResumen extends PanelModulo {
    private final JLabel[] valores = new JLabel[9];
    private final Tabla<modelo.Sede> sedes = new Tabla<>(Tabla.texto("Sede", elemento -> elemento.getNombre()),
            Tabla.numero("Personas dentro", elemento -> elemento.getPresentes()), Tabla.numero("Capacidad", elemento -> elemento.getCapacidad()),
            Tabla.numero("Espacios libres", elemento -> elemento.disponibles()));

    // Constructor del resumen
    PanelResumen(GymControlador controlador, Runnable exportar) {
        super("Resumen del gimnasio", "Consulta socios, vencimientos, pagos y ocupación por sede.");
        accion("Exportar asistencias", true, exportar);
        accion("Actualizar", false, () -> controlador.actualizar());

        JPanel cuerpo = new JPanel(new BorderLayout(0, 16));
        cuerpo.setOpaque(false);
        JPanel indicadores = new JPanel(new GridLayout(3, 3, 12, 11));
        indicadores.setOpaque(false);
        String[] nombres = {"Socios registrados", "Socios activos", "Socios suspendidos",
                "Membresías vigentes", "Por vencer", "Membresías congeladas",
                "Membresías con saldo", "Personas dentro", "Clases programadas"};
        for (int i = 0; i < nombres.length; i++) {
            JPanel tarjeta = new JPanel(new GridLayout(2, 1, 0, 2));
            tarjeta.setBackground(Color.WHITE);
            tarjeta.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PanelModulo.LINEA),
                    BorderFactory.createEmptyBorder(9, 15, 9, 15)));
            tarjeta.add(PanelModulo.etiqueta(nombres[i], 13, true, PanelModulo.MUTED));
            valores[i] = PanelModulo.etiqueta("—", 26, true, PanelModulo.VERDE);
            tarjeta.add(valores[i]);
            indicadores.add(tarjeta);
        }
        cuerpo.add(indicadores, BorderLayout.NORTH);
        cuerpo.add(seccion("Aforo por sede", sedes), BorderLayout.CENTER);
        add(cuerpo, BorderLayout.CENTER);
    }

    // Observer: actualización de indicadores
    @Override
    public void gymActualizado(EstadoGym estado) {
        this.estado = estado;
        long porVencer = estado.porVigencia(Membresia.Vigencia.POR_VENCER);
        long activos = estado.activos();
        long[] cifras = {estado.getSocios().size(), activos, estado.getSocios().size() - activos,
                estado.porVigencia(Membresia.Vigencia.VIGENTE) + porVencer, porVencer,
                estado.porVigencia(Membresia.Vigencia.CONGELADA), estado.pagosPendientes(),
                estado.presentes(), estado.clasesProgramadas()};
        for (int i = 0; i < cifras.length; i++) valores[i].setText(Long.toString(cifras[i]));
        sedes.mostrar(estado.getSedes(), elemento -> elemento.getId());
    }
}
