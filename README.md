# SQL GUI (Developer & Modeler)

## Little Instructions to Run

You'll need to install this two jar;

* The first one (database_manager) is a library that i made to manage the database, you can find it in the folder sql_gui/dataBaseManager-1.0-SNAPSHOT.jar

* The second one (richtextfx) is a library that i used to make the text area have colors, you can find it in the folder sql_gui/richtextfx-fat-0.11.2.jar

``` bash
mvn install:install-file -Dfile=sql_gui/dataBaseManager-1.0-SNAPSHOT.jar -DgroupId=edu.upvictoria.fpoo -DartifactId=database_manager -Dversion=1.0 -Dpackaging=jar

mvn install:install-file -Dfile=sql_gui/richtextfx-fat-0.11.2.jar -DgroupId=org.fxmisc.richtext -DartifactId=richtextfx -Dversion=0.11.2 -Dpackaging=jar
```

Special thanks to [RichTextFX](https://github.com/FXMisc/RichTextFX) for the library that i used to make the text area have colors and at the same time be 
able of run what the user selects.
<!-- 
 I execute this bc i do it wherever i want:

 mvn install:install-file -Dfile=/home/richy/Documents/sql_gui/sql_gui/dataBaseManager-1.0-SNAPSHOT.jar -DgroupId=edu.upvictoria.fpoo -DartifactId=database_manager -Dversion=1.0 -Dpackaging=jar 

 mvn install:install-file -Dfile=/home/richy/Documents/sql_gui/sql_gui/richtextfx-fat-0.11.2.jar -DgroupId=org.fxmisc.richtext -DartifactId=richtextfx -Dversion=0.11.2 -Dpackaging=jar
 -->
## Implementación de una interfaz gráfica de usuario (GUI) para SQL con herramientas de diseño de diagramas Entidad-Relación y generación de código SQL

**Introducción:**

El objetivo de este proyecto es continuar con el desarrollo de una aplicación de escritorio que permita a los usuarios la manipulación de sentencias SQL, ademáas de crear y manipular diagramas Entidad-Relación (ER) de forma interactiva, con la funcionalidad adicional de generar código SQL a partir de dichos diagramas. La aplicación estará basada en maven, Java y JavaFX, proporcionando una interfaz gráfica amigable y herramientas intuitivas para el diseño de diagramas ER.

Se deberá continuar con la aplicación desarrollada en práctica 3.

**Requisitos:**

  * **Diseño de interfaz gráfica:** La aplicación debe contar con una interfaz gráfica de usuario (GUI) intuitiva y fácil de usar, diseñada con JavaFX. La interfaz debe incluir:
    * Un área de trabajo principal para la visualización y edición de diagramas ER.
    * Paneles de herramientas con controles para agregar, eliminar y modificar entidades, atributos y relaciones.
    * Opciones para generar código SQL a partir del diagrama ER actual.
    * Un panel de resultados para mostrar el código SQL generado o cualquier mensaje de error.
  * **Herramientas de diseño de diagramas ER:** La aplicación debe proporcionar herramientas para crear y modificar diagramas ER de forma interactiva. Estas herramientas deben incluir:
    * Controles para arrastrar y soltar entidades, atributos y relaciones en el área de trabajo principal.
    * Opciones para editar las propiedades de entidades, atributos y relaciones, como nombre, tipo de dato y cardinalidad.
    * Funciones para conectar entidades mediante relaciones y establecer el tipo de relación (uno a uno, uno a varios, varios a uno, varios a varios).
  * **Generación de código SQL:** La aplicación debe permitir generar código SQL a partir del diagrama ER actual. El código SQL generado debe ser válido y representar la estructura de la base de datos correspondiente al diagrama. La aplicación debe generar código SQL para:
    * Creación de tablas con sus respectivas columnas y restricciones.
    * Definición de relaciones entre tablas.
  * **Validación de diagramas ER:** La aplicación debe implementar mecanismos para validar la integridad del diagrama ER actual. Esto incluye:
    * Verificar que las entidades tengan un nombre único.
    * Asegurar que los atributos tengan un nombre y tipo de dato válidos.
    * Comprobar que la cardinalidad de las relaciones sea coherente.
    * Mostrar mensajes de error al usuario en caso de detectar inconsistencias en el diagrama.
  * **Ingeniería inversa**
    * El sistema deberá ser capaz de generar el diagrama ER a partir de la estructura de una base de datos previamente generada.

**Evaluación:**

La aplicación será evaluada en base a los siguientes criterios:

  * **Funcionalidad:** La aplicación debe cumplir con todos los requisitos descritos anteriormente.
  * **Usabilidad:** La interfaz gráfica debe ser intuitiva y fácil de usar. Las herramientas de diseño de diagramas ER deben ser prácticas y eficientes.
  * **Calidad del código:** El código fuente debe estar bien escrito, documentado y organizado de manera lógica.
  * **Manejo de errores:** La aplicación debe manejar adecuadamente los errores y mostrar mensajes informativos al usuario.# SQL GUI

## Implementación de una Interfaz Gráfica de Usuario (GUI) para SQL

## Descripción Detallada de la Actividad Práctica: Implementación de un IDE SQL con JavaFX y Maven


**Objetivo:**

El objetivo de esta actividad práctica es desarrollar una aplicación IDE (entorno de desarrollo integrado) específico para la edición y ejecución de códigos SQL utilizando JavaFX y Maven. La aplicación debe cumplir con las siguientes características:

**Funcionalidades:**

  1. **Resaltado de código SQL mediante colores:** Implementar un mecanismo para resaltar diferentes elementos del código SQL, como palabras clave, funciones, nombres de tablas, campos, etc., utilizando diferentes colores para mejorar la legibilidad y comprensión del código.
  2. **Menús para la carga y guardado de archivos SQL:** Implementar menús y/o botones que permitan al usuario cargar archivos SQL existentes y guardar los cambios realizados en la aplicación.
  3. **Botones para ejecución de códigos SQL:** Implementar botones que permitan al usuario ejecutar el código SQL escrito en el editor. Cada comando ejecutado se deberá ver reflejado en los archivos CSV que fungen como tablas, todo ésto mediante la implementación propia del manejador de bases de datos. La aplicación debe poder conectarse a una base de datos (carpeta) y ejecutar los comandos SQL, mostrando los resultados en un panel designado.
  4. **Panel izquierdo con control tipo árbol para el despliegue y muestra de las tablas y sus campos:** Implementar un control tipo árbol en el panel izquierdo de la interfaz que permita al usuario visualizar las tablas existentes en la base de datos y sus respectivos campos. El control debe actualizarse dinámicamente al conectarse a una base de datos (carpeta) o al realizar cambios en la estructura de la misma.
  5. **Panel inferior de tipo rejilla para mostrar resultados de consultas SELECT:** Implementar un control tipo rejilla en el panel inferior de la interfaz que permita al usuario visualizar los resultados de las consultas SELECT ejecutadas. La rejilla debe mostrar los nombres de las columnas y los datos correspondientes a cada fila. Se deberán respetar los nombre de los campos y/o los alias designados en el comando SELECT.

**Conceptos clave:**

Se deberán utilizar los siguientes temas vistos anteriormente:

  1. **Clases, objetos e interfaces:** Utilizar clases para representar las diferentes entidades del sistema, como la ventana principal, el editor de código, el control de árbol y la rejilla de resultados. Utilizar objetos para crear instancias de las clases y utilizar interfaces para definir contratos de comportamiento entre las clases.
  2. **Clases abstractas:** Utilizar clases abstractas para definir clases base que comparten características comunes, como la clase abstracta `SQLCommand` que puede representar una consulta SQL o un procedimiento almacenado.
  3. **Clases genéricas:** Utilizar clases genéricas para definir clases que pueden trabajar con diferentes tipos de datos, como la clase genérica `TreeItem` que puede representar un nodo en el control de árbol y puede contener datos de cualquier tipo.
  4. **Manejo de excepciones:** Implementar el manejo de excepciones para capturar y gestionar errores durante la ejecución de la aplicación, como errores de conexión a la base de datos, errores de sintaxis SQL o errores de tipo de datos.

**Evaluación:**

La actividad práctica será evaluada en base a los siguientes criterios:

* **Funcionalidad:** La aplicación debe cumplir con todas las características descritas anteriormente.
* **Diseño de la interfaz de usuario:** La interfaz de usuario debe ser intuitiva y fácil de usar.
* **Calidad del código:** El código debe ser limpio, organizado y bien documentado.
* **Manejo de excepciones:** La aplicación debe manejar adecuadamente las excepciones que puedan surgir durante la ejecución.
* **Test Unitarios y de integración** Se deberán generar las pruebas unitadas y de integración para el correcto functionamiento del sistema.