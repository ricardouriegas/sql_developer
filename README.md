# SQL GUI (Developer)
SQL GUI implementation using vanilla Java FX and the RichText Library for the Code Area.

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