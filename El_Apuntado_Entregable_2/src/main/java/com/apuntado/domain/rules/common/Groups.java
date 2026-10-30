package com.apuntado.domain.rules.common;
import com.apuntado.domain.model.Card;
import java.util.*;
/** Perfil MVP1: conjuntos por rango y palo distinto, o secuencias del mismo palo; sin comodines. */
public final class Groups {
 private Groups() {}
 public static boolean valid(List<Card> group) { return sameRank(group) || run(group); }
 private static boolean sameRank(List<Card> group) {
  return group.stream().map(Card::rank).distinct().count()==1
   && group.stream().map(Card::suit).distinct().count()==group.size();
 }
 private static boolean run(List<Card> group) {
  if (group.stream().map(Card::suit).distinct().count()!=1) return false;
  List<Integer> ranks=group.stream().map(Card::rank).sorted().toList();
  for(int i=1;i<ranks.size();i++) {
   if(ranks.get(i)!=ranks.get(i-1)+1) return false;
  }
  return true;
 }
 public static boolean matches(List<Card> cards, int[] sizes, int maxRemainder) {
  return assign(cards,sizes,0,maxRemainder);
 }
 private static boolean assign(List<Card> cards,int[] sizes,int index,int max) {
  if(index==sizes.length) return cards.stream().mapToInt(Card::points).sum()<=max;
  for(List<Card> group: combinations(cards,sizes[index])) {
   if(valid(group)) {
    List<Card> remaining=new ArrayList<>(cards); remaining.removeAll(group);
    if(assign(remaining,sizes,index+1,max)) return true;
   }
  }
  return false;
 }
 private static List<List<Card>> combinations(List<Card> cards,int size) {
  List<List<Card>> result=new ArrayList<>();
  collect(cards,size,0,new ArrayList<>(),result); return result;
 }
 private static void collect(List<Card> cards,int size,int start,List<Card> chosen,List<List<Card>> result) {
  if(chosen.size()==size) {result.add(List.copyOf(chosen)); return;}
  for(int i=start;i<cards.size();i++) {
   chosen.add(cards.get(i)); collect(cards,size,i+1,chosen,result); chosen.remove(chosen.size()-1);
  }
 }
}
