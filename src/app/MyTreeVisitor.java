package app;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

import com.sun.source.tree.*;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.*;
import jfx.incubator.scene.control.richtext.model.*;

public class MyTreeVisitor extends TreeScanner<Void, TreePath> {

	private final Trees trees;
	private final CompilationUnitTree unit;
	private final RichTextArea richTextArea;
	private final String sourceCode;
	private String className = "";
	StyleAttributeMap violet = StyleAttributeMap.builder().setTextColor(Color.BLUEVIOLET).setBold(true).build(); //parameter
	StyleAttributeMap blue = StyleAttributeMap.builder().setTextColor(Color.BLUE).setBold(true).build(); //fields
	StyleAttributeMap green = StyleAttributeMap.builder().setTextColor(Color.GREEN).setBold(true).build(); //method
	StyleAttributeMap brown = StyleAttributeMap.builder().setTextColor(Color.SADDLEBROWN).setBold(true).build(); // local variable 
	StyleAttributeMap pink = StyleAttributeMap.builder().setTextColor(Color.DEEPPINK).setBold(true).build(); //comments
	StyleAttributeMap red = StyleAttributeMap.builder().setTextColor(Color.RED).setBold(true).build(); // keywords
	StyleAttributeMap orange = StyleAttributeMap.builder().setTextColor(Color.ORANGE).setBold(true).build(); // String
	StyleAttributeMap azure = StyleAttributeMap.builder().setTextColor(Color.DODGERBLUE).setBold(true).build(); // Class
	StyleAttributeMap dark = StyleAttributeMap.builder().setTextColor(Color.BLACK).setBold(true).build();
	public static boolean visitorException = false;
	private final SourcePositions sourcePositions;

	public MyTreeVisitor(CompilationUnitTree compilationUnit, Trees trees, RichTextArea richTextArea, String sourceCode) {
		this.trees = trees;
		this.unit = compilationUnit;
		this.richTextArea = richTextArea;
		this.sourceCode = sourceCode;
		sourcePositions = trees.getSourcePositions();
	}
	


	public void colorizeKeywords() throws Exception {
		try {
			Pattern pattern = Pattern.compile("\\b(\\w+)\\b");
			Matcher matcher = pattern.matcher(sourceCode);
			while (matcher.find()) {
				String word = matcher.group();
				if (SourceVersion.isKeyword(word)) {
					int start = matcher.start();
					int end = start + word.length();
					richTextArea.setStyle(fromCharIndex(unit, start), fromCharIndex(unit, end), red);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new VisitorException("Erreur lors de la visite", e);
		}
	}

	public void colorizeComments() throws Exception {
		// Pattern pour capturer :
		// - Commentaires de ligne : // ...
		// - Commentaires de bloc : /* ... */
		// - Commentaires Javadoc : /** ... */
		try {
			Pattern pattern = Pattern.compile("//[^\\r\\n]*|/\\*(?:[^*]|\\*(?!/))*\\*/", Pattern.DOTALL);
			Matcher matcher = pattern.matcher(sourceCode);
			while (matcher.find()) {
				int start = matcher.start();
				int end = matcher.end();
				richTextArea.setStyle(fromCharIndex(unit, start), fromCharIndex(unit, end), pink);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new VisitorException("Erreur lors de la visite", e);
		}
	}

	public void colorizeAnnotations() throws Exception {
		try {
			Pattern annotationPattern = Pattern.compile("@[\\w\\.]+(?:\\s*\\([^\\)]*\\))?");
			Matcher matcher = annotationPattern.matcher(sourceCode);
			while (matcher.find()) {
				int start = matcher.start();
				int end = matcher.end();
				TextPos startPos = fromCharIndex(unit, start);
				TextPos endPos = fromCharIndex(unit, end);
				richTextArea.setStyle(startPos, endPos, dark);
			}
		} catch (Exception e) {
			throw new VisitorException("Erreur lors de la visite", e);
		}
	}


	/**
	 * Visite une classe et colorise toutes ses m�thodes
	 */
	@Override
	public Void visitClass(ClassTree node, TreePath parentPath) {
		try {
			TreePath currentPath = parentPath != null ? new TreePath(parentPath, node) : TreePath.getPath(unit, node);
			/*node.getMembers().forEach(member -> {
				System.out.println("Member kind: " + member.getKind());
			});*/
			// Colorise le nom de la classe
			long start = 0;
			long end = 0;
			className = node.getSimpleName().toString();
			if (!className.isEmpty()) {
				if (!node.getModifiers().toString().isEmpty()) {
					start = sourcePositions.getEndPosition(unit, node.getModifiers());
				} else {
					start = sourcePositions.getStartPosition(unit, node) - 1;
				}
					start = start + node.getKind().toString().length() + 2;
					end = start + node.getSimpleName().length();
					richTextArea.setStyle(fromCharIndex(unit, (int) start), fromCharIndex(unit, (int) end), azure);
			}
			// Colorise les types paramètres et les clauses extends/implements
			node.getTypeParameters().forEach(typeParam -> {
				long paramStart = sourcePositions.getStartPosition(unit, typeParam);
				long paramEnd = sourcePositions.getEndPosition(unit, typeParam);
				richTextArea.setStyle(fromCharIndex(unit, (int) paramStart), fromCharIndex(unit, (int) paramEnd), azure);
			});
			if (node.getExtendsClause() != null) {
				start = sourcePositions.getStartPosition(unit, node.getExtendsClause());
				end = sourcePositions.getEndPosition(unit, node.getExtendsClause());
				richTextArea.setStyle(fromCharIndex(unit, (int) start), fromCharIndex(unit, (int) end), azure);
			}
			node.getImplementsClause().forEach(impl -> {
				long implStart = sourcePositions.getStartPosition(unit, impl);
				long implEnd = sourcePositions.getEndPosition(unit, impl);
				richTextArea.setStyle(fromCharIndex(unit, (int) implStart), fromCharIndex(unit, (int) implEnd), azure);
			});
			// Visite les membres de la classe
			for (Tree member : node.getMembers()) {
				//System.out.println("Visiting member of kind: " + member.getKind());
				if (member instanceof MethodTree method) {
					TreePath memberPath = new TreePath(currentPath, method);
					visitMethod(method, memberPath);
				}
				if (member instanceof VariableTree variable) {
					TreePath memberPath = new TreePath(currentPath, variable);
					visitVariable(variable, memberPath);
				}
				if (member instanceof ExpressionStatementTree expr) {
					TreePath memberPath = new TreePath(currentPath, expr);
					visitExpressionStatement(expr, memberPath);
				}
				if (member instanceof ClassTree tree) {
					TreePath memberPath = new TreePath(currentPath, tree);
					visitClass(tree, memberPath);
				}
				if (member instanceof BlockTree tree) {
					TreePath memberPath = new TreePath(currentPath, tree);
					visitBlock(tree, memberPath);
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
	        throw new VisitorException("Erreur lors de la visite", e);
		}
		return null;
	}

	@Override
	public Void visitExpressionStatement(ExpressionStatementTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		scan(node.getExpression(), currentPath);
		return null;
	}

	@Override
	public Void visitMethod(MethodTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		Element element = trees.getElement(currentPath);
		long start = 0;
		long end = 0;
		if (element.getKind() != ElementKind.CONSTRUCTOR) {
			//colorize return type
			Tree returnType = node.getReturnType();
			start = sourcePositions.getStartPosition(unit, returnType);
			end = sourcePositions.getEndPosition(unit, returnType);
			if ( returnType.getKind() == Tree.Kind.PRIMITIVE_TYPE) {
				richTextArea.setStyle(fromCharIndex(unit, (int) start), fromCharIndex(unit, (int) end), red);
			} else {
				richTextArea.setStyle(fromCharIndex(unit, (int) start), fromCharIndex(unit, (int) end), azure);
			}
			//richTextArea.setStyle(fromCharIndex(unit, (int) start), fromCharIndex(unit, (int) end), azure);
			//colorize method name
			start = end + 1;
			end = start +  node.getName().length() ;
			richTextArea.setStyle(fromCharIndex(unit, (int)start), fromCharIndex(unit, (int) end), green);
		} else {
			//colorize constructor name
			if (!node.getModifiers().toString().isEmpty()) {
				start = sourcePositions.getEndPosition(unit, node.getModifiers()) + 1;
			} else {
				start = sourcePositions.getStartPosition(unit, node);
			}
			if (start != 0) {
				end = start + className.length();
				richTextArea.setStyle(fromCharIndex(unit, (int) start), fromCharIndex(unit, (int) end), green);
			}		
		}
		//scan parameters throws and body
		node.getParameters().forEach(param -> {
			scan(param, currentPath);
		});
		scan(node.getBody(), currentPath);
		node.getThrows().forEach(throwType -> {
			scan(throwType, currentPath);
		});
		return null;
	}

	@Override
	public Void visitMemberSelect(MemberSelectTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		long end = sourcePositions.getEndPosition(unit, node.getExpression());
		TextPos endPos = fromCharIndex(unit, (int) end);
		int nameLength = node.getIdentifier().length() + 1;
		TextPos endName = new TextPos(endPos.index(), endPos.offset() + nameLength, (int) (end + nameLength), false);
		richTextArea.setStyle(endPos, endName, green);
		scan(node.getExpression(), currentPath);
		return null;
	}

	@Override
	public Void visitMethodInvocation(MethodInvocationTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		scan(node.getMethodSelect(), currentPath);
		node.getArguments().forEach(arg -> {
			scan(arg, currentPath);
		});
		node.getTypeArguments().forEach(typeArg -> {
			scan(typeArg, currentPath);
		});
		return null;
	}

	@Override
	public Void visitBlock(BlockTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		node.getStatements().forEach(statement -> {
			scan(statement, currentPath);
		});
		return null;
	}

	@Override
	public Void visitIf(IfTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		scan(node.getCondition(), parentPath);
		scan(node.getThenStatement(), currentPath);
		if (node.getElseStatement() != null) {
			scan(node.getElseStatement(), currentPath);
		}
		return null;
	}
	
	@Override
	public Void visitTry(TryTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);	
		node.getResources().forEach(statement -> {
			//System.out.println("try ressource: " + statement.getKind());
			scan(statement, currentPath);
		});
		scan(node.getBlock(), currentPath);
		node.getCatches().forEach(statement -> {
			scan(statement, currentPath);
		});
		if (node.getFinallyBlock() != null) {
			scan(node.getFinallyBlock(), currentPath);
		}
		return null;
	}

	/*@Override
	public Void visitThrow(ThrowTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		scan(node.getExpression(), currentPath);
		return null;
	}*/
	
	@Override
	public Void visitForLoop(ForLoopTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		scan(node.getCondition(), currentPath);
		node.getInitializer().forEach(statement -> {
			scan(statement, currentPath);
		});
		scan(node.getStatement(), currentPath);
		return null;
	}
	
	@Override
	public Void visitEnhancedForLoop(EnhancedForLoopTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		scan(node.getVariable(), currentPath);
		scan(node.getExpression(), currentPath);
		scan(node.getStatement(), currentPath);
		return null;
	}

	@Override
	public Void visitSwitch(SwitchTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		scan(node.getExpression(), currentPath);
		node.getCases().forEach(switchCase -> {
			scan(switchCase, currentPath);
		});
		return null;
	}
	
	@Override
	public Void visitSwitchExpression(SwitchExpressionTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		scan(node.getExpression(), currentPath);
		node.getCases().forEach(switchCase -> {
			scan(switchCase, currentPath);
		});
		return null;
	}

	@Override
	public Void visitWhileLoop(WhileLoopTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		scan(node.getCondition(), currentPath);
		scan(node.getStatement(), currentPath);
		return null;
	}

	@Override
	public Void visitVariable(VariableTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		Tree type = node.getType();
		long start = sourcePositions.getStartPosition(unit, type);
		long end = sourcePositions.getEndPosition(unit, type);
		// cas particulier des constantes d'énumération (pas de type)
		if (trees.getElement(currentPath).getKind() == ElementKind.ENUM_CONSTANT) {
			System.out.println("Constante d'énumération détectée: " + node.getName());
			end = start + node.getName().length();
			richTextArea.setStyle(fromCharIndex(unit, (int) start), fromCharIndex(unit, (int) end), blue);
		//cas particulier des paramètres des boucles foreach (pas de type)
		} else if (trees.getElement(currentPath).getKind() == ElementKind.PARAMETER && end == -1) {
			start = sourcePositions.getStartPosition(unit, node);			
			end = start + node.getName().length();
			richTextArea.setStyle(fromCharIndex(unit, (int) start), fromCharIndex(unit, (int) end), violet);
		} else {
			// parse le type de la variable
			TextPos startPos = fromCharIndex(unit, (int) start);
			TextPos endPos;
			end = sourcePositions.getEndPosition(unit, type);
			// type var
			if (end == -1) {
				end = start + "var".length();
				endPos = fromCharIndex(unit, (int) end);
				richTextArea.setStyle(startPos, endPos, brown);
			} else {
				if (type.getKind() == Tree.Kind.PRIMITIVE_TYPE) {
					richTextArea.setStyle(startPos, fromCharIndex(unit, (int) end), red);
				} else {
					endPos = fromCharIndex(unit, (int) end);
					richTextArea.setStyle(startPos, endPos, azure);
				}
			}
			// parse le nom de la variable
			start = end + 1;
			end = start + node.getName().length();
			startPos = fromCharIndex(unit, (int) start);
			endPos = fromCharIndex(unit, (int) end);
			Element element = trees.getElement(currentPath);
			sortByElementKind(startPos, endPos, element);
		}
		// scan l'initialiseur de la variable
		ExpressionTree initializer = node.getInitializer();
		if (initializer != null) {
			scan(initializer, currentPath);
		}
		return null;
	}

	@Override
	public Void visitReturn(ReturnTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		scan(node.getExpression(), currentPath);
		return null;
	}

	@Override
	public Void visitIdentifier(IdentifierTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		long start = sourcePositions.getStartPosition(unit, node);
		long end = sourcePositions.getEndPosition(unit, node);
		if (end == -1) {
			return null;
		}
		TextPos startPos = fromCharIndex(unit, (int) start);
		TextPos endPos = fromCharIndex(unit, (int) end);
		Element element = trees.getElement(currentPath);
		if (element != null) {
			sortByElementKind(startPos, endPos, element);
		} else
			System.out.println(node.getName() + " non résolu.");
		return null;
	}

	@Override
	public Void visitBinary(BinaryTree node, TreePath parentPath) {
		TreePath currentPath = new TreePath(parentPath, node);
		scan(node.getLeftOperand(), currentPath);
		scan(node.getRightOperand(), currentPath);
		return null;
	}

	@Override
	public Void visitLiteral(LiteralTree node, TreePath parentPath) {
		long start = sourcePositions.getStartPosition(unit, node);
		long end = sourcePositions.getEndPosition(unit, node);
		TextPos startPos = fromCharIndex(unit, (int) start);
		TextPos endPos = fromCharIndex(unit, (int) end);
		switch (node.getKind()) {
		case STRING_LITERAL:
		case CHAR_LITERAL:
			richTextArea.setStyle(startPos, endPos, orange);
			break;
		case INT_LITERAL:
		case DOUBLE_LITERAL:
		case FLOAT_LITERAL:
		case LONG_LITERAL:
			richTextArea.setStyle(startPos, endPos, dark);
			break;
		case BOOLEAN_LITERAL:
			break;
		default:
			richTextArea.setStyle(startPos, endPos, dark);
		}
		return null;
	}
	
	@Override
	public Void visitAnnotation(AnnotationTree node, TreePath parentPath) {
        TreePath currentPath = new TreePath(parentPath, node);
        node.getAnnotationType().accept(this, currentPath);
        System.out.println("Visiting annotation: " + node.getAnnotationType().toString());
		node.getArguments().forEach(arg -> {
			System.out.println("Annotation argument kind: " + arg.getKind());
			scan(arg, currentPath);
		});
        return null;
      }

	public void sortByElementKind(TextPos startPos, TextPos endPos, Element element) {
		if (element != null) {
			switch (element.getKind()) {
			case INTERFACE:
			case ENUM:
			case CLASS:
			case PACKAGE:
			case TYPE_PARAMETER:
				richTextArea.setStyle(startPos, endPos, azure);
				//System.out.println(node.getName() + " est une classe. ------");
				break;
			case FIELD:
			case ENUM_CONSTANT:
				richTextArea.setStyle(startPos, endPos, blue);
				//System.out.println(node.getName() + " est un champ (membre de classe). ------------");
				break;
			case LOCAL_VARIABLE:
			case RESOURCE_VARIABLE:
			case BINDING_VARIABLE:
				richTextArea.setStyle(startPos, endPos, brown);
				//System.out.println(node.getName() + " est une variable locale. ----------------");
				break;
			case PARAMETER:
			case EXCEPTION_PARAMETER:
				richTextArea.setStyle(startPos, endPos, violet);
				//System.out.println(node.getName() + " est un paramètre de méthode. ----------------");
				break;
			case METHOD:
			case CONSTRUCTOR:
				richTextArea.setStyle(startPos, endPos, green);
				//System.out.println(node.getName() + " est une méthode. ------------");
				break;
			default:
				System.out.println(element.getSimpleName() + " est de type : " + element.getKind());
			}
		} else {
			System.out.println("Identifier non résolu: ");
		}
	}
	
	public static TextPos fromCharIndex(CompilationUnitTree unit, int charIndex){
		if (charIndex < 0 ) {
	       
	    }
		long index = unit.getLineMap().getLineNumber(charIndex);
		long offset = unit.getLineMap().getColumnNumber(charIndex);
		boolean leading = false;
		if (offset == 0)
			leading = true;
		return new TextPos((int) index - 1, (int) offset - 1, charIndex, leading);
	}

	public static TextPos fromIndexAndOffset(String source, int index, int offset) {
		int charIndex = 0;
		boolean leading = true;
		int currentLine = 0;
		int currentOffset = 0;
		for (int i = 0; i < source.length(); i++) {
			if (source.charAt(i) == '\n') {
				currentLine++;
				currentOffset = 0;
				leading = true;
			} else {
				currentOffset++;
				leading = false;
			}
			if (currentLine == index && currentOffset == offset) {
				charIndex = i;
				break;
			}
		}
		return new TextPos(index, offset, charIndex, leading);
	}

}
