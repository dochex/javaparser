package app;

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePathScanner;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.util.Elements;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassTestWithNestedClass {
    public static void main(String... args) throws IOException {
        try {
            var ok = new ClassTestWithNestedClass().run(args);
            if (!ok) {
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("IO Exception: " + e.getMessage());
            System.exit(2);
        }
    }

    public boolean run(String... args) throws IOException {
        List<String> options = List.of();

        var files = findJavaFiles(Stream.of(args)
                .map(Path::of)
                .toList());

        var c = ToolProvider.getSystemJavaCompiler();
        var fm = c.getStandardFileManager(null, Locale.getDefault(), StandardCharsets.UTF_8);
        var fileObjects = fm.getJavaFileObjectsFromPaths(files);
        var t = (JavacTask) c.getTask(null, fm, null, options, null, fileObjects);
        t.addTaskListener(new TaskListener() {
            @Override
            public void finished(TaskEvent ev) {
                if (ev.getKind() == TaskEvent.Kind.ENTER) {
                    var tree = ev.getCompilationUnit();
                    var packageName = tree.getPackageName();
                    if (packageName != null) {
                        var pe = elements.getPackageElement(packageName.toString());
                        if (pe != null) {
                            var me = elements.getModuleOf(pe);
                            if (me != null) {
                                var exported = me.getDirectives().stream()
                                        .filter(d -> d.getKind() == ModuleElement.DirectiveKind.EXPORTS)
                                        .map(ModuleElement.ExportsDirective.class::cast)
                                        .anyMatch(ed -> ed.getPackage() == pe);
                                if (!exported) {
                                    return;
                                }
                            }
                        }
                    }
                    var file = fm.asPath(ev.getSourceFile());
                    try {
                        processFile(file, tree);
                    } catch (IOException e) {
                        error(file, "IO exception: " + e);
                    }
                }
            }
        });

        elements = t.getElements();
        docTrees = DocTrees.instance(t);
        positions = docTrees.getSourcePositions();
        t.analyze();

        if (errors == 0) {
            return true;
        } else {
            log.println(errors + " errors");
            return false;
        }

    }

    private PrintStream out = System.out;
    private PrintStream log = System.err;

    private Path userDir = Path.of(System.getProperty("user.dir"));
    private int errors;
    private Elements elements;
    private DocTrees docTrees;
    private DocSourcePositions positions;
    private DeclScanner declScanner = new DeclScanner();

    void processFile(Path file, CompilationUnitTree compUnit) throws IOException {
        var linkScanner = new LinkScanner(compUnit);
        declScanner.scan(compUnit, linkScanner);

        out.println("*** file " + userDir.relativize(file));
        linkScanner.urls.forEach((u -> out.println("   " + u)));
    }

    List<Path> findJavaFiles(List<Path> files) throws IOException {
        List<Path> list = new ArrayList<>();
        for (var f : files) {
            if (Files.isRegularFile(f) && f.getFileName().toString().endsWith(".java")) {
                list.add(f);
            } else if (Files.isDirectory(f)) {
                Files.walkFileTree(f, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return dir.getFileName().toString().equals("internal")
                                ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.getFileName().toString().endsWith(".java")) {
                            list.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
        list.sort(Comparator.naturalOrder());
        return list;
    }

    void error(Path file, String message) {
        log.println(file + ": " + message);
        errors++;
    }

    class DeclScanner extends TreePathScanner<Void,Void> {
        private LinkScanner linkScanner;

        void scan(CompilationUnitTree tree, LinkScanner linkScanner) {
            this.linkScanner = linkScanner;
            super.scan(tree, null);
        }

        @Override
        public Void visitModule(ModuleTree tree, Void p) {
            processCurrentPath();
            return super.visitModule(tree, p);
        }

        @Override
        public Void visitPackage(PackageTree tree, Void p) {
            processCurrentPath();
            return super.visitPackage(tree, p);
        }

        @Override
        public Void visitClass(ClassTree tree, Void p) {
            processCurrentPath(tree.getModifiers());
            return super.visitClass(tree, p);

        }

        @Override
        public Void visitVariable(VariableTree tree, Void p) {
            processCurrentPath(tree.getModifiers());
            return null; // do not scan within the declaration
        }

        @Override
        public Void visitMethod(MethodTree tree, Void p) {
            processCurrentPath(tree.getModifiers());
            return null; // do not scan within the declaration
        }

        void processCurrentPath(ModifiersTree tree) {
            var mods = tree.getFlags();
            if (mods.contains(Modifier.PUBLIC) || mods.contains(Modifier.PROTECTED)) {
                processCurrentPath();
            }
        }

        void processCurrentPath() {
            var dct = docTrees.getDocCommentTree(getCurrentPath());
            if (dct != null) {
                linkScanner.scan(getCurrentPath().getLeaf(), dct);
            }
        }
    }

    class LinkScanner extends DocTreeScanner<Void, Void> {
        final Set<String> urls = new TreeSet<>();

        private final CompilationUnitTree compUnit;
        private Tree declTree;
        private DocCommentTree docComment;

        private StartElementTree startTree;
        private SeeTree seeTree;

        LinkScanner(CompilationUnitTree compUnit) {
            this.compUnit = compUnit;
        }

        void scan(Tree declTree, DocCommentTree docComment) {
            this.declTree = declTree;
            this.docComment = docComment;

            startTree = null;
            docComment.accept(this, null);
        }

        @Override
        public Void visitStartElement(StartElementTree tree, Void p) {
            if (matches(tree.getName(), "a")) {
                startTree = tree;

                try {
                    // visit attributes
                    super.visitStartElement(tree, p);
                } finally {
                    startTree = null;
                }
            }

            return null;
        }

        @Override
        public Void visitAttribute(AttributeTree tree, Void p) {
            if (startTree != null && matches(tree.getName(), "href")) {
                var url = tree.getValue().stream().map(Object::toString).collect(Collectors.joining());
                urls.add(url);
            }
            return null;
        }

        private boolean matches(Name tagName, String s) {
            return tagName.toString().equalsIgnoreCase(s);
        }
    }
}
