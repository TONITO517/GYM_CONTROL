package vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import modelo.EstadoGym;
import modelo.GymListener;

abstract class PanelModulo extends JPanel implements GymListener {
    private final JPanel superior = new JPanel(new BorderLayout(0, 10));
    final JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    EstadoGym estado;

    PanelModulo(String titulo, String descripcion) {
        super(new BorderLayout(0, 16));
        setBackground(Tema.FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 26, 20, 26));
        JPanel cabecera = new JPanel(new GridLayout(0, 1, 0, 2));
        cabecera.setOpaque(false);
        cabecera.add(Tema.etiqueta(titulo, 24, true, Tema.TINTA));
        cabecera.add(Tema.etiqueta(descripcion, 13, false, Tema.MUTED));
        superior.setOpaque(false);
        acciones.setOpaque(false);
        superior.add(cabecera, BorderLayout.NORTH);
        superior.add(acciones, BorderLayout.SOUTH);
        add(superior, BorderLayout.NORTH);
    }

    JButton accion(String titulo, boolean principal, Runnable evento) {
        JButton boton = Tema.boton(titulo, principal, evento);
        acciones.add(boton);
        return boton;
    }

    static JLabel detalle(String texto) {
        JLabel etiqueta = Tema.etiqueta(texto, 13, false, Tema.MUTED);
        etiqueta.setBackground(Color.WHITE);
        return etiqueta;
    }

    <T> T seleccion(Tabla<T> tabla, String elemento) {
        T seleccionado = tabla.seleccion();
        if (seleccionado == null) {
            aviso("Selecciona " + elemento + " en la tabla.");
        }
        return seleccionado;
    }

    boolean disponible(boolean condicion, String mensaje) {
        if (!condicion) aviso(mensaje);
        return condicion;
    }

    void aviso(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "GYM CONTROL", JOptionPane.INFORMATION_MESSAGE);
    }

    static JPanel seccion(String titulo, Component contenido) {
        JPanel panel = new JPanel(new BorderLayout(0, 7));
        panel.setOpaque(false);
        panel.add(Tema.etiqueta(titulo, 15, true, Tema.TINTA), BorderLayout.NORTH);
        panel.add(contenido, BorderLayout.CENTER);
        return panel;
    }
}
