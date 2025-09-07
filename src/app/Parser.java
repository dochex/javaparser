package app;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;

public class Parser {


    List<? extends ImportTree>  importTree;
    List<MethodTree> methodList = new ArrayList<>();
    List<ClassTree> kindClassList = new ArrayList<>();
    List<VariableTree> variableList = new ArrayList<>();
    private ClassTree classTree;
    private String className;
    private String packageName;


    public static void main(String[] args) {
        new Parser(new File("C:\\Users\\Philippe\\eclipse-workspace\\javaparser\\src\\javaparser\\GuiController2.java"));

    }

    public Parser(File file) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null,StandardCharsets.UTF_8)) {
            final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(file));
            final JavacTask javacTask = (JavacTask) compiler.getTask(null, fileManager, null, null, null, compilationUnits);
            Iterable<? extends CompilationUnitTree> compilationUnitTrees;
            compilationUnitTrees = javacTask.parse();
            classTree = (ClassTree) compilationUnitTrees.iterator().next().getTypeDecls().get(0);
            className = classTree.getSimpleName().toString();
            //System.out.println(classTree.toString());
            importTree = (List<? extends ImportTree>) compilationUnitTrees.iterator().next().getImports();
            packageName = compilationUnitTrees.iterator().next().getPackageName().toString();
            //compilationUnitTrees.forEach(System.out::println);
            final List<? extends Tree> classMemberList = classTree.getMembers();
            for (Tree tree : classMemberList) {

                if (tree.getKind() == Tree.Kind.CLASS) {
                    ClassTree kindClassTree = (ClassTree) tree;
                    kindClassList.add(kindClassTree);
                }
                if (tree.getKind() == Tree.Kind.VARIABLE) {
                    VariableTree variableTree = (VariableTree) tree;
                    variableList.add(variableTree);
                }
                if (tree.getKind() == Tree.Kind.METHOD) {
                    MethodTree methodTree = (MethodTree) tree;
                    methodList.add(methodTree);
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }


    public List<? extends ImportTree> getImportTree() {
        return importTree;
    }

    public List<VariableTree> getVariableList() {
        return variableList;
    }

    public ClassTree getClassTree() {
        return classTree;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<MethodTree> getMethodList() {
        return methodList;
    }

	public List<ClassTree> getKindClassList() {
		return kindClassList;
	}

	public String getClassName() {
		return className;
	}


}

