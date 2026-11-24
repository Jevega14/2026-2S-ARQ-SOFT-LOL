package com.apuntado.tests;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.metrics.ArchitectureMetrics;
import com.tngtech.archunit.library.metrics.ComponentDependencyMetrics;
import com.tngtech.archunit.library.metrics.MetricsComponent;
import com.tngtech.archunit.library.metrics.MetricsComponents;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureMetricsTest {

    private static final JavaClasses CODE = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.apuntado");

    @Test
    void couplingAndDistanceStayWithinArchitecturalBounds() throws IOException {
        MetricsComponents<JavaClass> modules = MetricsComponents.from(CODE, cls -> moduleOf(cls.getPackageName()));
        ComponentDependencyMetrics moduleMetrics = ArchitectureMetrics.componentDependencyMetrics(modules);

        MetricsComponents<JavaClass> packages = MetricsComponents.from(CODE, JavaClass::getPackageName);
        ComponentDependencyMetrics packageMetrics = ArchitectureMetrics.componentDependencyMetrics(packages);

        assertEquals(0, moduleMetrics.getEfferentCoupling("com.apuntado.domain"),
                "el dominio no debe depender de otros módulos");
        assertTrue(moduleMetrics.getAfferentCoupling("com.apuntado.domain") > 0,
                "el dominio debe ser utilizado por otros módulos");
        assertEquals(0, moduleMetrics.getAfferentCoupling("com.apuntado.presentation"),
                "ningún módulo debe depender de presentación");
        assertTrue(moduleMetrics.getAfferentCoupling("com.apuntado.application") > 0,
                "aplicación debe ser una dependencia estable");
        assertTrue(packageMetrics.getAfferentCoupling("com.apuntado.domain.rules.api") > 0,
                "el contrato de variantes debe ser utilizado");
        assertTrue(packageMetrics.getNormalizedDistanceFromMainSequence("com.apuntado.domain.rules.api") < 0.5,
                "el contrato de variantes debe estar cerca de la secuencia principal");

        String report = render(modules, moduleMetrics, "Módulo")
                + "\n" + render(packages, packageMetrics, "Paquete");

        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target", "architecture-metrics.md"), report, StandardCharsets.UTF_8);
        System.out.println(report);
    }

    private static String moduleOf(String packageName) {
        String prefix = "com.apuntado";
        if (packageName.equals(prefix)) {
            return prefix;
        }
        String suffix = packageName.substring(prefix.length() + 1);
        int dot = suffix.indexOf('.');
        return prefix + "." + (dot < 0 ? suffix : suffix.substring(0, dot));
    }

    private static String render(MetricsComponents<JavaClass> components,
                                 ComponentDependencyMetrics metrics,
                                 String header) {
        List<MetricsComponent<JavaClass>> ordered = new ArrayList<>(components);
        ordered.sort(Comparator.comparing(MetricsComponent::getIdentifier));

        StringBuilder out = new StringBuilder();
        out.append("## Métricas por ").append(header.toLowerCase()).append("\n\n");
        out.append("| ").append(header).append(" | Ca | Ce | I | A | D |\n");
        out.append("| --- | ---: | ---: | ---: | ---: | ---: |\n");
        for (MetricsComponent<JavaClass> component : ordered) {
            String id = component.getIdentifier();
            out.append(String.format("| %s | %d | %d | %.2f | %.2f | %.2f |%n",
                    id,
                    metrics.getAfferentCoupling(id),
                    metrics.getEfferentCoupling(id),
                    metrics.getInstability(id),
                    metrics.getAbstractness(id),
                    metrics.getNormalizedDistanceFromMainSequence(id)));
        }
        return out.toString();
    }
}
