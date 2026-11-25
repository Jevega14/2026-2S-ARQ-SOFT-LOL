package com.apuntado.tests;
import org.junit.jupiter.api.Test;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.*;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
class ArchitectureFitnessTest {
 private final JavaClasses code=new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages("com.apuntado");
 @Test void presentationCannotBypassApplication() {
  noClasses().that().resideInAPackage("..presentation..")
   .should().dependOnClassesThat().resideInAnyPackage("..persistence..","..domain..","com.mongodb..","java.sql..").check(code);
 }
 @Test void classicIndependent() {
  noClasses().that().resideInAPackage("..rules.clasica..")
   .should().dependOnClassesThat().resideInAPackage("..rules.apuntado..").check(code);
 }
 @Test void apuntadoIndependent() {
  noClasses().that().resideInAPackage("..rules.apuntado..")
   .should().dependOnClassesThat().resideInAPackage("..rules.clasica..").check(code);
 }
 @Test void noPackageCycles() {slices().matching("com.apuntado.(**) ".trim()).should().beFreeOfCycles().check(code);}
 @Test void domainIsPure() {
  noClasses().that().resideInAPackage("..domain..")
   .should().dependOnClassesThat().resideInAnyPackage("..application..","..bootstrap..","..presentation..","..persistence..","..realtime..","java.net..","java.sql..","com.mongodb..","org.bson..","org.springframework..","jakarta..","io.netty..").check(code);
 }
 @Test void orchestratorDoesNotKnowStrategies() {
  noClasses().that().haveSimpleName("TurnOrchestratorComponent")
   .should().dependOnClassesThat().resideInAnyPackage("..rules.clasica..","..rules.apuntado..","..rules.common..").check(code);
 }
 @Test void handEvaluatorDependsOnRuleAbstractionNotStrategies() {
  classes().that().haveSimpleName("HandEvaluatorComponent")
   .should().dependOnClassesThat().resideInAPackage("..rules.api..").check(code);
  noClasses().that().haveSimpleName("HandEvaluatorComponent")
   .should().dependOnClassesThat().resideInAnyPackage("..rules.clasica..","..rules.apuntado..").check(code);
 }
 @Test void applicationStaysFrameworkAndInfrastructureAgnostic() {
  noClasses().that().resideInAPackage("..application..")
   .should().dependOnClassesThat().resideInAnyPackage(
    "..persistence..","..presentation..","..config..","org.springframework..","com.mongodb..","java.sql..").check(code);
 }
 @Test void persistenceDoesNotKnowPresentation() {
  noClasses().that().resideInAPackage("..persistence..")
   .should().dependOnClassesThat().resideInAPackage("..presentation..").check(code);
 }
 @Test void presentationDoesNotKnowConcreteRules() {
  noClasses().that().resideInAPackage("..presentation..")
   .should().dependOnClassesThat().resideInAnyPackage("..rules.clasica..","..rules.apuntado..","..rules.common..").check(code);
 }
 @Test void realtimeOnlyProjectsApplicationViews() {
  noClasses().that().resideInAPackage("..realtime..")
   .should().dependOnClassesThat().resideInAnyPackage("..domain..","..persistence..","com.mongodb..","java.sql..").check(code);
 }
 @Test void compositionRootDoesNotCreateCycles() {
  slices().matching("com.apuntado.(**)").should().beFreeOfCycles().check(code);
 }
 /** Entregable 3, prueba 2: la UI (presentación y tiempo real) no conoce el almacenamiento de partidas. */
 @Test void userInterfaceIsIsolatedFromGameStorage() {
  noClasses().that().resideInAnyPackage("..presentation..","..realtime..")
   .should().dependOnClassesThat().resideInAnyPackage("..persistence..","com.mongodb..","org.bson..",
    "org.springframework.data..").check(code);
 }
 /** Ley de Deméter entre capas: el adaptador HTTP solo habla con su amigo inmediato (la fachada de aplicación). */
 @Test void presentationOnlyTalksToTheApplicationFacade() {
  classes().that().resideInAPackage("..presentation..")
   .should().onlyDependOnClassesThat().resideInAnyPackage("..presentation..","..application","..application.command..",
    "..application.view..","java..","jakarta..","org.springframework..").check(code);
 }
 @Test void auditAdapterOnlyKnowsTheAuditPort() {
  noClasses().that().resideInAPackage("..audit..")
   .should().dependOnClassesThat().resideInAnyPackage("..domain..","..persistence..","..presentation..","..realtime..")
   .check(code);
 }
 @Test void applicationDoesNotDependOnAdapters() {
  noClasses().that().resideInAPackage("..application..")
   .should().dependOnClassesThat().resideInAnyPackage("..audit..","..realtime..","org.slf4j..").check(code);
 }
}
