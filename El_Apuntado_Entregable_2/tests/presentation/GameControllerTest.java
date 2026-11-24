package com.apuntado.tests.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class GameControllerTest {
 private static final String CREATE_BODY = """
  {"variantId":"CLASICA","players":[
    {"playerId":"p1","seat":0,"displayName":"Ana"},
    {"playerId":"p2","seat":1,"displayName":"Luis"}]}
  """;
 @Autowired
 private MockMvc mvc;

 @Test
 void exposesRegisteredVariants() throws Exception {
  mvc.perform(get("/api/variants"))
   .andExpect(status().isOk())
   .andExpect(jsonPath("$[0].id").value("APUNTADO101"))
   .andExpect(jsonPath("$[1].id").value("CLASICA"));
 }

 @Test
 void createsReadsAndPlaysAGame() throws Exception {
  String body = mvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
   .andExpect(status().isCreated())
   .andExpect(jsonPath("$.status").value("ACTIVE"))
   .andExpect(jsonPath("$.hand.length()").value(10))
   .andExpect(jsonPath("$.players.length()").value(2))
   .andReturn().getResponse().getContentAsString();
  String gameId = JsonPath.read(body, "$.gameId");

  mvc.perform(get("/api/games/{gameId}", gameId).param("viewerId", "p1"))
   .andExpect(status().isOk())
   .andExpect(jsonPath("$.turn.phase").value("DRAW"))
   .andExpect(jsonPath("$.hand.length()").value(10));

  mvc.perform(post("/api/games/{gameId}/draw-deck", gameId)
    .contentType(MediaType.APPLICATION_JSON)
    .content("{\"playerId\":\"p1\",\"commandId\":\"cmd-1\"}"))
   .andExpect(status().isOk())
   .andExpect(jsonPath("$.hand.length()").value(11))
   .andExpect(jsonPath("$.turn.phase").value("DISCARD"));
 }

 @Test
 void rejectsOutOfTurnWithConflictStatus() throws Exception {
  String body = mvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
   .andReturn().getResponse().getContentAsString();
  String gameId = JsonPath.read(body, "$.gameId");
  mvc.perform(post("/api/games/{gameId}/draw-deck", gameId)
    .contentType(MediaType.APPLICATION_JSON)
    .content("{\"playerId\":\"p2\",\"commandId\":\"cmd-2\"}"))
   .andExpect(status().isConflict())
   .andExpect(jsonPath("$.error").exists());
 }

 @Test
 void rejectsInvalidBody() throws Exception {
  mvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON)
    .content("{\"variantId\":\"CLASICA\",\"players\":[]}"))
   .andExpect(status().isBadRequest());
 }
}
