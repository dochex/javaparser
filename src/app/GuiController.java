package app;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.lang.model.SourceVersion;

import javafx.fxml.FXML;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.scene.control.Button;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

import com.sun.source.tree.ImportTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

public class GuiController {

	@FXML
	RichTextArea textArea;
	@FXML
	Button openJava;

	StyleAttributeMap violet = StyleAttributeMap.builder().setBold(true).setFontFamily("Monospaced").setFontSize(14)
			.setTextColor(Color.BLUEVIOLET).build();
	StyleAttributeMap blue = StyleAttributeMap.builder().setBold(true).setFontFamily("Monospaced").setFontSize(14)
			.setTextColor(Color.BLUE).build();
	StyleAttributeMap lowerBlue = StyleAttributeMap.builder().setBold(true).setFontFamily("Monospaced").setFontSize(14)
			.setTextColor(Color.DODGERBLUE).build();
	StyleAttributeMap green = StyleAttributeMap.builder().setBold(true).setFontFamily("Monospaced").setFontSize(14)
			.setTextColor(Color.GREEN).build();
	StyleAttributeMap grey = StyleAttributeMap.builder().setBold(true).setFontFamily("Monospaced").setFontSize(14)
			.setTextColor(Color.DIMGRAY).build();
	StyleAttributeMap red = StyleAttributeMap.builder().setBold(true).setFontFamily("Monospaced").setFontSize(14)
			.setTextColor(Color.RED).build();
	StyleAttributeMap dark = StyleAttributeMap.builder().setBold(true).setFontFamily("Monospaced").setFontSize(14)
			.setTextColor(Color.BLACK).build();

	private Parser parser;
	public List<String> importsClass = new ArrayList<>();
	public List<VariableTree> innerVariablesList = new ArrayList<>();
	private List<MethodTree> innerMethodList = new ArrayList<>();
	private List<String> listVarName = new ArrayList<>();
	
	public GuiController() {
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
		process(file);
		
	}

	private void process(File file) {
		parser = new Parser(file);
		String packageName = parser.getPackageName();
		if (!packageName.equals("")) {
			textArea.appendText("package ", red);
			textArea.appendText(packageName + "\n\n", dark);
		}
		processImports(parser.getImportTree());
		processDeclaration(packageName, parser.getClassTree());
		processVariables(parser.getVariableList());
		textArea.appendText("\n");
		processMethods(parser.getClassName(), parser.getMethodList());
		processKindClass(parser.getKindClassList());
		textArea.appendText("}", dark);
	}

	private void processKindClass(List<ClassTree> kindClassList) {
		for (ClassTree kcTree: kindClassList){
			processDeclaration("", kcTree);
			String className = kcTree.getSimpleName().toString();
			final List<? extends Tree> classMemberList = kcTree.getMembers();
			for (Tree tree : classMemberList) {

                if (tree.getKind() == Tree.Kind.VARIABLE) {
                    VariableTree variableTree = (VariableTree) tree;
                    innerVariablesList.add(variableTree);
                }
                if (tree.getKind() == Tree.Kind.METHOD) {
                    MethodTree methodTree = (MethodTree) tree;
                    innerMethodList .add(methodTree);
                }

            }
			processVariables(innerVariablesList);
			textArea.appendText("\n", dark);
			innerVariablesList.clear();
			processMethods(className, innerMethodList);
			innerMethodList.clear();
			textArea.appendText("}\n\n", dark);
		}
		
	}

	private void processMethods(String className, List<MethodTree> classMethods) {
		for (MethodTree mTree : classMethods) {
			textArea.appendText(mTree.getModifiers().toString(), red);
			if (mTree.getReturnType() != null) {
				getElementFromString(mTree.getReturnType().toString());
			}
			textArea.appendText(" ", dark);
			String methodName = mTree.getName().toString();
			if (methodName.equals("<init>")) {
				methodName = className;
			}
			textArea.appendText(methodName, green);
			textArea.appendText("(", dark);
			getElementFromString(mTree.getParameters().toString());
			textArea.appendText(")\n", dark);
			String bodyMethod = mTree.getBody().toString();
			Stream<String> lines = bodyMethod.lines();
			processBodyLines(lines);
			textArea.appendText("\n\n", dark);
		}

	}

	private void processBodyLines(Stream<String> lines) {
		lines.forEach(line -> {
			getElementFromString(line);
			textArea.appendText("\n", dark);
		});
	}

	private void processVariables(List<VariableTree> variableList) {
		for (VariableTree variable : variableList) {
			textArea.appendText(variable.getModifiers().toString(), red);
			getElementFromString(variable.getType().toString());
			textArea.appendText(" ", dark);
			//textArea.appendText(variable.getType().toString() + " ", blue);
			String varName = variable.getName().toString();
			listVarName.add(varName);
			textArea.appendText(varName, lowerBlue);
			if (variable.getInitializer() != null) {
				textArea.appendText(" = ", dark);
				getElementFromString(variable.getInitializer().toString());
			}
			textArea.appendText(";\n", dark);
		}
	}

	private void getElementFromString2(String str) {
		// the regex split on space , . ( ) < > and the + is for them together; ?= keep the delimiter
		String[] words = str.split("(?=[ |\\.|(|)|,|<|>]+)");
		String previous = "";
		for (String s : words) {
			System.out.println(s);
			if (s.length() > 1) {
				String st = s.trim();
				int end = s.length();
				if (isCapitalize(st)) {
					textArea.appendText(s.substring(0, end), violet);
				} else if (isKeyword(st)) {
					if (st.charAt(0) == '(') {
						textArea.appendText(s.substring(0, 1), grey);
						textArea.appendText(s.substring(1, end), red);
					} else {
						textArea.appendText(s, red);
					}
				} else if ((st.charAt(0) == '<' || st.charAt(0) == '(' || st.charAt(0) == ',') && isCapitalize(st.substring(1))) {
					textArea.appendText(s.substring(0, 1), dark);
					textArea.appendText(s.substring(1, end), violet);
				} else if (st.charAt(0) == '.' && Character.isLowerCase(st.charAt(1))) {
					if (s.contains("\"")) {
						textArea.appendText(s, grey);
					}else if(previous.trim().equals("this")) {
						textArea.appendText(s, blue);
					}else {
						textArea.appendText(s, green);
					}
				} else if (isClassField(s)) {
					continue;
				} else {
					textArea.appendText(s, grey);
				}
			} else {
				textArea.appendText(s, grey);
			}
			previous = s;
		}
	}
	
	private void getElementFromString(String str) {
		// the regex split on space , . ( ) < > and the + is for them together; ?= keep
		// the delimiter
		String[] words = str.split("(?<=[ |\\.|(|)|,|<|>|;|=]+)|(?=[ |\\.|(|)|,|<|>|;|=]+)");
		int endWords = words.length - 1;
		boolean quote = false;
		boolean comment = false;
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			if (word.length() > 1) {
				int end = word.length();
				if (quote) {
					textArea.appendText(word, grey);
					if (word.contains("\"") | word.contains("'") || word.equals("\"")) {
						quote = false;
					}
				} else if (word.charAt(0) == '"' | word.charAt(0) == '\'') {
					textArea.appendText(word, grey);
					if (word.charAt(end - 1) != '"' & word.charAt(end - 1) != '\'') {
						quote = true;
					}
					continue;
				}if (comment) {
						textArea.appendText(word, grey);
						if ( word.equals("\n")) {
							comment = false;
						}
					} else if (word.charAt(0) == '/' && word.charAt(1) == '/') {
						textArea.appendText(word, grey);
						if (word.charAt(end - 1) != ' ' & word.charAt(end - 1) != '\n') {
							comment = true;
						}
						continue;
				} else if (isKeyword(word)) {
					textArea.appendText(word, red);
					continue;
				} else if (isCapitalize(word)) {
					textArea.appendText(word.substring(0, end), violet);
					continue;				
				// methods
				} else if (isClassMethod(word)) {
					textArea.appendText(word, green);
					continue;
				} else if (Character.isLowerCase(word.charAt(0)) && i < endWords && words[i + 1].equals("(")) {
					textArea.appendText(word, green);
				// fields
				} else if (isClassField2(word)) {
					textArea.appendText(word, lowerBlue);
					continue;
				} else if (words[i - 1].equals(" ")) {
					if (i == endWords) {
						textArea.appendText(word, blue);
					} else if (words[i + 1].equals(".") || words[i + 1].equals(" ") || words[i + 1].equals(")")
							|| words[i + 1].equals(",") || words[i + 1].equals(";")) {
						textArea.appendText(word, blue);
					} else {
						textArea.appendText(words[i], grey);
					}
				} else if (words[i - 1].equals(")") && i < endWords) {
					if (words[i + 1].equals(",") || words[i + 1].equals(".") || words[i + 1].equals(";")) {
						textArea.appendText(word, blue);
					} else {
						textArea.appendText(word, grey);
					}
				} else if (words[i - 1].equals("(") && i < endWords) {
					if (words[i + 1].equals(")") || words[i + 1].equals(".") || words[i + 1].equals(",")
							|| words[i + 1].equals(" ")) {
						textArea.appendText(word, blue);
					} else {
						textArea.appendText(word, grey);
					}
				} else {
					textArea.appendText(word, grey);
				}
			} else {
				if (word.matches("[aA-zZ]")) {
					textArea.appendText(word, blue);
					continue;
				} else {
					textArea.appendText(word, grey);
					if (word.equals("\"") | word.equals("'")) {
						quote = !quote;
					}
				}
			}
		}
	}
	
	private boolean isClassMethod(String s) {
		for (String methodName : parser.methodNameList) {
			if (s.contains(methodName)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isClassField2(String s) {
		for (String varName : listVarName) {
			if (s.contains(varName)) {
				return true;
			}
		}
		return false;
	}

	
	
	private boolean isClassField(String s) {
		for (String varName : listVarName) {
			if (s.contains(varName)) {
				textArea.appendText(s.substring(0, s.indexOf(varName)), grey);
				textArea.appendText(s.substring(s.indexOf(varName), varName.length()+1), lowerBlue);
				if (s.length() > varName.length()) {
					textArea.appendText(s.substring(s.indexOf(varName) + varName.length()), grey);
				}
				return true;
			}
		}
		return false;
	}

	private static boolean isKeyword(String str) {
		if (str.length() > 1) {
			if (SourceVersion.isKeyword(str)) {
				return true;
			} else if (str.charAt(0) == '(') {
				str = str.substring(1);
				if (SourceVersion.isKeyword(str))
					return true;
			} else if (str.charAt(str.length()-1) == ';' || str.charAt(str.length()-1) == ':') {
				str = str.substring(0, str.length()-1);
                if (SourceVersion.isKeyword(str))
                	return true;			
			}
		}
		return false;
	}

	private static boolean isCapitalize(String str) {
		if (Character.isUpperCase(str.charAt(0)) && !Character.isUpperCase(str.charAt(str.length() - 1))) {
			return true;
		} else
			return false;
	}

	private void processImports(List<? extends ImportTree> importTree) {
		for (ImportTree importLine : importTree) {
			String line = importLine.toString();
			textArea.appendText(line.substring(0, 6), red);
			textArea.appendText(line.substring(6), dark);
			importsClass.add(line.substring(line.lastIndexOf(".") + 1, line.trim().length() - 1));
		}
		textArea.appendText("\n", dark);
	}

	private void processDeclaration(String packageName, ClassTree classTree) {
		textArea.appendText(classTree.getModifiers().toString(), red);
		switch (classTree.getKind()) {
		case ENUM -> textArea.appendText("enum ", red);
		case INTERFACE -> textArea.appendText("interface ", red);
		case RECORD -> textArea.appendText("record ", red);
		case ANNOTATION_TYPE -> textArea.appendText("@interface ", red);
		case CLASS -> textArea.appendText("class ", red);
		default -> textArea.appendText("unknown ", red);
		}
		textArea.appendText(classTree.getSimpleName().toString(), violet);
		Tree extendsClause = classTree.getExtendsClause();
		if (extendsClause != null) {
		    textArea.appendText(" extends ", red);
		    textArea.appendText(extendsClause.toString(), violet);
		}

		List<? extends Tree> implementsClause = classTree.getImplementsClause();
		if (implementsClause != null && !implementsClause.isEmpty()) {
		    textArea.appendText(" implements ", red);
		    textArea.appendText(implementsClause.toString(), violet);
		}
		textArea.appendText(" { \n\n", dark);
	}
	

}
