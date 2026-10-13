module org.example.module6assigment {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.module6assigment to javafx.fxml;
    exports org.example.module6assigment;
}