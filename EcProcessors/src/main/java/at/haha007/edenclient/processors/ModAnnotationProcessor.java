package at.haha007.edenclient.processors;

import at.haha007.edenclient.annotations.Mod;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes(value = "at.haha007.edenclient.annotations.Mod")
public class ModAnnotationProcessor extends AbstractProcessor {
    private static boolean processed = false;

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (processed) return true;
        processed = true;
        String classes = roundEnv.getElementsAnnotatedWith(Mod.class).stream()
                .map(TypeElement.class::cast)
                .map(TypeElement::getQualifiedName)
                .map(Objects::toString)
                .collect(Collectors.joining("\n"));
        try {
            FileObject filer = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "mods.txt");
            try (Writer writer = filer.openWriter()) {
                writer.append(classes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}