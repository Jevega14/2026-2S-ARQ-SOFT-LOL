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
   .should().dependOnClassesThat().resideInAnyPackage("..application..","..bootstrap..","..presentation..","..persistence..","java.net..","java.sql..","com.mongodb..","org.springframework..","jakarta..","io.netty..").check(code);
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
}
