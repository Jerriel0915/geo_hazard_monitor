// ============================================
// QuantumSphere - TerraMens 3D 虚拟形象（粒子球体动画版）
// 基于 ai.html 实现
// ============================================

import * as THREE from 'three'

/**
 * 球体状态 - 5级状态
 */
export type SphereState = 'normal' | 'info' | 'caution' | 'warning' | 'critical'

/**
 * 交互状态 - 对应 ai.html 的 ACTION 状态
 */
export type ActionState = 'idle' | 'thinking' | 'voicing'

/**
 * 量子球体配置
 */
export interface QuantumSphereConfig {
  size?: number
  color?: {
    core?: string
    glow?: string
  }
  state?: SphereState
  action?: ActionState
  breathing?: boolean
  breathingSpeed?: number
  turbulence?: number // 流体湍流度
}

/**
 * 状态颜色映射 - 绿色→蓝→黄→橙→红
 * 对应 ai.html 的 emotionColors 数组顺序（但将正常改为绿色）
 */
const STATE_COLORS: Record<SphereState, string> = {
  normal: '#00ff5a',    // 绿色（正常）
  info: '#00d4ff',      // 蓝色（INFO）
  caution: '#ffcc00',   // 黄色（WARN）
  warning: '#ff8800',   // 橙色（ALERT）
  critical: '#ff0022'   // 红色（CRITICAL）
}

/**
 * 状态对应的 emotion 索引（0-4）
 */
const STATE_TO_EMOTION: Record<SphereState, number> = {
  normal: 0,
  info: 1,
  caution: 2,
  warning: 3,
  critical: 4
}

/**
 * 动作状态对应的 action 索引（0-2）
 */
const ACTION_TO_INDEX: Record<ActionState, number> = {
  idle: 0,
  thinking: 1,
  voicing: 2
}

/**
 * 获取状态颜色的辅助函数
 */
function getStateColor(state: SphereState): string {
  return STATE_COLORS[state]!
}

/**
 * 顶点着色器 - 3D Perlin Noise 粒子变形
 */
const VERTEX_SHADER = `
uniform float uTime;
uniform float uEmotion;
uniform float uAction;
uniform float uGlobalSpeed;

attribute float aSize;
attribute vec3 aColor;
varying vec3 vColor;

// 3D Perlin Noise 实现平滑流动
vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 permute(vec4 x) { return mod289(((x*34.0)+1.0)*x); }
vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }
vec3 fade(vec3 t) { return t*t*t*(t*(t*6.0-15.0)+10.0); }

float pnoise(vec3 P, vec3 rep) {
  vec3 Pi0 = mod(floor(P), rep); vec3 Pi1 = mod(Pi0 + vec3(1.0), rep);
  Pi0 = mod289(Pi0); Pi1 = mod289(Pi1);
  vec3 Pf0 = fract(P); vec3 Pf1 = Pf0 - vec3(1.0);
  vec4 ix = vec4(Pi0.x, Pi1.x, Pi0.x, Pi1.x); vec4 iy = vec4(Pi0.yy, Pi1.yy); vec4 iz = vec4(Pi0.zzzz); vec4 iw = vec4(Pi1.zzzz);
  vec4 ixy = permute(permute(ix) + iy); vec4 ixy0 = permute(ixy + iz); vec4 ixy1 = permute(ixy + iw);
  vec4 gx0 = ixy0 / 7.0; vec4 gy0 = fract(floor(gx0) / 7.0) - 0.5; gx0 = fract(gx0); vec4 gz0 = vec4(0.5) - abs(gx0) - abs(gy0); vec4 sz0 = step(gz0, vec4(0.0)); gx0 -= sz0 * (step(0.0, gx0) - 0.5); gy0 -= sz0 * (step(0.0, gy0) - 0.5);
  vec4 gx1 = ixy1 / 7.0; vec4 gy1 = fract(floor(gx1) / 7.0) - 0.5; gx1 = fract(gx1); vec4 gz1 = vec4(0.5) - abs(gx1) - abs(gy1); vec4 sz1 = step(gz1, vec4(0.0)); gx1 -= sz1 * (step(0.0, gx1) - 0.5); gy1 -= sz1 * (step(0.0, gy1) - 0.5);
  vec3 g000 = vec3(gx0.x,gy0.x,gz0.x); vec3 g100 = vec3(gx0.y,gy0.y,gz0.y); vec3 g010 = vec3(gx0.z,gy0.z,gz0.z); vec3 g110 = vec3(gx0.w,gy0.w,gz0.w); vec3 g001 = vec3(gx1.x,gy1.x,gz1.x); vec3 g101 = vec3(gx1.y,gy1.y,gz1.y); vec3 g011 = vec3(gx1.z,gy1.z,gz1.z); vec3 g111 = vec3(gx1.w,gy1.w,gz1.w);
  vec4 norm0 = taylorInvSqrt(vec4(dot(g000, g000), dot(g100, g100), dot(g010, g010), dot(g110, g110))); g000 *= norm0.x; g100 *= norm0.y; g010 *= norm0.z; g110 *= norm0.w;
  vec4 norm1 = taylorInvSqrt(vec4(dot(g001, g001), dot(g101, g101), dot(g011, g011), dot(g111, g111))); g001 *= norm1.x; g101 *= norm1.y; g011 *= norm1.z; g111 *= norm1.w;
  float n000 = dot(g000, Pf0); float n100 = dot(g100, vec3(Pf1.x, Pf0.y, Pf0.z)); float n010 = dot(g010, vec3(Pf0.x, Pf1.y, Pf0.z)); float n110 = dot(g110, vec3(Pf1.x, Pf1.y, Pf0.z)); float n001 = dot(g001, vec3(Pf0.x, Pf0.y, Pf1.z)); float n101 = dot(g101, vec3(Pf1.x, Pf0.y, Pf1.z)); float n011 = dot(g011, vec3(Pf0.x, Pf1.y, Pf1.z)); float n111 = dot(g111, Pf1);
  vec3 fade_xyz = fade(Pf0); vec4 n_z = mix(vec4(n000, n100, n010, n110), vec4(n001, n101, n011, n111), fade_xyz.z); vec2 n_yz = mix(n_z.xy, n_z.zw, fade_xyz.y);
  return 2.2 * mix(n_yz.x, n_yz.y, fade_xyz.x);
}

void main() {
  vColor = aColor;
  float time = uTime * (0.2 + uEmotion * 0.2) * uGlobalSpeed;

  float baseRadius = (uAction == 1.0) ? 0.7 : 1.0;
  float noiseFreq = 1.2 + uEmotion * 0.4;
  float noiseAmp = 0.15 + uEmotion * 0.1;

  if(uAction == 2.0) noiseAmp += sin(uTime * 15.0 - position.y * 5.0) * 0.1;

  vec3 noiseInput = position * noiseFreq + vec3(time);
  vec3 displacement = vec3(
    pnoise(noiseInput, vec3(10.0)),
    pnoise(noiseInput + vec3(12.3), vec3(10.0)),
    pnoise(noiseInput - vec3(8.7), vec3(10.0))
  ) * noiseAmp;

  vec3 newPosition = position * baseRadius + displacement;
  vec4 mvPosition = modelViewMatrix * vec4(newPosition, 1.0);
  gl_Position = projectionMatrix * mvPosition;

  float sizeMelt = (uAction == 1.0) ? 0.7 : (uAction == 2.0 ? 1.3 : 1.0);
  gl_PointSize = aSize * sizeMelt * (20.0 / -mvPosition.z);
}
`

/**
 * 片元着色器 - 粒子渲染
 */
const FRAGMENT_SHADER = `
varying vec3 vColor;
void main() {
  float d = distance(gl_PointCoord, vec2(0.5));
  if(d > 0.5) discard;
  float alpha = (1.0 - d * 2.0) * 0.15;
  gl_FragColor = vec4(vColor * 1.5, alpha);
}
`

/**
 * 片元着色器 - 心跳粒子渲染（带透明度控制）
 */
const HEARTBEAT_FRAGMENT_SHADER = `
varying vec3 vColor;
uniform float uOpacity;
void main() {
  float d = distance(gl_PointCoord, vec2(0.5));
  if(d > 0.5) discard;
  float alpha = (1.0 - d * 2.0) * 0.2 * uOpacity;
  gl_FragColor = vec4(vColor * 1.5, alpha);
}
`

/**
 * 量子球体类
 */
export class QuantumSphere {
  private scene!: THREE.Scene
  private camera!: THREE.PerspectiveCamera
  private renderer!: THREE.WebGLRenderer
  private container: HTMLElement
  private animationId: number | null = null

  // 粒子系统
  private geometry!: THREE.BufferGeometry
  private material!: THREE.ShaderMaterial
  private points!: THREE.Points
  private readonly particleCount = 1200

  // 配置
  private config: Required<Omit<QuantumSphereConfig, 'color'>> & {
    color: Required<QuantumSphereConfig['color']>
  }

  // 状态
  private currentState: SphereState = 'normal'
  private currentAction: ActionState = 'idle'
  private time: number = 0

  // 心跳粒子系统
  private heartbeatGeometry?: THREE.BufferGeometry
  private heartbeatMaterial?: THREE.ShaderMaterial
  private heartbeatPoints?: THREE.Points
  private heartbeatActive = false
  private heartbeatStartTime = 0
  private readonly heartbeatParticleCount = 12

  /**
   * 构造函数
   */
  constructor(container: HTMLElement, config: QuantumSphereConfig = {}) {
    this.container = container

    // 默认配置 - 默认使用空闲状态
    const defaultState = config.state || 'normal'
    const defaultAction = config.action || 'idle'
    const stateColor = getStateColor(defaultState)

    this.config = {
      size: config.size || 280,
      color: {
        core: config.color?.core || stateColor,
        glow: config.color?.glow || stateColor
      },
      state: defaultState,
      action: defaultAction,
      breathing: config.breathing !== false,
      breathingSpeed: config.breathingSpeed || 1,
      turbulence: config.turbulence || 1.0
    }

    this.currentState = this.config.state
    this.currentAction = this.config.action

    // 初始化场景
    this.initScene()
    this.initGrid()
    this.initParticleSystem()
    this.resize()
    this.render()

    // 监听窗口大小变化
    window.addEventListener('resize', () => this.resize())
  }

  /**
   * 初始化场景
   */
  private initScene() {
    this.scene = new THREE.Scene()

    // 防止容器尺寸为0导致 aspect 为 NaN
    const width = this.container.clientWidth || 280
    const height = this.container.clientHeight || 280
    const aspect = width / height

    this.camera = new THREE.PerspectiveCamera(60, aspect, 0.1, 1000)
    this.camera.position.z = 4

    this.renderer = new THREE.WebGLRenderer({
      alpha: true,
      antialias: true
    })
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    this.renderer.setSize(width, height)
    this.container.appendChild(this.renderer.domElement)
  }

  /**
   * 初始化网格背景
   */
  private initGrid() {
    // 创建网格参数
    const gridSize = 10
    const gridDivisions = 20
    const gridColor = new THREE.Color(0x004466)

    // 主网格（无中心线）
    const gridHelper1 = new THREE.GridHelper(gridSize, gridDivisions, gridColor, gridColor)
    gridHelper1.position.y = -2
    gridHelper1.material.transparent = true
    gridHelper1.material.opacity = 0.3
    this.scene.add(gridHelper1)

    // 副网格（旋转90度，无中心线）
    const gridHelper2 = new THREE.GridHelper(gridSize, gridDivisions, gridColor, gridColor)
    gridHelper2.position.y = -2
    gridHelper2.rotation.x = Math.PI / 2
    gridHelper2.material.transparent = true
    gridHelper2.material.opacity = 0.2
    this.scene.add(gridHelper2)

    // 添加外围圆形网格
    const circleRadius = 3
    const circleSegments = 64
    const circleGeometry = new THREE.BufferGeometry()
    const circlePositions = []

    for (let i = 0; i <= circleSegments; i++) {
      const theta = (i / circleSegments) * Math.PI * 2
      circlePositions.push(
        Math.cos(theta) * circleRadius,
        -2,
        Math.sin(theta) * circleRadius
      )
    }

    circleGeometry.setAttribute('position', new THREE.Float32BufferAttribute(circlePositions, 3))
    const circleMaterial = new THREE.LineBasicMaterial({
      color: 0x00d4ff,
      transparent: true,
      opacity: 0.4
    })
    const circle = new THREE.Line(circleGeometry, circleMaterial)
    this.scene.add(circle)

    // 添加同心圆
    for (let r = 1; r <= 3; r++) {
      const innerCircleGeometry = new THREE.BufferGeometry()
      const innerCirclePositions = []

      for (let i = 0; i <= circleSegments; i++) {
        const theta = (i / circleSegments) * Math.PI * 2
        innerCirclePositions.push(
          Math.cos(theta) * r,
          -2,
          Math.sin(theta) * r
        )
      }

      innerCircleGeometry.setAttribute('position', new THREE.Float32BufferAttribute(innerCirclePositions, 3))
      const innerCircleMaterial = new THREE.LineBasicMaterial({
        color: 0x00d4ff,
        transparent: true,
        opacity: 0.15
      })
      const innerCircle = new THREE.Line(innerCircleGeometry, innerCircleMaterial)
      this.scene.add(innerCircle)
    }
  }

  /**
   * 初始化粒子系统
   */
  private initParticleSystem() {
    this.geometry = new THREE.BufferGeometry()

    const posArr = new Float32Array(this.particleCount * 3)
    const colArr = new Float32Array(this.particleCount * 3)
    const sizArr = new Float32Array(this.particleCount)

    // 生成球面上的粒子
    for (let i = 0; i < this.particleCount; i++) {
      const theta = Math.random() * Math.PI * 2
      const phi = Math.acos(Math.random() * 2 - 1)

      posArr[i * 3] = Math.sin(phi) * Math.cos(theta)
      posArr[i * 3 + 1] = Math.sin(phi) * Math.sin(theta)
      posArr[i * 3 + 2] = Math.cos(phi)

      sizArr[i] = Math.random() * 3 + 1
    }

    this.geometry.setAttribute('position', new THREE.BufferAttribute(posArr, 3))
    this.geometry.setAttribute('aColor', new THREE.BufferAttribute(colArr, 3))
    this.geometry.setAttribute('aSize', new THREE.BufferAttribute(sizArr, 1))

    this.material = new THREE.ShaderMaterial({
      uniforms: {
        uTime: { value: 0 },
        uEmotion: { value: STATE_TO_EMOTION[this.currentState] },
        uAction: { value: ACTION_TO_INDEX[this.currentAction] },
        uGlobalSpeed: { value: this.config.turbulence }
      },
      vertexShader: VERTEX_SHADER,
      fragmentShader: FRAGMENT_SHADER,
      transparent: true,
      blending: THREE.AdditiveBlending,
      depthTest: false
    })

    this.points = new THREE.Points(this.geometry, this.material)
    this.scene.add(this.points)

    // 初始化颜色
    this.updateColors()
  }

  /**
   * 更新粒子颜色
   */
  private updateColors() {
    const targetColor = new THREE.Color(getStateColor(this.currentState))
    const colors = this.geometry.attributes.aColor.array as Float32Array

    for (let i = 0; i < this.particleCount; i++) {
      colors[i * 3] = targetColor.r + (Math.random() - 0.5) * 0.1
      colors[i * 3 + 1] = targetColor.g + (Math.random() - 0.5) * 0.1
      colors[i * 3 + 2] = targetColor.b + (Math.random() - 0.5) * 0.1
    }

    this.geometry.attributes.aColor.needsUpdate = true
  }

  /**
   * 调整尺寸
   */
  resize(width?: number, height?: number) {
    const w = width || this.container.clientWidth
    const h = height || this.container.clientHeight

    this.camera.aspect = w / h
    this.camera.updateProjectionMatrix()
    this.renderer.setSize(w, h)
  }

  /**
   * 更新状态
   */
  setState(state: SphereState) {
    if (this.currentState === state) return

    this.currentState = state
    this.config.state = state

    // 更新 emotion uniform
    this.material.uniforms.uEmotion.value = STATE_TO_EMOTION[state]

    // 更新颜色
    this.updateColors()
  }

  /**
   * 更新动作状态
   */
  setAction(action: ActionState) {
    if (this.currentAction === action) return

    this.currentAction = action
    this.config.action = action

    // 更新 action uniform
    this.material.uniforms.uAction.value = ACTION_TO_INDEX[action]
  }

  /**
   * 更新湍流度
   */
  setTurbulence(value: number) {
    this.config.turbulence = value
    this.material.uniforms.uGlobalSpeed.value = value
  }

  /**
   * 心跳动画 - 创建粒子飘向生命体的效果
   */
  heartbeat() {
    if (this.heartbeatActive) return

    this.heartbeatActive = true
    this.heartbeatStartTime = Date.now()

    // 清理之前的心跳粒子（如果有）
    if (this.heartbeatPoints) {
      this.scene.remove(this.heartbeatPoints)
      this.heartbeatGeometry?.dispose()
      this.heartbeatMaterial?.dispose()
    }

    // 创建心跳粒子系统（使用与主粒子相同的着色器）
    this.heartbeatGeometry = new THREE.BufferGeometry()

    const posArr = new Float32Array(this.heartbeatParticleCount * 3)
    const colArr = new Float32Array(this.heartbeatParticleCount * 3)
    const sizArr = new Float32Array(this.heartbeatParticleCount)

    // 生成球体周围的粒子起始位置
    for (let i = 0; i < this.heartbeatParticleCount; i++) {
      const theta = (i / this.heartbeatParticleCount) * Math.PI * 2 + Math.random() * 0.3
      const phi = Math.acos(Math.random() * 2 - 1)
      const startRadius = 3.0 + Math.random() * 0.5

      posArr[i * 3] = Math.sin(phi) * Math.cos(theta) * startRadius
      posArr[i * 3 + 1] = Math.sin(phi) * Math.sin(theta) * startRadius
      posArr[i * 3 + 2] = Math.cos(phi) * startRadius

      // 绿色（正常状态颜色）
      const color = new THREE.Color(0x00ff5a)
      colArr[i * 3] = color.r
      colArr[i * 3 + 1] = color.g
      colArr[i * 3 + 2] = color.b

      sizArr[i] = Math.random() * 2 + 2  // 稍大一些
    }

    this.heartbeatGeometry.setAttribute('position', new THREE.BufferAttribute(posArr, 3))
    this.heartbeatGeometry.setAttribute('aColor', new THREE.BufferAttribute(colArr, 3))
    this.heartbeatGeometry.setAttribute('aSize', new THREE.BufferAttribute(sizArr, 1))

    // 存储起始位置用于动画计算
    const startPositions = new Float32Array(posArr)
    this.heartbeatGeometry.setAttribute('aStartPosition', new THREE.BufferAttribute(startPositions, 3))

    this.heartbeatMaterial = new THREE.ShaderMaterial({
      uniforms: {
        uTime: { value: 0 },
        uProgress: { value: 0 },
        uEmotion: { value: 0 },  // 正常状态
        uAction: { value: 0 },   // idle
        uGlobalSpeed: { value: 1 },
        uOpacity: { value: 1 }   // 透明度控制
      },
      vertexShader: VERTEX_SHADER,
      fragmentShader: HEARTBEAT_FRAGMENT_SHADER,
      transparent: true,
      blending: THREE.AdditiveBlending,
      depthTest: false
    })

    this.heartbeatPoints = new THREE.Points(this.heartbeatGeometry, this.heartbeatMaterial)
    this.scene.add(this.heartbeatPoints)

    // 4.5 秒后清理心跳粒子（稍微多一点余量）
    setTimeout(() => {
      this.cleanupHeartbeat()
    }, 4500)
  }

  /**
   * 清理心跳粒子
   */
  private cleanupHeartbeat() {
    if (this.heartbeatPoints) {
      this.scene.remove(this.heartbeatPoints)
      this.heartbeatPoints = undefined
    }
    this.heartbeatGeometry?.dispose()
    this.heartbeatGeometry = undefined
    this.heartbeatMaterial?.dispose()
    this.heartbeatMaterial = undefined
    this.heartbeatActive = false
  }

  /**
   * 渲染循环
   */
  render() {
    const animate = () => {
      this.animationId = requestAnimationFrame(animate)

      const currentTime = performance.now() / 1000
      const speedMultiplier = this.config.breathing ? this.config.breathingSpeed : 0
      this.time += 0.001 * speedMultiplier

      this.material.uniforms.uTime.value = currentTime
      this.points.rotation.y += 0.001

      // 更新心跳粒子动画
      if (this.heartbeatActive && this.heartbeatGeometry && this.heartbeatMaterial && this.heartbeatPoints) {
        const elapsed = (Date.now() - this.heartbeatStartTime) / 1000  // 秒
        const duration = 4.0  // 动画持续时间（秒）- 放慢速度
        const progress = Math.min(elapsed / duration, 1)

        // 使用缓动函数让动画更平滑
        const easedProgress = 1 - Math.pow(1 - progress, 2)  // easeOutQuad (更平缓)

        this.heartbeatMaterial.uniforms.uTime.value = currentTime
        this.heartbeatMaterial.uniforms.uProgress.value = easedProgress

        // 添加轻微旋转
        this.heartbeatPoints.rotation.y += 0.002

        // 手动更新粒子位置（从起始位置飘向球体中心）
        const positions = this.heartbeatGeometry.attributes.position.array as Float32Array
        const startPositions = this.heartbeatGeometry.attributes.aStartPosition?.array as Float32Array

        if (startPositions) {
          for (let i = 0; i < this.heartbeatParticleCount; i++) {
            const ix = i * 3
            const iy = i * 3 + 1
            const iz = i * 3 + 2

            // 从起始位置向球体中心（1.0 半径处）移动
            const startRadius = Math.sqrt(
              startPositions[ix] ** 2 +
              startPositions[iy] ** 2 +
              startPositions[iz] ** 2
            )

            const targetRadius = 1.0  // 移动到球体表面
            const currentRadius = startRadius + (targetRadius - startRadius) * easedProgress

            const ratio = currentRadius / startRadius
            positions[ix] = startPositions[ix] * ratio
            positions[iy] = startPositions[iy] * ratio
            positions[iz] = startPositions[iz] * ratio
          }

          this.heartbeatGeometry.attributes.position.needsUpdate = true
        }

        // 渐隐效果（最后 20% 的时间内快速消失）
        if (progress > 0.8) {
          const fadeProgress = (progress - 0.8) / 0.2  // 0 到 1
          this.heartbeatMaterial.uniforms.uOpacity.value = 1 - fadeProgress * fadeProgress  // 二次衰减更平滑
        }
      }

      this.renderer.render(this.scene, this.camera)
    }

    animate()
  }

  /**
   * 销毁
   */
  dispose() {
    if (this.animationId !== null) {
      cancelAnimationFrame(this.animationId)
    }

    window.removeEventListener('resize', () => this.resize())

    // 清理心跳粒子
    this.cleanupHeartbeat()

    this.geometry.dispose()
    this.material.dispose()
    this.renderer.dispose()

    if (this.renderer.domElement.parentNode === this.container) {
      this.container.removeChild(this.renderer.domElement)
    }
  }
}
