// @spec spec://modules/android/PROP-010-android-demo-architecture#online
// ponytail: an explicit math signal keeps this free; ambiguous phrasing needs rewording.
const STEMS = `
  matem aritmet algebr geometr trigonometr sinus sine cosinus cosine tangent seno coseno cossen
  hipoten hypotenus catet angle angul triangul circul circunfer radiu diamet
  perimet area volumen volume frac fracc decimal percent porcent ecuacion equa
  equation function funcion derivad derivat integral limite limit vector matriz matrix
  probab statist estadist prime primo ratio razon proporc theor teorem poligon
  calcul calcular calculat comput sumando sumar resta subtract multipli divisi divid
  raiz root cuadrad square potenc power logarit logarithm graf graph solve resolv factor degree grado
  papapy papapykuaa
  матем арифмет алгебр геометр тригонометр синус косинус тангенс гипотенуз катет
  угол треуголь окружн радиус диаметр периметр площад объем дроб десятич процент
  уравнен функц производн интеграл предел вектор матриц вероятност статистик
  числ сумм вычит умнож делен вычисл корен квадрат степен логарифм график теорем градус фактор разлож
`.trim().split(/\s+/);
const EXACT = new Set(["sin", "cos", "tan", "log", "ln", "pi", "sum"]);

export function isMathQuestion(question) {
  const text = question.normalize("NFKD").replace(/\p{M}/gu, "").toLowerCase();
  if (/[√∑∫π∞≠≤≥≈]/u.test(text) || /\d+\s*[%°]/u.test(text)) return true;
  const withoutDates = text.replace(/\b\d{4}-\d{2}-\d{2}\b/g, "");
  if (/(?:\d|[xyz])\s*[+\-−*/×÷^=<>]\s*(?:\d|[xyz(])/u.test(withoutDates)) return true;
  return (text.match(/\p{L}+/gu) || []).some(word => EXACT.has(word) || STEMS.some(stem => word.startsWith(stem)));
}
