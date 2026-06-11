<template>
  <div class="three-map">
    <div ref="mapContainer" class="map"></div>
    <div v-if="showBack" class="back-to" @click.stop="backMainBack"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, getCurrentInstance } from 'vue'
import { geoMercator } from 'd3-geo'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { CSS2DRenderer, CSS2DObject } from 'three/examples/jsm/renderers/CSS2DRenderer.js'
import { Reflector } from './Reflector'
import { Tween, Easing, remove as tweenRemove } from '@tweenjs/tween.js'

const props = defineProps<{
  activeTab: number
  hazardPoints?: Array<{ longitude: number; latitude: number; name: string; alarmLevel?: number }>
}>()

const mapContainer = ref<HTMLDivElement | null>(null)
const showBack = ref(false)

// Non-reactive Three.js objects
let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let camera: THREE.PerspectiveCamera | null = null
let controls: OrbitControls | null = null
let labelRenderer: CSS2DRenderer | null = null
let clock: THREE.Clock | null = null

let tween: Tween<any> | null = null
let tweenInnerCircle: Tween<any> | null = null
let tweenOuterCircle: Tween<any> | null = null
let scenePlane: THREE.Mesh | null = null
let pointerPlane: THREE.Mesh | null = null
let innerCirclePlane: THREE.Mesh | null = null
let outerCirclePlane: THREE.Mesh | null = null
let mirrorReflector: Reflector | null = null
let spriteGroup: THREE.Group | null = null
let spriteTextures: THREE.Texture[] = []
let markerRipplingGroup: THREE.Mesh[] = []
let pillarParticles: PillarParticle[] = []
let oldAreas: THREE.Mesh[] | null = null
let animationFrameId: number | null = null

let initAreaCode = ''
let oldChooseCode = ''
let isRotating = false
let sceneOriginY = 0
let sceneOriginX = 0
let cameraOriginPosition = { x: 0, y: 0, z: 0 }
let cameraOriginRotation = { x: 0, y: 0, z: 0 }
let timer: ReturnType<typeof setTimeout> | null = null
const timerTime = 8000
const textureUpdateInterval = 0.15
let _terrainMapCenter = new THREE.Vector3()
let _centerMapSize = new THREE.Vector3()

const codeList = ['100000', '100000B', '510000', '510000B', '440000', '440000B', '440300']

const ALARM_COLORS: Record<number, { pillar: string; particle: string; glow: string }> = {
  0: { pillar: '#91cc75', particle: '#b4e09d', glow: '#3d6b2e' },
  1: { pillar: '#00aaff', particle: '#00ccff', glow: '#005588' },
  2: { pillar: '#ffd666', particle: '#ffe066', glow: '#886600' },
  3: { pillar: '#ff9c2e', particle: '#ffb366', glow: '#884400' },
  4: { pillar: '#ff3333', particle: '#ff6666', glow: '#880000' },
}

const ALARM_PARTICLE_SPEED: Record<number, number> = {
  0: 0.15,
  1: 0.2,
  2: 0.35,
  3: 0.5,
  4: 0.7,
}

interface PillarParticle {
  points: THREE.Points
  baseY: number
  height: number
  speed: number
  count: number
  positions: Float32Array
}

function countTrailingZeros(num: number | string): number {
  let n = Number(num)
  let zeroCount = 0
  while (n % 10 === 0) {
    zeroCount++
    n = Math.floor(n / 10)
  }
  return zeroCount
}

function animate() {
  animationFrameId = requestAnimationFrame(animate)
  if (!renderer || !scene || !camera) return

  controls?.update()
  animateCallback()
  renderer.render(scene, camera)
}

function animateCallback() {
  if (!scene || !camera) return
  tween?.update()
  tweenInnerCircle?.update()
  tweenOuterCircle?.update()
  labelRenderer?.render(scene, camera)

  if (spriteGroup) updateSpriteGroup()
  updateMarkerRippling()
  updatePillarParticles()
}

async function initRenderer(areaCode: string) {
  if (!mapContainer.value) return

  const container = mapContainer.value
  const width = container.clientWidth
  const height = container.clientHeight

  // Scene
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x061820)

  // Camera
  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 10000)
  camera.position.set(0, 200, 200)

  // Renderer
  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(window.devicePixelRatio)
  container.appendChild(renderer.domElement)

  // Controls
  controls = new OrbitControls(camera, renderer.domElement)
  controls.target.set(0.62, 1.64, -5.93)
  controls.mouseButtons = { LEFT: 0 as any, RIGHT: 2 as any, MIDDLE: 1 as any }
  controls.minPolarAngle = -Math.PI / 2
  controls.maxPolarAngle = Math.PI / 2.2
  controls.enableDamping = true
  controls.enableZoom = false
  controls.addEventListener('start', () => {
    stopAutoRotation()
  })
  controls.addEventListener('end', () => {
    if (camera) {
      cameraOriginPosition = { x: camera.position.x, y: camera.position.y, z: camera.position.z }
      cameraOriginRotation = { x: camera.rotation.x, y: camera.rotation.y, z: camera.rotation.z }
    }
    if (scene) {
      sceneOriginY = scene.rotation.y
      sceneOriginX = scene.rotation.x
    }
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      autoRotation('start', sceneOriginY, sceneOriginX)
    }, timerTime)
  })

  // Lights
  const dirLight1 = new THREE.DirectionalLight(0xffffff, 0.5)
  dirLight1.position.set(400, 200, 300)
  const dirLight2 = new THREE.DirectionalLight(0xffffff, 0.6)
  dirLight2.position.set(-400, -200, -300)
  const ambientLight = new THREE.AmbientLight(0xffffff, 1)
  scene.add(dirLight1, dirLight2, ambientLight)

  // CSS2D Label Renderer
  labelRenderer = new CSS2DRenderer()
  labelRenderer.domElement.style.position = 'absolute'
  labelRenderer.domElement.style.top = '0px'
  labelRenderer.domElement.style.pointerEvents = 'none'
  labelRenderer.setSize(width, height)
  container.appendChild(labelRenderer.domElement)

  // Clock
  clock = new THREE.Clock()

  // Click event
  renderer.domElement.addEventListener('click', onMouseClick, false)

  // Resize
  window.addEventListener('resize', onResize)

  animate()
  await initAreas(areaCode, 3)
}

async function initAreas(areaCode: string, depth: number) {
  if (!scene || !camera) return
  initAreaCode = areaCode
  isRotating = false

  if (codeList.indexOf(areaCode) >= 0) {
    markerRipplingGroup = []
    const jsonUrl = '/GeoJson/' + areaCode + '.json'
    const jsonBoundaryUrl = codeList.indexOf(areaCode + 'B') >= 0 ? '/GeoJson/' + areaCode + 'B.json' : '/GeoJson/' + areaCode + '.json'

    const jsonData = await getJsonData(jsonUrl)
    const jsonDataBoundary = await getJsonData(jsonBoundaryUrl)

    const jsonMap = await createJsonMap(jsonData, jsonDataBoundary, depth, areaCode)
    scene.add(jsonMap)

    const zeroNumer = countTrailingZeros(areaCode)

    // Background effects
    const box = new THREE.Box3().setFromObject(jsonMap)
    const center = box.getCenter(new THREE.Vector3())
    const size = box.getSize(new THREE.Vector3())
    _terrainMapCenter = center
    _centerMapSize = size

    // Reflector
    const planeGeometry = new THREE.PlaneGeometry(size.x, size.z)
    const reflector = new Reflector(planeGeometry, {
      clipBias: 0.003,
      textureWidth: (mapContainer.value?.clientWidth || 800) * window.devicePixelRatio,
      textureHeight: (mapContainer.value?.clientHeight || 600) * window.devicePixelRatio,
      color: 0xffffff
    })
    reflector.position.y = zeroNumer <= 2 ? -0.2 : -2
    reflector.rotation.x = -Math.PI / 2
    ;(reflector.material as any).transparent = true
    ;(reflector.material as any).opacity = 0
    scene.add(reflector)
    mirrorReflector = reflector

    let r = Math.min(size.x, size.z)
    const opacity = 1
    if (areaCode === '100000') r = r / 1.8

    // Scene background plane
    const sceneCircleGeo = new THREE.CircleGeometry(r * 2.5, 32)
    const sceneTex = new THREE.TextureLoader().load('/texture/scene-bg2.png')
    const sceneMat = new THREE.MeshBasicMaterial({ map: sceneTex, transparent: true, opacity, side: THREE.FrontSide })
    scenePlane = new THREE.Mesh(sceneCircleGeo, sceneMat)
    setJsonMapCenter(scenePlane, true, [0, 0], 2.5)
    scenePlane.position.y = 0
    scene.add(scenePlane)

    // Pointer plane
    const pointerGeo = new THREE.CircleGeometry(r, 32)
    const pointerTex = new THREE.TextureLoader().load('/texture/point-back.png')
    const pointerMat = new THREE.MeshBasicMaterial({ map: pointerTex, transparent: true, opacity, side: THREE.FrontSide })
    pointerPlane = new THREE.Mesh(pointerGeo, pointerMat)
    setJsonMapCenter(pointerPlane, true, [0, 0], 2.5)
    pointerPlane.position.y = -1
    scene.add(pointerPlane)

    // Inner circle
    let innerR = r
    if (areaCode === '510000') innerR = r / 1.5
    else if (areaCode === '440000') innerR = r / 1.2

    const innerCircleGeo = new THREE.CircleGeometry(innerR, 32)
    const innerCircleTex = new THREE.TextureLoader().load('/texture/compact-circle-back.png')
    const innerCircleMat = new THREE.MeshBasicMaterial({ map: innerCircleTex, transparent: true, opacity, side: THREE.FrontSide })
    innerCirclePlane = new THREE.Mesh(innerCircleGeo, innerCircleMat)
    setJsonMapCenter(innerCirclePlane, true, [0, 0], 2.5)
    innerCirclePlane.position.y = zeroNumer <= 2 ? 0 : -1
    scene.add(innerCirclePlane)

    // Outer circle
    const outerCircleGeo = new THREE.CircleGeometry(innerR, 32)
    const outerCircleTex = new THREE.TextureLoader().load('/texture/loose-circle-back.png')
    const outerCircleMat = new THREE.MeshBasicMaterial({ map: outerCircleTex, transparent: true, opacity, side: THREE.FrontSide })
    outerCirclePlane = new THREE.Mesh(outerCircleGeo, outerCircleMat)
    setJsonMapCenter(outerCirclePlane, true, [0, 0], 2.5)
    outerCirclePlane.position.y = zeroNumer <= 2 ? 0.1 : 1
    scene.add(outerCirclePlane)

    // Clean tweens
    if (tween) tweenRemove(tween)
    tween = null
    if (tweenInnerCircle) tweenRemove(tweenInnerCircle)
    tweenInnerCircle = null
    if (tweenOuterCircle) tweenRemove(tweenOuterCircle)
    tweenOuterCircle = null

    // Record initial camera state
    if (camera) {
      cameraOriginPosition.x = camera.position.x
      cameraOriginPosition.y = camera.position.y
      cameraOriginPosition.z = camera.position.z
      cameraOriginRotation.x = camera.rotation.x
      cameraOriginRotation.y = camera.rotation.y
      cameraOriginRotation.z = camera.rotation.z
    }

    // Auto rotation after delay
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      autoRotation('start', sceneOriginY, sceneOriginX)
    }, timerTime)

    autoRotationInnerCircle(0)
    autoRotationOuterCircle(0)

    initUpSpriteParticle(areaCode)
  }
}

function removeAreas() {
  if (!scene || !camera) return

  camera.position.set(cameraOriginPosition.x, cameraOriginPosition.y, cameraOriginPosition.z)
  camera.rotation.set(cameraOriginRotation.x, cameraOriginRotation.y, cameraOriginRotation.z)

  scene.rotation.y = sceneOriginY
  scene.rotation.x = sceneOriginX

  if (tween) tweenRemove(tween)
  if (tweenInnerCircle) tweenRemove(tweenInnerCircle)
  if (tweenOuterCircle) tweenRemove(tweenOuterCircle)
  tween = null
  tweenInnerCircle = null
  tweenOuterCircle = null

  if (scenePlane) { scene.remove(scenePlane); scenePlane = null }
  if (pointerPlane) { scene.remove(pointerPlane); pointerPlane = null }
  if (innerCirclePlane) { scene.remove(innerCirclePlane); innerCirclePlane = null }
  if (outerCirclePlane) { scene.remove(outerCirclePlane); outerCirclePlane = null }

  removeAreasTree(scene.children)

  if (spriteGroup) {
    scene.remove(spriteGroup)
    spriteGroup.traverse((obj: any) => {
      obj.geometry?.dispose()
      obj.material?.dispose()
    })
    spriteGroup.children = []
    spriteGroup = null
    markerRipplingGroup = []
    spriteTextures = []
  }

  // Clean up pillar particles
  pillarParticles.forEach(pp => {
    pp.points.geometry.dispose()
    ;(pp.points.material as THREE.PointsMaterial).map?.dispose()
    ;(pp.points.material as THREE.PointsMaterial).dispose()
  })
  pillarParticles = []
}

function removeAreasTree(list: THREE.Object3D[]) {
  if (!scene) return
  list.forEach((object: any) => {
    if (object.type === 'Object3D') {
      const isArea = object.userData.isArea
      if (isArea) {
        object.traverse((child: any) => {
          if (child.userData?.isLabel) child.element?.remove()
          child.geometry?.dispose()
          if (Array.isArray(child.material)) {
            child.material.forEach((m: any) => m.dispose())
          } else {
            child.material?.dispose()
          }
          scene!.remove(child)
        })
        scene!.remove(object)
      } else if (isArea === undefined && object.children?.length) {
        removeAreasTree([...object.children])
      }
    }
  })
}

async function getJsonData(url: string): Promise<any> {
  try {
    const res = await fetch(url)
    return await res.json()
  } catch {
    return []
  }
}

async function createJsonMap(data: any, dataBoundary: any, depth: number, areaCode: string): Promise<THREE.Object3D> {
  const map = new THREE.Object3D()
  map.userData.isArea = true

  const offsetXY = geoMercator()

  data.features.forEach((feature: any) => {
    const unit = new THREE.Object3D()
    const { name, centroid, center, adcode } = feature.properties
    unit.name = name
    const { coordinates, type } = feature.geometry

    const point = centroid || center || [0, 0]
    if (name) {
      const label = createJsonMapLabel(name, point, depth, adcode)
      unit.add(label)
    }

    const fn = (coordinate: number[][]) => {
      const mesh = createJsonMapMesh(coordinate, depth, adcode)
      const line = createJsonMapLine(coordinate, depth)
      unit.add(mesh, ...line)
    }

    coordinates.forEach((coordinate: any) => {
      if (type === 'MultiPolygon') {
        const flat = coordinate.reduce((r: any[], a: any) => r.concat(a), [])
        fn(flat)
      } else if (type === 'Polygon') {
        fn(coordinate)
      }
    })
    map.add(unit)
    setJsonMapCenter(map, false, [0, 0], areaCode === '100000' ? 2 : 0.93)
  })

  // Boundary
  const unitBoundary = createJsonMapBoundary(dataBoundary, depth + 0.1)
  map.add(...unitBoundary)

  // Markers (hazard points)
  const markers = await initMarkers(areaCode, depth)
  if (markers.length) map.add(...markers)

  return map
}

function createJsonMapMesh(data: number[][], depth: number, adcode: number, addTexture = false): THREE.Mesh {
  const offsetXY = geoMercator()
  const shape = new THREE.Shape()
  data.forEach((item, idx) => {
    const [x, y] = offsetXY(item as any)!
    if (idx === 0) shape.moveTo(x, -y)
    else shape.lineTo(x, -y)
  })

  const extrudeSettings: THREE.ExtrudeGeometryOptions = {
    depth: addTexture ? 0 : depth,
    steps: 1,
    bevelEnabled: true,
    bevelSegments: 1,
    bevelThickness: 0.1,
    bevelOffset: 0
  }

  const geometry = new THREE.ExtrudeGeometry(shape, extrudeSettings)
  let mesh: THREE.Mesh

  if (addTexture) {
    const texture = new THREE.TextureLoader().load('/texture/gz-map.jpg')
    texture.wrapS = THREE.RepeatWrapping
    texture.wrapT = THREE.RepeatWrapping
    texture.repeat = new THREE.Vector2(adcode === 100000 ? 0.008 : 0.08, adcode === 100000 ? 0.008 : 0.08)
    texture.flipY = false
    texture.rotation = THREE.MathUtils.degToRad(45)

    const material = new THREE.MeshPhongMaterial({ map: texture, transparent: false, opacity: 1 })
    mesh = new THREE.Mesh(geometry, material)
    mesh.userData.isTexture = true
    mesh.position.z = depth
  } else {
    const material = new THREE.MeshPhongMaterial({
      color: '#b4eeea',
      transparent: true,
      opacity: 0.5,
      combine: THREE.MultiplyOperation
    });
    (material as any).userData = { originColor: '#b4eeea', originOpacity: 0.5 }

    const material2 = new THREE.MeshLambertMaterial({ color: '#123024', transparent: true, opacity: 0.8 });
    (material2 as any).userData = { originColor: '#123024', originOpacity: 0.8 }

    mesh = new THREE.Mesh(geometry, [material, material2])
    mesh.userData.isTexture = false
  }

  mesh.userData.adcode = adcode
  mesh.userData.originDepth = depth
  return mesh
}

function createJsonMapBoundary(data: any, depth: number): THREE.Object3D[] {
  const meshs: THREE.Object3D[] = []
  data.features.forEach((feature: any) => {
    const unit = new THREE.Object3D()
    unit.name = feature.properties.name
    const { coordinates, type } = feature.geometry
    const flat = coordinates.reduce((r: any[], c: any) => {
      if (type === 'Polygon') return r.concat(c)
      return r.concat(...c)
    }, [])
    const mesh = createJsonMapMesh(flat, depth, feature.properties.adcode, true)
    unit.add(mesh)
    meshs.push(unit)
  })
  return meshs
}

function createJsonMapLine(data: number[][], depth: number): THREE.Line[] {
  const offsetXY = geoMercator()
  const points = data.map((item) => {
    const [x, y] = offsetXY(item as any)!
    return new THREE.Vector3(x, -y, 0)
  })
  const lineGeometry = new THREE.BufferGeometry().setFromPoints(points)
  const upMat = new THREE.LineBasicMaterial({ linewidth: 10, color: 0xffffff })
  const downMat = new THREE.LineBasicMaterial({ linewidth: 10, color: new THREE.Color('rgb(96,252,255)') })

  const upLine = new THREE.Line(lineGeometry, upMat)
  const downLine = new THREE.Line(lineGeometry.clone(), downMat)
  downLine.position.z = -0.0001
  upLine.position.z = depth + 0.3
  return [upLine, downLine]
}

function createJsonMapLabel(name: string, point: number[], depth: number, adcode: number): CSS2DObject {
  const offsetXY = geoMercator()
  const div = document.createElement('div')
  div.style.color = '#fff'
  div.style.fontSize = '10px'
  div.style.textShadow = '1px 1px 2px #047cd6'
  div.style.opacity = '0.8'
  div.textContent = name
  const label = new CSS2DObject(div)
  const [x, y] = offsetXY(point as any)!
  label.position.set(x, -y, depth)
  label.userData.adcode = adcode
  label.userData.originDepth = depth
  label.userData.isLabel = true
  return label
}

async function initMarkers(areaCode: string, depth: number): Promise<THREE.Object3D[]> {
  const markerList: THREE.Object3D[] = []

  // If we have hazard points from props, create light pillars
  if (props.hazardPoints && props.hazardPoints.length > 0) {
    props.hazardPoints.forEach((hp) => {
      const level = hp.alarmLevel ?? 0
      const marker = createLightPillar(
        [hp.longitude, hp.latitude],
        depth,
        areaCode,
        level,
        hp.name
      )
      markerList.push(marker)
    })
    return markerList
  }

  // Default static markers (from showroom)
  let pointList: Array<{ type: number; point: [number, number]; waring: boolean }> = []
  switch (areaCode) {
    case '100000':
      pointList = [
        { type: 1, point: [102.693453, 30.674545], waring: false },
        { type: 1, point: [113.429919, 23.334643], waring: true },
        { type: 2, point: [102.693453, 30.674545], waring: false },
        { type: 2, point: [113.429919, 23.334643], waring: true }
      ]
      break
    default:
      break
  }

  pointList.forEach((p) => {
    if (p.type === props.activeTab) {
      const markers = createJsonMapMarker(p, depth, areaCode)
      markerList.push(...markers)
    }
  })

  return markerList
}

function createLightPillar(
  lngLat: [number, number],
  depth: number,
  areaCode: string,
  alarmLevel: number,
  _name?: string
): THREE.Group {
  const zeroNumer = countTrailingZeros(areaCode)
  const offsetXY = geoMercator()
  const [x, y] = offsetXY(lngLat)!
  const colors = ALARM_COLORS[alarmLevel] ?? ALARM_COLORS[0]
  const isSmall = zeroNumer <= 4

  const group = new THREE.Group()
  group.position.set(x, -y, depth)

  const pillarHeight = isSmall ? 25 : 40
  const pillarRadiusBottom = isSmall ? 0.8 : 1.5
  const pillarRadiusTop = isSmall ? 0.12 : 0.25

  // 1. Bottom glow ring
  const ringGeo = new THREE.RingGeometry(pillarRadiusBottom * 1.2, pillarRadiusBottom * 2.8, 32)
  const ringMat = new THREE.MeshBasicMaterial({
    color: new THREE.Color(colors.glow),
    transparent: true,
    opacity: 0.4,
    side: THREE.DoubleSide,
  })
  const ring = new THREE.Mesh(ringGeo, ringMat)
  ring.rotation.x = -Math.PI / 2
  ring.position.z = 0.1
  group.add(ring)

  // 2. Main pillar cylinder
  const pillarGeo = new THREE.CylinderGeometry(
    Math.max(0.01, pillarRadiusTop),
    Math.max(0.01, pillarRadiusBottom),
    pillarHeight,
    16,
    1,
    true
  )
  const pillarMat = new THREE.MeshBasicMaterial({
    color: new THREE.Color(colors.pillar),
    transparent: true,
    opacity: 0.5,
    side: THREE.DoubleSide,
  })
  const pillar = new THREE.Mesh(pillarGeo, pillarMat)
  pillar.rotation.x = Math.PI / 2
  pillar.position.z = pillarHeight / 2
  group.add(pillar)

  // 3. Halo cylinder (wider, more transparent)
  const haloGeo = new THREE.CylinderGeometry(
    Math.max(0.01, pillarRadiusTop * 2.2),
    Math.max(0.01, pillarRadiusBottom * 1.7),
    pillarHeight,
    16,
    1,
    true
  )
  const haloMat = new THREE.MeshBasicMaterial({
    color: new THREE.Color(colors.glow),
    transparent: true,
    opacity: 0.12,
    side: THREE.DoubleSide,
  })
  const halo = new THREE.Mesh(haloGeo, haloMat)
  halo.rotation.x = Math.PI / 2
  halo.position.z = pillarHeight / 2
  group.add(halo)

  // 4. Particle system
  const particleCount = 15
  const positions = new Float32Array(particleCount * 3)
  const spread = pillarRadiusBottom * 0.6
  for (let i = 0; i < particleCount; i++) {
    positions[i * 3] = (Math.random() - 0.5) * spread * 2
    positions[i * 3 + 1] = (Math.random() - 0.5) * spread * 2
    positions[i * 3 + 2] = Math.random() * pillarHeight
  }
  const particleGeo = new THREE.BufferGeometry()
  particleGeo.setAttribute('position', new THREE.BufferAttribute(positions, 3))

  const particleTex = new THREE.TextureLoader().load('/texture/ascending-particle.png')
  const particleMat = new THREE.PointsMaterial({
    color: new THREE.Color(colors.particle),
    size: isSmall ? 1.5 : 2.5,
    map: particleTex,
    transparent: true,
    opacity: 0.85,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
    sizeAttenuation: true,
  })
  const points = new THREE.Points(particleGeo, particleMat)
  group.add(points)

  pillarParticles.push({
    points,
    baseY: 0,
    height: pillarHeight,
    speed: ALARM_PARTICLE_SPEED[alarmLevel] ?? 0.15,
    count: particleCount,
    positions,
  })

  // 5. Ripple for alarm points (level >= 3)
  if (alarmLevel >= 3) {
    const rippleGeo = new THREE.CircleGeometry(pillarRadiusBottom * 2, 32)
    const rippleTex = new THREE.TextureLoader().load('/texture/marker-circle.png')
    const rippleColor = alarmLevel === 4 ? '#ff3333' : '#ff9c2e'
    const rippleMat = new THREE.MeshBasicMaterial({
      map: rippleTex,
      transparent: true,
      opacity: 0.8,
      color: rippleColor,
      side: THREE.FrontSide,
    })
    const ripple = new THREE.Mesh(rippleGeo, rippleMat)
    ripple.rotation.x = -Math.PI / 2
    ripple.position.z = 0.1
    ripple.userData.zeroNumer = zeroNumer
    ripple.userData.scale = { x: 1, y: 1, z: 1 }
    group.add(ripple)
    markerRipplingGroup.push(ripple)
  }

  if (isSmall) {
    group.scale.set(0.1, 0.1, 0.1)
  }

  return group
}

function createJsonMapMarker(point: { point: [number, number]; waring: boolean; type?: number }, depth: number, areaCode: string): THREE.Object3D[] {
  const markers: THREE.Object3D[] = []
  const zeroNumer = countTrailingZeros(areaCode)
  const offsetXY = geoMercator()
  const [x, y] = offsetXY(point.point)!
  let color = '#fb7e3e'
  if (point.type === 2) color = '#03ffff'

  // Light pillar (cone)
  const spriteMat = new THREE.MeshStandardMaterial({
    transparent: true,
    color: new THREE.Color('#1195e1'),
    opacity: 0.6,
    emissive: new THREE.Color(color),
    emissiveIntensity: 1.1
  })
  const spriteGeo = new THREE.ConeGeometry(zeroNumer > 4 ? 0.5 : 1, zeroNumer > 4 ? 20 : 30, 32)
  const sprite = new THREE.Mesh(spriteGeo, spriteMat)
  sprite.rotation.x = Math.PI / 2

  // Bottom marker
  const markerGeo = new THREE.CircleGeometry(2, 32)
  const markerTex = new THREE.TextureLoader().load('/texture/marker.png')
  const markerMat = new THREE.MeshBasicMaterial({ map: markerTex, transparent: true, opacity: 0.8, color, side: THREE.FrontSide })
  const marker = new THREE.Mesh(markerGeo, markerMat)
  marker.position.set(x, -y, depth + 1)

  // Ripple effect for warning points
  let marker2: THREE.Mesh | null = null
  if (point.waring) {
    const rippleGeo = new THREE.CircleGeometry(3, 32)
    const rippleTex = new THREE.TextureLoader().load('/texture/marker-circle.png')
    const rippleMat = new THREE.MeshBasicMaterial({ map: rippleTex, transparent: true, opacity: 0.8, color, side: THREE.FrontSide })
    marker2 = new THREE.Mesh(rippleGeo, rippleMat)
    marker2.position.set(x, -y, depth + 1)
    marker2.userData.zeroNumer = zeroNumer
  }

  if (zeroNumer <= 4) {
    sprite.scale.set(0.1, 0.1, 0.1)
    marker.scale.set(0.2, 0.2, 0.2)
    sprite.position.set(x, -y, depth + 2)
    marker.position.set(x, -y, depth + 0.5)
    if (marker2) {
      marker2.scale.set(0.2, 0.2, 0.2)
      marker2.position.set(x, -y, depth + 0.5)
      marker2.userData.scale = { x: 0.2, y: 0.2, z: 0.2 }
    }
  } else {
    sprite.position.set(x, -y, depth + 11)
    if (marker2) {
      marker2.userData.scale = { x: 1, y: 1, z: 1 }
    }
  }

  markers.push(sprite, marker)
  if (marker2) {
    markers.push(marker2)
    markerRipplingGroup.push(marker2)
  }
  return markers
}

function updateMarkerRippling() {
  if (!markerRipplingGroup.length) return
  markerRipplingGroup.forEach((item: any) => {
    const splitNumer = item.userData.zeroNumer <= 4 ? 0.002 : 0.008
    let scaleX = item.scale.x + splitNumer
    let scaleY = item.scale.y + splitNumer
    let scaleZ = item.scale.z + splitNumer
    const base = item.userData.scale
    if (scaleX / base.x >= 1.8) scaleX = base.x
    if (scaleY / base.y >= 1.8) scaleY = base.y
    if (scaleZ / base.z >= 1.8) scaleZ = base.z
    item.scale.set(scaleX, scaleY, scaleZ)
  })
}

function updatePillarParticles() {
  pillarParticles.forEach(pp => {
    const pos = pp.positions
    for (let i = 0; i < pp.count; i++) {
      pos[i * 3 + 2] += pp.speed
      if (pos[i * 3 + 2] > pp.baseY + pp.height) {
        pos[i * 3 + 2] = pp.baseY
        pos[i * 3] = (Math.random() - 0.5) * 2
        pos[i * 3 + 1] = (Math.random() - 0.5) * 2
      }
    }
    pp.points.geometry.attributes.position.needsUpdate = true
  })
}

function setJsonMapCenter(map: THREE.Object3D, notCamera: boolean, offset: number[], offsetDis: number) {
  map.rotation.x = -Math.PI / 2
  const box = new THREE.Box3().setFromObject(map)
  const center = box.getCenter(new THREE.Vector3())
  const size = box.getSize(new THREE.Vector3())

  if (!notCamera && camera) {
    const cameraDistance = Math.max(size.x, size.y, size.z) / offsetDis
    camera.position.copy(center).add(new THREE.Vector3(0, cameraDistance, cameraDistance))
    camera.lookAt(center)
  }

  map.position.x = map.position.x - center.x - offset[0]
  map.position.z = map.position.z - center.z - offset[1]
}

function onMouseClick(_event: MouseEvent) {
  // Disabled: no region pop-up on click
}

function getMeshByCode(code: number, list: THREE.Mesh[]): THREE.Mesh[] {
  return list.filter((it) => it.userData.adcode === code)
}

function revertOldAreas() {
  if (!oldAreas?.length) return
  oldAreas.forEach((areaItem: any) => {
    areaItem.material.forEach((mat: any) => {
      mat.color = new THREE.Color(mat.userData.originColor)
      mat.opacity = mat.userData.originOpacity
    })
    const originDepth = areaItem.userData.originDepth
    const geo = areaItem.geometry as THREE.ExtrudeGeometry
    const opts = { ...geo.parameters.options, depth: originDepth }
    geo.dispose()
    areaItem.geometry = new THREE.ExtrudeGeometry(geo.parameters.shapes, opts)
  })
  oldAreas = null
}

function getAreaMeshes(): THREE.Mesh[] {
  if (!scene) return []
  const allMeshes: THREE.Mesh[] = []
  scene.children.forEach((object: any) => {
    if (object.type === 'Object3D' && object.userData.isArea) {
      object.traverse((child: any) => {
        if (child.type === 'Mesh' && child.geometry?.type === 'ExtrudeGeometry' && !child.userData.isTexture) {
          allMeshes.push(child)
        }
      })
    }
  })
  return allMeshes
}

function onAreaClicked(code: number) {
  if (!code) return
  if (codeList.indexOf(code + '') >= 0) {
    removeAreas()
    showBack.value = true
  }
  const zeroNumer = countTrailingZeros(code)
  const depth = zeroNumer <= 2 ? 0.2 : 2
  initAreas(code + '', depth)
}

function backMainBack() {
  removeAreas()
  initAreas('510000', 3)
  showBack.value = false
}

function stopAutoRotation() {
  if (timer) { clearTimeout(timer); timer = null }
  if (tween) { tween.stop(); tweenRemove(tween); tween = null }
  isRotating = false
  if (controls) controls.enabled = true
}

function autoRotation(type: string, angle: number, anglex: number) {
  isRotating = true
  if (!scene || !camera || !controls) return

  scene.rotation.y = sceneOriginY
  scene.rotation.x = sceneOriginX
  camera.position.set(cameraOriginPosition.x, cameraOriginPosition.y, cameraOriginPosition.z)
  camera.rotation.set(cameraOriginRotation.x, cameraOriginRotation.y, cameraOriginRotation.z)
  controls.enabled = false

  if (type === 'start') {
    tween = new Tween({ y: angle, x: anglex })
      .to({ y: -Math.PI / 6, x: -Math.PI / 10 }, 4000)
      .easing(Easing.Quadratic.InOut)
      .onUpdate((r: any) => {
        if (scene) { scene.rotation.y = r.y; scene.rotation.x = r.x }
      })
      .onComplete(() => {
        if (camera) cameraOriginPosition = { x: camera.position.x, y: camera.position.y, z: camera.position.z }
        tween?.stop()
        tweenRemove(tween!)
        tween = null
        if (timer) clearTimeout(timer)
        isRotating = false
        timer = null
        if (controls) {
          controls.enabled = true
          controls.minPolarAngle = -Math.PI / 4
          controls.maxPolarAngle = Math.PI / 3
        }
        timer = setTimeout(() => autoRotation('end', angle, anglex), timerTime)
      })
      .start()
  } else if (type === 'end') {
    tween = new Tween({ y: -Math.PI / 6, x: -Math.PI / 10 })
      .to({ y: angle, x: anglex }, 4000)
      .easing(Easing.Quadratic.InOut)
      .onUpdate((r: any) => {
        if (scene) { scene.rotation.y = r.y; scene.rotation.x = r.x }
      })
      .onComplete(() => {
        if (camera) cameraOriginPosition = { x: camera.position.x, y: camera.position.y, z: camera.position.z }
        tween?.stop()
        tweenRemove(tween!)
        tween = null
        if (timer) clearTimeout(timer)
        isRotating = false
        timer = null
        if (controls) {
          controls.enabled = true
          controls.minPolarAngle = -Math.PI / 2
          controls.maxPolarAngle = Math.PI / 2.2
        }
        timer = setTimeout(() => autoRotation('start', angle, anglex), timerTime)
      })
      .start()
  }
}

function autoRotationInnerCircle(angle: number) {
  if (!innerCirclePlane) return
  tweenInnerCircle = new Tween({ y: angle })
    .to({ y: Math.PI / 4 }, 8000)
    .easing(Easing.Linear.None)
    .onUpdate((r: any) => { if (innerCirclePlane) innerCirclePlane.rotation.z = r.y })
    .onComplete(() => {
      if (innerCirclePlane) innerCirclePlane.rotation.z = 0
      tweenInnerCircle?.stop()
      if (tweenInnerCircle) tweenRemove(tweenInnerCircle)
      tweenInnerCircle = null
      autoRotationInnerCircle(innerCirclePlane?.rotation.z || 0)
    })
    .start()
}

function autoRotationOuterCircle(angle: number) {
  if (!outerCirclePlane) return
  tweenOuterCircle = new Tween({ y: angle })
    .to({ y: -Math.PI / 6 }, 8000)
    .easing(Easing.Linear.None)
    .onUpdate((r: any) => { if (outerCirclePlane) outerCirclePlane.rotation.z = r.y })
    .onComplete(() => {
      if (outerCirclePlane) outerCirclePlane.rotation.z = 0
      tweenOuterCircle?.stop()
      if (tweenOuterCircle) tweenRemove(tweenOuterCircle)
      tweenOuterCircle = null
      autoRotationOuterCircle(outerCirclePlane?.rotation.z || 0)
    })
    .start()
}

function initUpSpriteParticle(areaCode: string) {
  if (!scene) return
  spriteTextures = []
  const zeroNumer = countTrailingZeros(areaCode)
  const zoomNumber = zeroNumer <= 4 ? 4 : 1
  const group = new THREE.Group()

  const positions = [
    [-100, -50, 50], [-300, -50, -200], [-200, -50, -100], [-100, -50, -100],
    [100, -50, -100], [100, -50, 50], [200, -50, -50], [200, -50, -100], [100, -50, -100]
  ]

  for (let i = 0; i < 9; i++) {
    const tex = new THREE.TextureLoader().load(`/texture/${i + 1}.png`)
    spriteTextures.push(tex)
    const mat = new THREE.SpriteMaterial({ map: tex, transparent: true, opacity: 0.8 })
    const sprite = new THREE.Sprite(mat)
    sprite.userData.index = i
    sprite.position.set(
      positions[i][0] / zoomNumber,
      positions[i][1] / zoomNumber,
      positions[i][2] / zoomNumber
    )
    const scaleNumber = zeroNumer <= 4 ? 5 : 30
    sprite.scale.set(scaleNumber, scaleNumber, scaleNumber)
    group.add(sprite)
  }

  scene.add(group)
  spriteGroup = group
}

function updateSpriteGroup() {
  if (!spriteGroup || !clock) return
  const zeroNumer = countTrailingZeros(initAreaCode)
  const zoomNumber = zeroNumer <= 4 ? 4 : 1

  spriteGroup.children.forEach((sprite: any) => {
    sprite.position.y += zeroNumer <= 4 ? 0.1 : 0.6
    if (sprite.position.y > 90 / zoomNumber) {
      sprite.position.y = -50 / zoomNumber
    }
  })

  const elapsedTime = clock.getElapsedTime()
  if (elapsedTime >= textureUpdateInterval) {
    spriteGroup.children.forEach((sprite: any) => {
      let idx = sprite.userData.index
      if (idx === 8) {
        idx = -1
        sprite.userData.index = 0
      } else {
        sprite.userData.index++
      }
      sprite.material.map = spriteTextures[idx + 1]
    })
    clock.start()
  }
}

function onResize() {
  if (!renderer || !camera || !mapContainer.value || !labelRenderer) return
  const w = mapContainer.value.clientWidth
  const h = mapContainer.value.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
  labelRenderer.setSize(w, h)
}

watch(() => props.activeTab, () => {
  removeAreas()
  initAreas('510000', 3)
  showBack.value = false
})

watch(() => props.hazardPoints, (newVal) => {
  if (newVal && newVal.length > 0 && initAreaCode) {
    removeAreas()
    initAreas(initAreaCode, 3)
    showBack.value = false
  }
})

onMounted(() => {
  initRenderer('510000')
})

onBeforeUnmount(() => {
  removeAreas()
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
  if (timer) clearTimeout(timer)
  renderer?.domElement.removeEventListener('click', onMouseClick, false)
  window.removeEventListener('resize', onResize)
  renderer?.dispose()
  labelRenderer?.domElement.remove()
  controls?.dispose()
})
</script>

<style scoped>
.three-map {
  width: 100%;
  height: 100%;
  position: relative;
}
.three-map .map {
  width: 100%;
  height: 100%;
  position: relative;
}
.three-map .back-to {
  width: 11.5714rem;
  height: 3.4286rem;
  position: absolute;
  left: 20%;
  bottom: 3rem;
  z-index: 2;
  cursor: pointer;
  background: url('@/assets/images/disaster/back-map.png') no-repeat center / 100% 100%;
}
.three-map .back-to:hover {
  opacity: 0.8;
}
</style>
