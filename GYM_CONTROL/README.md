# GYM CONTROL

Avance del trabajo final de Lenguajes de Programación III, Ingeniería de Sistemas, UCSM. Aplicación de escritorio para registrar socios, administrar membresías y cobros, y controlar ingresos y actividades de un gimnasio con varias sedes.

## Ejecutar desde Eclipse

1. Extrae `GYM_CONTROL_AVANCE.zip` completo.
2. En Eclipse, entra a **File → Import → General → Existing Projects into Workspace**. En **Select root directory**, elige la carpeta extraída que contiene `.project` y termina con **Finish**.
3. Verifica que **Window → Preferences → Java → Installed JREs** tenga un JDK 21 y que **Execution Environments → JavaSE-21** lo tenga asignado. El proyecto usa Java 21, UTF-8 y las carpetas fuente `src` y `recursos`.
4. Abre `src/aplicacion/App.java`. Haz clic derecho y elige **Run As → Java Application**.
5. Para ejecutar las comprobaciones, abre `src/pruebas/PruebaModelo.java` y repite **Run As → Java Application**. Las pruebas usan bases temporales y su salida se muestra en **Console**.

El archivo `lib/sqlite-jdbc.jar` ya figura con ruta relativa en `.classpath`. Si Eclipse lo marca como faltante, confirma que extrajiste todo el ZIP, actualiza el proyecto con **Refresh** y revisa **Build Path → Configure Build Path → Libraries → Classpath → Add JARs**. Selecciona `GYM_CONTROL/lib/sqlite-jdbc.jar`. El controlador incluido es Xerial SQLite JDBC 3.46.1.3. El proyecto funciona localmente con el JAR distribuido.

### Ejecutar también desde terminal

Desde la carpeta `GYM_CONTROL`, con JDK 21:

**PowerShell**

```powershell
New-Item -ItemType Directory -Force bin | Out-Null
$fuentesGym = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
javac --release 21 -encoding UTF-8 -d bin $fuentesGym
java -cp "bin;recursos;lib/*" aplicacion.App
java -cp "bin;recursos;lib/*" pruebas.PruebaModelo
```

**Linux o macOS**

```sh
mkdir -p bin
listaGym=$(mktemp)
find src -name '*.java' > "$listaGym"
javac --release 21 -encoding UTF-8 -d bin @"$listaGym"
rm "$listaGym"
java -cp 'bin:recursos:lib/*' aplicacion.App
java -cp 'bin:recursos:lib/*' pruebas.PruebaModelo
```

### Crear un ejecutable portátil de un solo archivo

En Windows con JDK 21, ejecuta desde la carpeta del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-portable.ps1
java -jar .\dist\GYM_CONTROL_portable.jar
```

El JAR incluye la aplicación compilada, SQLite JDBC, el esquema y los datos de demostración, las licencias y el código fuente en `META-INF/sources`. En otra PC basta Java 21 para ejecutarlo. Para compilar también el código fuente extraído del mismo JAR, desde la carpeta donde está el proyecto usa PowerShell:

```powershell
New-Item -ItemType Directory -Force fuentes, bin | Out-Null
Push-Location fuentes
jar xf ..\dist\GYM_CONTROL_portable.jar META-INF/sources
Pop-Location
$fuentesGym = Get-ChildItem -Recurse .\fuentes\META-INF\sources -Filter *.java | ForEach-Object { $_.FullName }+javac --release 21 -encoding UTF-8 -cp .\dist\GYM_CONTROL_portable.jar -d bin $fuentesGym
java -cp "bin;.\dist\GYM_CONTROL_portable.jar" aplicacion.App
```

El archivo de base de datos `gym_control.db` se crea localmente en la carpeta desde donde se ejecuta la aplicación; no hace falta instalar ni conectarse a un servidor de base de datos.

## Datos y recorrido de demostración

La primera ejecución crea `gym_control.db` en el directorio de trabajo, normalmente la carpeta del proyecto en Eclipse. Se inicializa con sedes, socios, planes, membresías, asistencias y clases ficticias. Las ejecuciones siguientes conservan los cambios y no repiten la carga inicial. Para comenzar con otra base vacía, agrega `-Dgym.db=gym_vacio.db -Dgym.sinDemo=true` en **Run Configurations → Arguments → VM arguments**. Los ejemplos dependen de la fecha en que se creó la base.

| Para mostrar | En la aplicación |
| --- | --- |
| Observer | Crea un socio y vuelve a **Inicio**. El total cambia junto con la tabla **Socios**. |
| Command | Registra un socio, una membresía y un abono; usa **Deshacer / Rehacer** o **Ctrl+Z / Ctrl+Y**. En **Pagos**, deshaz un abono y comprueba que vuelve el saldo. |
| Strategy | Selecciona a Valeria Torres para su primer contrato y elige **Nuevo socio (10 %)**. Para una renovación anticipada, selecciona la membresía de Diego Rivas si le quedan hasta 7 días. |
| Pago en cuotas | Selecciona la membresía de Camila Salas en **Pagos**. El ejemplo tiene S/ 150.00 de precio, S/ 100.00 abonados y S/ 50.00 de saldo. |
| Aforo y asistencia | En **Accesos y asistencias**, prueba el DNI ficticio `00000011` en **Arequipa Centro**. Registra su entrada y su salida. |
| Congelamiento | Consulta la membresía de Lucía Vega. Si la pausa inicial sigue activa, intenta ingresarla con el DNI `00000033` y luego reactívala en **Planes y membresías**. |
| Reservas | En **Entrenadores y clases**, selecciona **Funcional**, reserva un cupo para otro socio activo y verifica el cambio en la lista de participantes. |
| SwingWorker | En **Inicio**, exporta el historial de asistencias a CSV. La barra muestra el progreso de la escritura. |

## Funcionalidades del avance

- Socios con registro, edición, búsqueda, estado e historial relacionado. Sedes con capacidad y aforo. Planes con duración, precio y sedes permitidas.
- Membresías y renovaciones con historial, precios calculados según la tarifa y vigencia derivada de las fechas.
- Congelamientos con motivo, reactivación y ajuste de vencimiento. Pagos completos o parciales con historial y saldo.
- Validación de ingreso por DNI y sede, entradas y salidas con hora y control de aforo.
- Entrenadores, clases grupales, listado de participantes, reservas y cancelaciones.
- Inicio con cifras de SQLite, reporte CSV de asistencias y deshacer/rehacer para cambios en socios, registros de membresía y abonos.

## Reglas adoptadas

- **Vigencia.** El último día de servicio se incluye. El vencimiento se calcula con `inicio.plusMonths(meses).minusDays(1)`. Faltando siete días o menos, incluida la fecha de vencimiento, aparece **Por vencer**.
- **Renovación.** Se renueva la última membresía vigente o vencida. El nuevo periodo empieza el día siguiente del vencimiento, o hoy si el contrato ya venció. Se impiden intervalos superpuestos.
- **Congelamiento.** Empieza el día del registro y admite de 1 a 90 días. Se amplía el vencimiento por el periodo completo. Si se reactiva antes, se descuentan los días sin usar. El día de reactivación vuelve a habilitar el ingreso si se cumplen las demás reglas. Si otra membresía ya programada se cruza con la ampliación, la pausa se rechaza.
- **Tarifas.** Primera membresía con 10 % de descuento. Renovación con 5 % durante los últimos siete días de vigencia. Primero se valida si corresponde el descuento, luego se guarda el precio acordado. Los importes se guardan en céntimos.
- **Ingreso.** Requiere socio activo, membresía vigente, al menos 50 % pagado, sede incluida y aforo libre. Cada socio puede tener una sola entrada abierta, con salida en la misma sede. El permiso usa la lista actual de sedes del plan.
- **Clases.** Se programan a futuro. Un socio activo puede reservar un cupo por clase. Al cancelar se libera el cupo. El aforo de acceso a la sede y los cupos de una clase se administran por separado.
- **Historial de edición.** Deshacer y rehacer funciona durante la sesión para altas, edición y cambio de estado de socios, registros de membresía y abonos. Una membresía no se puede revertir si ya tiene pagos, pausas o asistencias. Las operaciones que todavía no tienen comando limpian el historial para evitar revertir cambios antiguos fuera de orden.

## Leer el código para exponer

Abre primero `App.java` y sigue `abrirVentana()` para ubicar las tres capas. Para la demostración, empieza en `PanelSocios.formulario()`, sigue la llamada a `GymControlador.registrarSocio()` y abre `RegistrarSocioComando`. Desde allí puedes llegar al alta en `GymModelo` y a la consulta de DNI en `SocioDAO`.

Las entidades principales usan clases con campos privados, constructores y getters. Los comandos conservan las versiones anterior y nueva del socio como objetos distintos para poder restaurarlas. Los métodos de `GymModelo` y `GymControlador` están agrupados por función. La guía dentro de `documentacion/` indica qué abrir para cada diapositiva y qué métodos conviene practicar.

## Organización del código

| Paquete | Responsabilidad |
| --- | --- |
| `aplicacion` | `App` configura y conecta las capas en el EDT. |
| `modelo` | Entidades, validación, reglas y consultas de negocio a través de `GymModelo`. |
| `modelo.persistencia` | Conexión, transacciones, cuatro DAO y exportación CSV. |
| `modelo.estrategia` | Contrato `EstrategiaPrecio` y las tres tarifas intercambiables. |
| `modelo.comando` y `util` | Comandos de socios, membresías y pagos, gestor y dos instancias de `PilaGenerica<Comando>`. |
| `controlador` | Recibe los datos de los formularios, coordina llamadas, crea `SwingWorker` y lleva Observer al EDT. |
| `vista` | Ventana y pestañas, formularios, tabla ordenable, renderer y panel de resumen. |
| `excepciones` y `pruebas` | Errores de validación y persistencia, y pruebas de consola. |

**Observer.** `GymModelo` publica un `EstadoGym` después de confirmar SQLite. El controlador adapta cada `GymListener` al EDT. Tanto `PanelSocios` como `PanelResumen` actualizan sus componentes en respuesta a esos avisos.

**Strategy.** El `JComboBox` entrega un objeto que cumple `EstrategiaPrecio`. El modelo ejecuta `calcular(contexto)` con la tarifa base, el historial y la fecha. La tarifa elegida decide sus propias condiciones.

**Command.** `GestorComandos` invoca `ejecutar()` o `deshacer()` y solo cambia las pilas al confirmarse la operación. Los comandos de socios, membresías y pagos guardan los datos necesarios para restaurar la operación y recuperar el mismo ID. Las operaciones no reversibles vacían ambas pilas.

**SOLID.** SRP separa persistencia, reglas y componentes Swing. OCP permite incorporar nuevas estrategias u observadores mediante registro. LSP permite intercambiar estrategias que aplican el mismo contrato de cálculo y validación. ISP mantiene pequeñas las interfaces `GymListener` y `Comando`. DIP se aprecia cuando el modelo usa `EstrategiaPrecio` y el gestor usa `Comando`, ambos a través de interfaces.

## Continuación del proyecto

| Función pendiente | Cómo se puede incorporar |
| --- | --- |
| Convenios con empresas, promoción de temporada y referidos, administración de campañas | Agregar implementaciones de `EstrategiaPrecio`, proporcionar los datos adicionales y registrar las opciones en el controlador. Si se administran convenios y campañas, agregar su entidad y DAO. |
| Rutinas y evaluaciones físicas | Agregar tablas relacionadas con socio y entrenador, un DAO y un módulo Swing. |
| Nutricionistas, planes nutricionales, seguimiento y citas | Agregar entidades, tablas y un módulo de atención. Vincular citas con el socio y el profesional correspondiente. |
| Sauna y casilleros | Integrar disponibilidad y uso por sede. El modelo valida disponibilidad y el controlador coordina el nuevo panel. |
| Alertas completas y reportes ampliados | Registrar un observador nuevo para reaccionar a cambios. Reutilizar `SwingWorker` para consultas y exportaciones. |
| Usuarios y permisos | Incorporar autenticación y roles si se acuerda para el final. |

Para un comando nuevo, implementar `Comando` y guardar el estado que se necesite antes del cambio. Para añadir un módulo, crear la entidad, la tabla en `esquema.sql`, las operaciones DAO, el panel y su pestaña en `VentanaPrincipal`. Los módulos pueden reutilizar el modelo de tablas, el manejo de errores y la actualización de vistas.

## Evidencia y limitaciones

- [UML y arquitectura MVC](documentacion/UML_y_MVC.pdf)
- [Salida de las 57 comprobaciones de consola](documentacion/PruebasModelo.txt)
- [Verificaciones de la interfaz Swing](documentacion/PruebasInterfaz.txt)
- [Compatibilidad con los datos de la versión anterior](documentacion/CompatibilidadSQLite.txt)
- [Guía para exponer el código en Eclipse](documentacion/Guia_Exposicion_Eclipse.pdf)
- [Captura del panel de inicio](documentacion/capturas/inicio.png)
- [Captura de planes y membresías](documentacion/capturas/membresias.png)
- [Captura de control de acceso](documentacion/capturas/acceso.png)

Esta revisión se compiló con Eclipse Compiler for Java en Java 21. Pasaron las 57 comprobaciones de consola y siete recorridos en Swing. La revisión leyó una base SQLite creada por la versión anterior, incluidos los datos de una alta, sin duplicar la demostración. Se comprobó que las rutas de `.classpath` son relativas y que el JAR incluido funciona. Queda por comprobar la importación directamente en Eclipse de escritorio y en el equipo Windows de la exposición. La base es local y el flujo de administración no incluye autenticación. No hay calendario de cuotas, sincronización entre instalaciones ni integraciones de pago. La interfaz muestra el último congelamiento de cada membresía. Las sesiones de entrenamiento se programan sin verificar cruces de horario entre clases.

**Uso de IA.** Se utilizó ChatGPT como apoyo para adaptar el diseño MVC al alcance, implementar y simplificar el código inicial, redactar documentación, preparar los diagramas y la presentación, y ejecutar comprobaciones. Los integrantes deben revisar el código, probarlo en sus equipos y poder explicar y modificar su parte. Las pruebas aquí incluidas fueron ejecutadas sobre el código que se entrega.

`sqlite-jdbc.jar` incluye sus avisos de licencia. Se acompañan también copias de las licencias de Xerial y Zentus en `lib/`.
