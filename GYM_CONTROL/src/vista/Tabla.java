package vista;

import java.awt.Color;
import java.awt.Component;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

final class Tabla<T> extends JScrollPane {
    private final JTable tabla;
    private final ModeloTabla modelo;
    private final TableRowSorter<ModeloTabla> orden;

    // Interfaz para valores de columna
    interface ValorColumna<T> {
        Object obtener(T elemento);
    }

    // Cada columna conoce su título, tipo y valor.
    static final class Columna<T> {
        private final String titulo;
        private final Class<?> tipo;
        private final ValorColumna<T> valor;

        private Columna(String titulo, Class<?> tipo, ValorColumna<T> valor) {
            this.titulo = titulo;
            this.tipo = tipo;
            this.valor = valor;
        }
    }

    static <T> Columna<T> texto(String titulo, ValorColumna<T> valor) {
        return new Columna<>(titulo, String.class, valor);
    }

    static <T> Columna<T> numero(String titulo, ValorColumna<T> valor) {
        return new Columna<>(titulo, Integer.class, valor);
    }

    // Configuración de JTable
    @SafeVarargs
    Tabla(Columna<T>... columnas) {
        modelo = new ModeloTabla(List.of(columnas));
        tabla = new JTable(modelo);
        orden = new TableRowSorter<>(modelo);
        tabla.setRowSorter(orden);
        tabla.setRowHeight(32);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setSelectionBackground(new Color(223, 242, 228));
        tabla.setSelectionForeground(PanelModulo.TINTA);
        tabla.setFillsViewportHeight(true);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(PanelModulo.LINEA);
        tabla.setDefaultRenderer(String.class, new EstadoRenderer());
        for (int i = 0; i < columnas.length; i++) {
            javax.swing.table.TableColumn columna = tabla.getColumnModel().getColumn(i);
            String titulo = columnas[i].titulo;
            if (titulo.equals("ID")) {
                columna.setPreferredWidth(58);
                columna.setMaxWidth(72);
            } else if (titulo.equals("Meses")) {
                columna.setPreferredWidth(70);
                columna.setMaxWidth(95);
            } else if (titulo.equals("Sedes permitidas") || titulo.equals("Nombre completo")
                    || titulo.equals("Disponibilidad") || titulo.equals("Especialidad")) {
                columna.setPreferredWidth(185);
            }
        }
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setBackground(PanelModulo.TINTA);
        tabla.getTableHeader().setForeground(Color.WHITE);
        tabla.getTableHeader().setFont(tabla.getTableHeader().getFont().deriveFont(java.awt.Font.BOLD));
        setViewportView(tabla);
        setBorder(BorderFactory.createLineBorder(PanelModulo.LINEA));
    }

    // Carga de datos
    void mostrar(List<T> filas, ValorColumna<T> identidad) {
        T seleccion = seleccion();
        Object clave = seleccion == null ? null : identidad.obtener(seleccion);
        modelo.filas = filas;
        modelo.fireTableDataChanged();
        if (clave != null) {
            for (int i = 0; i < filas.size(); i++) {
                if (Objects.equals(clave, identidad.obtener(filas.get(i)))) {
                    int filaVista = tabla.convertRowIndexToView(i);
                    if (filaVista >= 0) tabla.setRowSelectionInterval(filaVista, filaVista);
                    break;
                }
            }
        }
    }

    // Selección de filas
    T seleccion() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : modelo.filas.get(tabla.convertRowIndexToModel(fila));
    }

    void alSeleccionar(Runnable accion) {
        tabla.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) accion.run();
        });
    }

    // Filtro de búsqueda
    void buscar(String texto, int... columnas) {
        orden.setRowFilter(texto.isBlank() ? null : RowFilter.regexFilter("(?iu)" + Pattern.quote(texto.strip()), columnas));
    }

    // Modelo de datos de JTable.
    private class ModeloTabla extends AbstractTableModel {
        private final List<Columna<T>> columnas;
        private List<T> filas = List.of();

        ModeloTabla(List<Columna<T>> columnas) { this.columnas = columnas; }

        @Override public int getRowCount() { return filas.size(); }
        @Override public int getColumnCount() { return columnas.size(); }
        @Override public String getColumnName(int columna) { return columnas.get(columna).titulo; }
        @Override public Class<?> getColumnClass(int columna) { return columnas.get(columna).tipo; }
        @Override
        public Object getValueAt(int fila, int columna) {
            T elemento = filas.get(fila);
            Columna<T> columnaActual = columnas.get(columna);
            Object valor = columnaActual.valor.obtener(elemento);
            if (getColumnClass(columna) == String.class) {
                return Objects.toString(valor, "");
            }
            return valor;
        }
    }

    // Renderer: colores para los estados.
    private static class EstadoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada, boolean foco,
                                                        int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, seleccionada, foco, fila, columna);
            setForeground(seleccionada ? tabla.getSelectionForeground() : PanelModulo.TINTA);
            setBackground(seleccionada ? tabla.getSelectionBackground()
                    : fila % 2 == 0 ? Color.WHITE : new Color(247, 250, 248));
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 5));
            boolean columnaEstado = tabla.getColumnName(columna).equals("Estado");
            if (columnaEstado && valor != null && !seleccionada) {
                Color color;
                switch (valor.toString().toUpperCase(Locale.ROOT)) {
                    case "ACTIVO": case "VIGENTE": case "PAGADO":
                        color = new Color(22, 101, 67); break;
                    case "POR VENCER": case "PENDIENTE": case "PARCIAL":
                        color = new Color(139, 91, 0); break;
                    case "SUSPENDIDO": case "VENCIDA":
                        color = new Color(164, 43, 52); break;
                    case "CONGELADA": case "PROGRAMADA":
                        color = new Color(48, 80, 134); break;
                    default:
                        color = PanelModulo.TINTA;
                }
                setForeground(color);
            }
            return this;
        }
    }
}
