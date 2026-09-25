import * as THREE from "three";
import { RoundedBoxGeometry } from "three/addons/geometries/RoundedBoxGeometry.js";
import { FontLoader } from "three/addons/loaders/FontLoader.js";
import { TextGeometry } from "three/addons/geometries/TextGeometry.js";

// @spec spec://modules/learning/FEAT-010-learning-demo#course-map
const SIZE = 768;
const scene = new THREE.Scene();
const renderer = new THREE.WebGLRenderer({
  alpha: true,
  antialias: true,
  preserveDrawingBuffer: true,
  premultipliedAlpha: false,
});
renderer.setPixelRatio(1);
renderer.setSize(SIZE, SIZE, false);
renderer.setClearColor(0x000000, 0);
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.12;
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
document.body.appendChild(renderer.domElement);

const camera = new THREE.OrthographicCamera(-1.75, 1.75, 1.75, -1.75, 0.1, 40);
camera.position.set(5, 7.06, 5);
camera.lookAt(0.04, 1.35, 0);

scene.add(new THREE.HemisphereLight(0xEAF8FF, 0x6F8EAD, 1.1));
scene.add(new THREE.AmbientLight(0x9DCBE5, 0.22));

const key = new THREE.DirectionalLight(0xFFF9EE, 2.6);
key.position.set(-2, 8.5, 2.5);
key.castShadow = true;
key.shadow.mapSize.set(2048, 2048);
key.shadow.camera.left = -2.5;
key.shadow.camera.right = 2.5;
key.shadow.camera.top = 2.5;
key.shadow.camera.bottom = -2.5;
key.shadow.camera.near = 0.1;
key.shadow.camera.far = 18;
key.shadow.bias = -0.0003;
key.shadow.normalBias = 0.015;
key.shadow.radius = 4;
scene.add(key);

const fill = new THREE.DirectionalLight(0xB8E7FF, 0.4);
fill.position.set(4, 2.5, 3);
scene.add(fill);

const ground = new THREE.Mesh(
  new THREE.PlaneGeometry(8, 8),
  new THREE.ShadowMaterial({ color: 0x203F5D, opacity: 0.15 }),
);
ground.rotation.x = -Math.PI / 2;
ground.position.y = 0.001;
ground.receiveShadow = true;
scene.add(ground);

const prop = new THREE.Group();
prop.rotation.y = Math.PI / 4;
scene.add(prop);

const baseWidth = 1.78;
const baseHeight = 0.62;
const baseDepth = 0.72;
const base = new THREE.Mesh(
  new RoundedBoxGeometry(baseWidth, baseHeight, baseDepth, 5, 0.045),
  new THREE.MeshStandardMaterial({ color: 0xD8EAF6, roughness: 0.35, metalness: 0 }),
);
base.position.y = baseHeight / 2;
base.castShadow = true;
base.receiveShadow = true;
prop.add(base);

const triangleHeight = 2.1;
const triangleBase = Math.tan(Math.PI / 6) * triangleHeight;
const halfBase = triangleBase / 2;
const triangleOriginX = -0.14;
const triangleBaseY = baseHeight;
const triangleFrontZ = 0.055;
const triangleZ = -0.17;
const triangleDepth = 0.205;
const shape = new THREE.Shape();
shape.moveTo(-halfBase, 0);
shape.lineTo(-halfBase, triangleHeight);
shape.lineTo(halfBase, 0);
shape.closePath();

const prism = new THREE.Mesh(
  new THREE.ExtrudeGeometry(shape, {
    depth: triangleDepth,
    steps: 1,
    curveSegments: 8,
    bevelEnabled: true,
    bevelSize: 0.024,
    bevelThickness: 0.024,
    bevelSegments: 4,
  }),
  [
    new THREE.MeshStandardMaterial({ color: 0xC5F0FA, roughness: 0.35, metalness: 0 }),
    new THREE.MeshStandardMaterial({ color: 0x548EB9, roughness: 0.35, metalness: 0 }),
  ],
);
prism.position.set(triangleOriginX, triangleBaseY, triangleZ);
prism.castShadow = true;
prism.receiveShadow = true;
prop.add(prism);

const font = await new FontLoader().loadAsync("/assets/helvetiker_bold.typeface.json");
const textFace = new THREE.MeshStandardMaterial({ color: 0x123C59, roughness: 0.38, metalness: 0 });
const textSide = new THREE.MeshStandardMaterial({ color: 0x0A2D49, roughness: 0.42, metalness: 0 });

function makeRaisedText(text, size, depth) {
  const geometry = new TextGeometry(text, {
    font,
    size,
    depth,
    curveSegments: 8,
    bevelEnabled: true,
    bevelThickness: 0.0025,
    bevelSize: 0.0025,
    bevelSegments: 2,
  });
  geometry.computeBoundingBox();
  const box = geometry.boundingBox;
  geometry.translate(-(box.min.x + box.max.x) / 2, -(box.min.y + box.max.y) / 2, 0);
  const textMesh = new THREE.Mesh(geometry, [textFace, textSide]);
  textMesh.castShadow = false;
  textMesh.receiveShadow = true;
  textMesh.userData.width = box.max.x - box.min.x;
  return textMesh;
}

function addTriangleText(text, position, size, rotation = 0) {
  const label = makeRaisedText(text, size, 0.012);
  label.position.set(position.x, position.y, triangleFrontZ + 0.008);
  label.rotation.z = rotation;
  prop.add(label);
  return label;
}

const triangleLeftFoot = new THREE.Vector3(triangleOriginX - halfBase, triangleBaseY, triangleFrontZ);
const apex = new THREE.Vector3(triangleOriginX - halfBase, triangleBaseY + triangleHeight, triangleFrontZ);
const triangleRightFoot = new THREE.Vector3(triangleOriginX + halfBase, triangleBaseY, triangleFrontZ);
const hypotenuse = triangleRightFoot.clone().sub(apex);
const hypotenuseMid = apex.clone().addScaledVector(hypotenuse, 0.53);
addTriangleText(
  "15 cm",
  new THREE.Vector3(triangleOriginX - 0.12, triangleBaseY + 0.32, triangleFrontZ),
  0.18,
);

const arcMaterial = new THREE.MeshStandardMaterial({ color: 0x347FA7, roughness: 0.36, metalness: 0 });
const angleArcPoints = Array.from({ length: 25 }, (_, index) => {
  const angle = THREE.MathUtils.degToRad(-90 + (30 * index) / 24);
  return new THREE.Vector3(
    apex.x + 0.28 * Math.cos(angle),
    apex.y + 0.28 * Math.sin(angle),
    triangleFrontZ + 0.006,
  );
});
const angleArc = new THREE.Mesh(
  new THREE.TubeGeometry(new THREE.CatmullRomCurve3(angleArcPoints), 24, 0.012, 8, false),
  arcMaterial,
);
angleArc.castShadow = true;
prop.add(angleArc);

addTriangleText("30°", new THREE.Vector3(apex.x - 0.17, apex.y - 0.06, triangleFrontZ), 0.2);

const rightAngleMaterial = new THREE.MeshStandardMaterial({ color: 0x3980A7, roughness: 0.35 });
const rightAngleGeometry = new THREE.BufferGeometry().setFromPoints([
  new THREE.Vector3(triangleLeftFoot.x + 0.11, triangleLeftFoot.y + 0.012, triangleFrontZ + 0.006),
  new THREE.Vector3(triangleLeftFoot.x + 0.11, triangleLeftFoot.y + 0.1, triangleFrontZ + 0.006),
  new THREE.Vector3(triangleLeftFoot.x + 0.198, triangleLeftFoot.y + 0.1, triangleFrontZ + 0.006),
]);
const rightAngleMark = new THREE.Line(rightAngleGeometry, rightAngleMaterial);
rightAngleMark.computeLineDistances();
prop.add(rightAngleMark);

const hypotenuseText = makeRaisedText("30 cm", 0.2, 0.012);
hypotenuseText.rotation.x = -Math.PI / 2;
hypotenuseText.position.set(0.58, baseHeight + 0.01, 0.2);
prop.add(hypotenuseText);

const hypotenuseLeader = new THREE.Mesh(
  new THREE.TubeGeometry(
    new THREE.LineCurve3(
      hypotenuseMid.clone().setZ(triangleFrontZ + 0.02),
      new THREE.Vector3(0.58 - hypotenuseText.userData.width / 2 - 0.035, baseHeight + 0.01, 0.2),
    ),
    16,
    0.008,
    7,
    false,
  ),
  new THREE.MeshStandardMaterial({ color: 0x347FA7, roughness: 0.36, metalness: 0 }),
);
hypotenuseLeader.castShadow = true;
prop.add(hypotenuseLeader);

const formula = makeRaisedText("sin 30° = 1/2", 0.18, 0.012);
formula.position.set(0, baseHeight / 2, baseDepth / 2 + 0.01);
prop.add(formula);

const vertex = new THREE.Mesh(
  new THREE.SphereGeometry(0.022, 20, 14),
  new THREE.MeshStandardMaterial({ color: 0xF7FDFF, roughness: 0.22, metalness: 0 }),
);
vertex.position.copy(apex).add(new THREE.Vector3(0, 0, 0.012));
vertex.castShadow = true;
prop.add(vertex);

renderer.render(scene, camera);
window.mathPropReady = true;
