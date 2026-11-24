package vista;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

final class Tema {
    static final Color TINTA = new Color(26, 37, 34);
    static final Color MUTED = new Color(81, 103, 94);
    static final Color FONDO = new Color(240, 245, 241);
    static final Color LINEA = new Color(217, 228, 222);
    static final Color VERDE = new Color(22, 101, 67);
    static final Color LIMA = new Color(188, 246, 124);

    private Tema() {}

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
}
