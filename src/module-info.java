module richtext {
	requires javafx.controls;
	requires javafx.fxml;
	requires jfx.incubator.richtext;
	requires javafx.graphics;
	requires java.compiler;
	requires jdk.compiler;
	
	opens app to javafx.graphics, javafx.fxml;
}
