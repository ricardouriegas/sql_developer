module sql.ide {
    requires javafx.controls;
    requires javafx.fxml;

    opens sql.ide to javafx.fxml;
    exports sql.ide;
}
