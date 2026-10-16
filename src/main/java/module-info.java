module org.example.module6assigment {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens org.example.module6assigment to javafx.fxml;
    exports org.example.module6assigment;
}