package com.apuntado.domain.scoring;
import com.apuntado.domain.model.Hand;
import com.apuntado.domain.model.Card;
public final class RoundScoringComponent {
 public int penalty(Hand hand, boolean closer) {
  if (closer) return 0;
  return hand.cards().stream().mapToInt(Card::points).sum();
 }
 public int accumulate(int current,int penalty) {
  if(current<0 || penalty<0) throw new IllegalArgumentException("Puntajes negativos");
  return Math.addExact(current,penalty);
 }
 public boolean reachedLimit(int score) {return score>=101;}
}
