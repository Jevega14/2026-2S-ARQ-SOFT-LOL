package com.apuntado.bootstrap;
import com.apuntado.domain.rules.api.IReglaJuego;
import java.util.*;
public final class RuleRegistry {
 private final Map<String,IReglaJuego> rules=new HashMap<>();
 public RuleRegistry() {
  for(IReglaJuego rule:ServiceLoader.load(IReglaJuego.class)) {
   if(rules.putIfAbsent(rule.id(),rule)!=null) throw new IllegalStateException("Variante duplicada");
  }
  if(rules.isEmpty()) throw new IllegalStateException("Sin proveedores");
 }
 public IReglaJuego get(String id) {
  IReglaJuego rule=rules.get(id);
  if(rule==null) throw new IllegalArgumentException("Variante desconocida");
  return rule;
 }
}
