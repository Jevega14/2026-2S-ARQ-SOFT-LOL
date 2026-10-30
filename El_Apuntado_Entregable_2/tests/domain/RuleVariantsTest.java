package com.apuntado.tests;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.apuntado.domain.model.*;
import com.apuntado.domain.rules.clasica.ReglaClasica;
import com.apuntado.domain.rules.apuntado.ReglaApuntado;
import com.apuntado.domain.scoring.RoundScoringComponent;
import com.apuntado.bootstrap.RuleRegistry;
import java.util.*;
class RuleVariantsTest {
 private List<Card> triple(int rank) {return List.of(new Card(rank+"H",rank,"H",rank),new Card(rank+"D",rank,"D",rank),new Card(rank+"S",rank,"S",rank));}
 private List<Card> nine() {List<Card> c=new ArrayList<>(); for(int rank:new int[]{7,9,11}) c.addAll(triple(rank));return c;}
 private Hand pointed(int value) {List<Card> c=nine();c.add(new Card("rest",value,"C",value));return new Hand(c);}
 @Test void apuntadoAcceptsFour() {assertTrue(new ReglaApuntado().permiteCerrar(pointed(4)));}
 @Test void apuntadoRejectsFive() {assertFalse(new ReglaApuntado().permiteCerrar(pointed(5)));}
 @Test void classicAcceptsFourThreeThree() {List<Card> c=nine();c.add(new Card("7C",7,"C",7));assertTrue(new ReglaClasica().permiteCerrar(new Hand(c)));}
 @Test void classicRejectsThreeTriplesAndFour() {assertFalse(new ReglaClasica().permiteCerrar(pointed(4)));}
 @Test void apontadoRejectsClassicWithHighRemainder() {List<Card> c=nine();c.add(new Card("7C",7,"C",7));assertFalse(new ReglaApuntado().permiteCerrar(new Hand(c)));}
 @Test void duplicateIdentityRejected() {List<Card> c=nine();c.add(c.get(0));assertThrows(IllegalArgumentException.class,()->new Hand(c));}
 @Test void nullCardRejected() {List<Card> c=nine();c.add(null);assertThrows(NullPointerException.class,()->new Hand(c));}
 @Test void beforeDiscardRejected() {List<Card> c=new ArrayList<>(pointed(4).cards());c.add(new Card("extra",2,"S",2));assertThrows(IllegalArgumentException.class,()->new Hand(c));}
 @Test void limitIncludes101() {var s=new RoundScoringComponent();assertFalse(s.reachedLimit(100));assertTrue(s.reachedLimit(101));assertTrue(s.reachedLimit(102));}
 @Test void scoreAccumulates() {assertEquals(101,new RoundScoringComponent().accumulate(97,4));}
 @Test void negativePenaltyRejected() {assertThrows(IllegalArgumentException.class,()->new RoundScoringComponent().accumulate(10,-1));}
 @Test void registryLoadsBoth() {var r=new RuleRegistry();assertEquals("CLASICA",r.get("CLASICA").id());assertEquals("APUNTADO101",r.get("APUNTADO101").id());}
 @Test void unknownVariantFails() {assertThrows(IllegalArgumentException.class,()->new RuleRegistry().get("DESCONOCIDA"));}
 @Test void aceAndFacesUseMvpValues() {assertEquals(1,new Card("a",1,"H",99).points());assertEquals(10,new Card("j",11,"H",99).points());assertEquals(10,new Card("q",12,"H",99).points());assertEquals(10,new Card("k",13,"H",99).points());}
 @Test void closerPenaltyIsZero() {assertEquals(0,new RoundScoringComponent().penalty(pointed(4),true));}
 @Test void rivalPaysWholeHand() {assertEquals(82,new RoundScoringComponent().penalty(pointed(4),false));}
}
