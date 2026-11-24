package vista;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

final class Formulario extends JPanel {
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
