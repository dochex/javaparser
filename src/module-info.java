module richtext {
	requires javafx.controls;
	requires javafx.fxml;
	requires jfx.incubator.richtext;
	requires javafx.graphics;
	requires static java.compiler;
	requires jdk.compiler;
	requires java.desktop;
	requires jdk.javadoc;
	requires jfx.incubator.input;
	requires javafx.web;

	
	opens app to javafx.graphics, javafx.fxml;
}
