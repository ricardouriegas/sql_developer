module sql.ide {
    requires javafx.controls;
    requires javafx.fxml;
    requires edu.upvictoria.fpoo;
    requires javafx.graphics;
    requires richtextfx.fat;

    opens sql.ide to javafx.fxml;
    exports sql.ide;
}
