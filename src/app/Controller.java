package app;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

public class Controller {

	@FXML
	RichTextArea textArea;
	@FXML
	Button openJava;
	private Trees trees;
	private CompilationUnitTree compilationUnit;
	private String sourceCode;
	private ClassTree classTree;
	private String fileName = "";
	StyleAttributeMap dark = StyleAttributeMap.builder().setTextColor(Color.BLACK).setBold(true).build();

	public Controller() {
	}

	public void initialize() {
		textArea.setEditable(true);
	}

	@FXML
	public void openJavaFile() {
		FileChooser chooser = new FileChooser();
		chooser.setInitialDirectory(new File(Path.of("./src/app").toAbsolutePath().toString()));
		chooser.setTitle("Select the first file");
		File file = chooser.showOpenDialog(null);
		try {
			fileName = file.getAbsolutePath();
			sourceCode = Files.readString(Paths.get(fileName), StandardCharsets.ISO_8859_1);
			sourceCode = sourceCode.replace("\t", "    "); // pour éviter les erreurs du charIndex de TextPos
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		textArea.clear();
		parse(file);
	}

	private void parse(File file) {
		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
			String className = file.getName().substring(0, file.getName().indexOf("."));
			JavaFileObject javaFile = new StringJavaFileObject(className, sourceCode);
			JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, null, null,
					Arrays.asList(javaFile));
			JavacTask javacTask = (JavacTask) task;
			Iterable<? extends CompilationUnitTree> compilationUnitTrees = javacTask.parse();
			javacTask.analyze();
			trees = Trees.instance(task);
			compilationUnit = compilationUnitTrees.iterator().next();
			textArea.appendText(sourceCode, dark);
			MyTreeVisitor visitor = new MyTreeVisitor(compilationUnit, trees, textArea, sourceCode);
			try {
				visitor.colorizeKeywords();
				visitor.colorizeComments();
				for (Tree typeDecl : compilationUnit.getTypeDecls()) {
					if (typeDecl instanceof ClassTree) {
						classTree = (ClassTree) typeDecl;
						TreePath classPath = TreePath.getPath(compilationUnit, classTree);
						visitor.visitClass(classTree, classPath);
						break;
					}
				}
				//visitor.colorizeAnnotations();
			} catch (VisitorException e) {
				System.err.println("Erreur : " + e.getMessage());
				e.printStackTrace();
				textArea.clear();
				textArea.appendText(sourceCode, dark);
			}
		} catch (Exception e) {
			showAlert("Erreur", "Erreur lors du parsing:\n" + e.getMessage());
			e.printStackTrace();
			textArea.clear();
			textArea.appendText(sourceCode, dark);
		}
	}

	private void showAlert(String title, String message) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	/**
	 * JavaFileObject pour cr�er un fichier depuis une String
	 */
	class StringJavaFileObject extends SimpleJavaFileObject {
		private final String code;

		public StringJavaFileObject(String name, String code) {
			super(java.net.URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
			this.code = code;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return code;
		}
	}

}
