package vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import modelo.EstadoGym;
import modelo.GymListener;

abstract class PanelModulo extends JPanel implements GymListener {
    // Componentes comunes
    private final JPanel superior = new JPanel(new BorderLayout(0, 10));
    final JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    EstadoGym estado;

    // Constructor del panel
    PanelModulo(String titulo, String descripcion) {
        super(new BorderLayout(0, 16));
        setBackground(PanelModulo.FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 26, 20, 26));
        JPanel cabecera = new JPanel(new GridLayout(0, 1, 0, 2));
        cabecera.setOpaque(false);
        cabecera.add(PanelModulo.etiqueta(titulo, 24, true, PanelModulo.TINTA));
        cabecera.add(PanelModulo.etiqueta(descripcion, 13, false, PanelModulo.MUTED));
        superior.setOpaque(false);
        acciones.setOpaque(false);
        superior.add(cabecera, BorderLayout.NORTH);
        superior.add(acciones, BorderLayout.SOUTH);
        add(superior, BorderLayout.NORTH);
    }

    // Botones y eventos
    JButton accion(String titulo, boolean principal, Runnable evento) {
        JButton boton = PanelModulo.boton(titulo, principal, evento);
        acciones.add(boton);
        return boton;
    }

    static JLabel detalle(String texto) {
        JLabel etiqueta = PanelModulo.etiqueta(texto, 13, false, PanelModulo.MUTED);
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

    // Mensajes de validación
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
        panel.add(PanelModulo.etiqueta(titulo, 15, true, PanelModulo.TINTA), BorderLayout.NORTH);
        panel.add(contenido, BorderLayout.CENTER);
        return panel;
    }

    // Colores y componentes comunes de la interfaz.

    static final Color TINTA = new Color(26, 37, 34);
    static final Color MUTED = new Color(81, 103, 94);
    static final Color FONDO = new Color(240, 245, 241);
    static final Color LINEA = new Color(217, 228, 222);
    static final Color VERDE = new Color(22, 101, 67);
    static final Color LIMA = new Color(188, 246, 124);


    static JLabel etiqueta(String texto, int puntos, boolean negrita, Color color) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(color);
        etiqueta.setFont(new Font(Font.SANS_SERIF, negrita ? Font.BOLD : Font.PLAIN, puntos));
        return etiqueta;
    }

    static JButton boton(String texto, boolean principal, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.setBackground(principal ? VERDE : Color.WHITE);
        boton.setForeground(principal ? Color.WHITE : VERDE);
        boton.setFocusPainted(false);
        boton.setHorizontalAlignment(SwingConstants.CENTER);
        boton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(principal ? VERDE : LINEA),
                BorderFactory.createEmptyBorder(9, 13, 9, 13)));
        boton.addActionListener(evento -> accion.run());
        return boton;
    }

    // Formulario común para los diálogos de registro y edición.
    static final class Formulario extends JPanel {
        Formulario() {
            super(new GridLayout(0, 2, 12, 9));
            setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        }

        JTextField texto(String nombre, String inicial) {
            JTextField campo = new JTextField(inicial, 20);
            agregar(nombre, campo);
            return campo;
        }

        <T> JComboBox<T> elegir(String nombre, List<T> elementos, T inicial) {
            JComboBox<T> opciones = new JComboBox<>(new Vector<>(elementos));
            if (inicial != null) opciones.setSelectedItem(inicial);
            agregar(nombre, opciones);
            return opciones;
        }

        static <T> T elegido(JComboBox<T> opciones) {
            int indice = opciones.getSelectedIndex();
            return indice < 0 ? null : opciones.getItemAt(indice);
        }

        <T> JList<T> multiples(String nombre, List<T> posibles, List<T> iniciales) {
            JList<T> lista = new JList<>(new Vector<>(posibles));
            lista.setVisibleRowCount(3);
            for (int i = 0; i < posibles.size(); i++) {
                if (iniciales.contains(posibles.get(i))) lista.addSelectionInterval(i, i);
            }
            JScrollPane scroll = new JScrollPane(lista);
            scroll.setPreferredSize(new Dimension(230, 75));
            agregar(nombre, scroll);
            return lista;
        }

        private void agregar(String nombre, JComponent campo) {
            JLabel etiqueta = new JLabel(nombre);
            etiqueta.setLabelFor(campo);
            add(etiqueta);
            add(campo);
        }

        boolean aceptar(Component padre, String titulo) {
            return JOptionPane.showConfirmDialog(padre, this, titulo, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
        }
    }
}
