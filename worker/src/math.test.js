import test from "node:test";
import assert from "node:assert/strict";
import { isMathQuestion } from "./math.js";

test("math questions pass and unrelated questions do not", () => {
  for (const question of [
    "Mba'épa seno 30°?", "Papapykuaa: mba'épa 2 + 2?", "¿Cuál es la hipotenusa?",
    "Explain a fraction", "What is sin(30)?", "Как найти площадь треугольника?",
    "2+2", "√9", "What is 10% of 50?", "How do I factor 12?", "What is 30°?",
  ]) assert.equal(isMathQuestion(question), true, question);
  for (const question of [
    "Who won the football match?", "Tell me a joke", "Как приготовить суп?",
    "¿Qué tiempo hace hoy?", "What happened on 2026-09-26?", "",
  ]) assert.equal(isMathQuestion(question), false, question);
});
