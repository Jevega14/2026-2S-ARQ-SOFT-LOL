# Prueba de proveedor externo
DEMO demuestra carga de un proveedor, no una modalidad de juego válida.
Tras mvn verify, compilar DemoRule.java y Probe.java con javac -cp ../../target/classes -d build.
Crear build/META-INF/services/com.apuntado.domain.rules.api.IReglaJuego con una línea: demo.DemoRule.
Empaquetar build en demo-rule.jar y ejecutar Probe con classpath target/classes y el JAR.
No recompilar TurnOrchestratorComponent durante esta comprobación. Comparar su hash antes y después.
La evidencia local de carga y del hash aparece en docs/evidence/verification.json.
