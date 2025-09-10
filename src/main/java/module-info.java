module com.asos {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.web;
    requires javafx.graphics;
    
    requires com.github.oshi;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    
    requires org.fxmisc.richtext;
    
    requires org.slf4j;
    requires ch.qos.logback.classic;
    
    exports com.asos;
}
