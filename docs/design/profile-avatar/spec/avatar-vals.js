// Вычисление значений для avatar-template.svg из AvatarLook. Портировать 1:1 в Kotlin.
function headVals(L /* AvatarLook */) {
    var mix = function (hex, t, to) {
      var n = parseInt(String(hex).replace('#', ''), 16);
      var r = (n >> 16) & 255, g = (n >> 8) & 255, b = n & 255;
      var tv = to === 'w' ? 255 : 0;
      r = Math.round(r + (tv - r) * t); g = Math.round(g + (tv - g) * t); b = Math.round(b + (tv - b) * t);
      return '#' + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
    };
    var star = function (cx, cy, R, r) {
      var d = '';
      for (var i = 0; i < 10; i++) {
        var a = -Math.PI / 2 + i * Math.PI / 5; var rad = i % 2 === 0 ? R : r;
        d += (i === 0 ? 'M' : 'L') + (cx + rad * Math.cos(a)).toFixed(1) + ' ' + (cy + rad * Math.sin(a)).toFixed(1) + ' ';
      }
      return d + 'Z';
    };
    var ex = L.expr, hs = L.hairStyle, hat = L.hat, gl = L.glasses, of = L.outfit;
    var brows = { normal: 'M75 78 Q84 73 92 77 M108 77 Q116 73 125 78', up: 'M75 72 Q84 65 92 70 M108 70 Q116 65 125 72', cool: 'M74 70 Q83 62 92 68 M108 77 Q116 74 125 77' };
    var bk = (ex === 'surprised' || ex === 'star') ? 'up' : (ex === 'cool' ? 'cool' : 'normal');
    return {
      hasBg: L.bg !== 'none', bg: L.bg,
      skin: L.skin, skinShade: mix(L.skin, 0.14, 'b'),
      hair: L.hair, hairHi: mix(L.hair, 0.35, 'w'), hairDark: mix(L.hair, 0.25, 'b'),
      shirt: L.shirt, shirtShade: mix(L.shirt, 0.2, 'b'),
      oTee: of === 'tee', oHoodie: of === 'hoodie',
      hBuzz: hs === 'buzz', hShort: hs === 'short', hCurly: hs === 'curly', hLong: hs === 'long',
      hBun: hs === 'bun' && hat !== 'cap' && hat !== 'beanie' && hat !== 'grad',
      browD: brows[bk],
      eOpen: ex === 'smile' || ex === 'surprised', eyeRx: ex === 'surprised' ? 7 : 5.5, eyeRy: ex === 'surprised' ? 8 : 6.5,
      eHappy: ex === 'happy', eWink: ex === 'wink', eCool: ex === 'cool', eStar: ex === 'star',
      starEyes: star(84, 92, 10, 4.5) + ' ' + star(116, 92, 10, 4.5),
      mSmile: ex === 'smile', mOpen: ex === 'happy' || ex === 'star', mSmirk: ex === 'wink' || ex === 'cool', mO: ex === 'surprised',
      gRound: gl === 'round', gSun: gl === 'sun', gStar: gl === 'star',
      starLenses: star(84, 92, 16, 8) + ' ' + star(116, 92, 16, 8),
      tCap: hat === 'cap', tBeanie: hat === 'beanie', tPhones: hat === 'phones', tPiri: hat === 'piri', tGrad: hat === 'grad', tCrown: hat === 'crown'
    };
  }
module.exports = { headVals };
