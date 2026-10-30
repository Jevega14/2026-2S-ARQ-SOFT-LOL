package com.apuntado.domain.rules.api;
import com.apuntado.domain.model.Hand;
public interface IReglaJuego {
 String id();
 boolean permiteCerrar(Hand hand);
}
