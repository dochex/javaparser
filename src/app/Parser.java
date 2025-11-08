package app;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;

public class Parser {

    private ClassTree classTree;
    private String className;
    private String packageName;
	Trees trees;
	CompilationUnitTree compilationUnitTree;


	public Parser(File file) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null,StandardCharsets.UTF_8)) {
            final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(file));
            final JavacTask javacTask = (JavacTask) compiler.getTask(null, fileManager, null, null, null, compilationUnits);
           Iterable<? extends CompilationUnitTree> compilationUnitTrees;
            compilationUnitTrees = javacTask.parse();
            javacTask.analyze();           
            classTree = (ClassTree) compilationUnitTrees.iterator().next().getTypeDecls().get(0);
            compilationUnitTree = compilationUnitTrees.iterator().next();
            trees = Trees.instance(javacTask);
            className = classTree.getSimpleName().toString();
            packageName = compilationUnitTrees.iterator().next().getPackageName().toString();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

	public String getClassName() {
		return className;
	}

	public String getPackageName() {
		return packageName;
	}



}

