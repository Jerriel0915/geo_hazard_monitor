/**
 * Reflector - 平面镜面反射效果
 * 移植自 showroom/src/libs/Reflector.js
 * @cljs-earth/three → 原生 three
 * 注意: Three.js r152+ 移除了 sRGBEncoding，改用 SRGBColorSpace
 */
import {
  Color,
  Matrix4,
  Mesh,
  PerspectiveCamera,
  Plane,
  ShaderMaterial,
  UniformsUtils,
  Vector3,
  Vector4,
  WebGLRenderTarget,
  HalfFloatType,
  type BufferGeometry,
  type Camera,
  type Scene,
  type WebGLRenderer,
  SRGBColorSpace
} from 'three'

interface ReflectorOptions {
  color?: number
  textureWidth?: number
  textureHeight?: number
  clipBias?: number
  shader?: typeof Reflector.ReflectorShader
  multisample?: number
}

class Reflector extends Mesh {
  isReflector = true
  type = 'Reflector'
  camera: PerspectiveCamera

  private _reflectorPlane: Plane
  private _normal: Vector3
  private _reflectorWorldPosition: Vector3
  private _cameraWorldPosition: Vector3
  private _rotationMatrix: Matrix4
  private _lookAtPosition: Vector3
  private _clipPlane: Vector4
  private _view: Vector3
  private _target: Vector3
  private _q: Vector4
  private _textureMatrix: Matrix4
  private _virtualCamera: PerspectiveCamera
  private _renderTarget: WebGLRenderTarget

  constructor(geometry: BufferGeometry, options: ReflectorOptions = {}) {
    super(geometry)

    this.camera = new PerspectiveCamera()

    const scope = this

    const color = (options.color !== undefined) ? new Color(options.color) : new Color(0x7F7F7F)
    const textureWidth = options.textureWidth || 512
    const textureHeight = options.textureHeight || 512
    const clipBias = options.clipBias || 0
    const shader = options.shader || Reflector.ReflectorShader
    const multisample = (options.multisample !== undefined) ? options.multisample : 0

    this._reflectorPlane = new Plane()
    this._normal = new Vector3()
    this._reflectorWorldPosition = new Vector3()
    this._cameraWorldPosition = new Vector3()
    this._rotationMatrix = new Matrix4()
    this._lookAtPosition = new Vector3(0, 0, -1)
    this._clipPlane = new Vector4()
    this._view = new Vector3()
    this._target = new Vector3()
    this._q = new Vector4()
    this._textureMatrix = new Matrix4()
    this._virtualCamera = this.camera

    this._renderTarget = new WebGLRenderTarget(textureWidth, textureHeight, {
      samples: multisample,
      type: HalfFloatType
    })

    const material = new ShaderMaterial({
      name: (shader.name !== undefined) ? shader.name : 'unspecified',
      uniforms: UniformsUtils.clone(shader.uniforms),
      fragmentShader: shader.fragmentShader,
      vertexShader: shader.vertexShader
    })

    material.uniforms.tDiffuse.value = this._renderTarget.texture
    material.uniforms.color.value = color
    material.uniforms.textureMatrix.value = this._textureMatrix

    this.material = material

    this.onBeforeRender = function (renderer: WebGLRenderer, scene: Scene, camera: Camera) {
      this._reflectorWorldPosition.setFromMatrixPosition(scope.matrixWorld)
      this._cameraWorldPosition.setFromMatrixPosition(camera.matrixWorld)

      this._rotationMatrix.extractRotation(scope.matrixWorld)

      this._normal.set(0, 0, 1)
      this._normal.applyMatrix4(this._rotationMatrix)

      this._view.subVectors(this._reflectorWorldPosition, this._cameraWorldPosition)

      if (this._view.dot(this._normal) > 0) return

      this._view.reflect(this._normal).negate()
      this._view.add(this._reflectorWorldPosition)

      this._rotationMatrix.extractRotation(camera.matrixWorld)

      this._lookAtPosition.set(0, 0, -1)
      this._lookAtPosition.applyMatrix4(this._rotationMatrix)
      this._lookAtPosition.add(this._cameraWorldPosition)

      this._target.subVectors(this._reflectorWorldPosition, this._lookAtPosition)
      this._target.reflect(this._normal).negate()
      this._target.add(this._reflectorWorldPosition)

      this._virtualCamera.position.copy(this._view)
      this._virtualCamera.up.set(0, 1, 0)
      this._virtualCamera.up.applyMatrix4(this._rotationMatrix)
      this._virtualCamera.up.reflect(this._normal)
      this._virtualCamera.lookAt(this._target)

      this._virtualCamera.far = (camera as PerspectiveCamera).far
      this._virtualCamera.updateMatrixWorld()
      this._virtualCamera.projectionMatrix.copy(camera.projectionMatrix)

      this._textureMatrix.set(
        0.5, 0.0, 0.0, 0.5,
        0.0, 0.5, 0.0, 0.5,
        0.0, 0.0, 0.5, 0.5,
        0.0, 0.0, 0.0, 1.0
      )
      this._textureMatrix.multiply(this._virtualCamera.projectionMatrix)
      this._textureMatrix.multiply(this._virtualCamera.matrixWorldInverse)
      this._textureMatrix.multiply(scope.matrixWorld)

      this._reflectorPlane.setFromNormalAndCoplanarPoint(this._normal, this._reflectorWorldPosition)
      this._reflectorPlane.applyMatrix4(this._virtualCamera.matrixWorldInverse)

      this._clipPlane.set(this._reflectorPlane.normal.x, this._reflectorPlane.normal.y, this._reflectorPlane.normal.z, this._reflectorPlane.constant)

      const projectionMatrix = this._virtualCamera.projectionMatrix

      this._q.x = (Math.sign(this._clipPlane.x) + projectionMatrix.elements[8]) / projectionMatrix.elements[0]
      this._q.y = (Math.sign(this._clipPlane.y) + projectionMatrix.elements[9]) / projectionMatrix.elements[5]
      this._q.z = -1.0
      this._q.w = (1.0 + projectionMatrix.elements[10]) / projectionMatrix.elements[14]

      this._clipPlane.multiplyScalar(2.0 / this._clipPlane.dot(this._q))

      projectionMatrix.elements[2] = this._clipPlane.x
      projectionMatrix.elements[6] = this._clipPlane.y
      projectionMatrix.elements[10] = this._clipPlane.z + 1.0 - clipBias
      projectionMatrix.elements[14] = this._clipPlane.w

      scope.visible = false

      const currentRenderTarget = renderer.getRenderTarget()
      const currentXrEnabled = renderer.xr.enabled
      const currentShadowAutoUpdate = renderer.shadowMap.autoUpdate

      renderer.xr.enabled = false
      renderer.shadowMap.autoUpdate = false

      renderer.setRenderTarget(this._renderTarget)
      renderer.state.buffers.depth.setMask(true)

      if (renderer.autoClear === false) renderer.clear()
      renderer.render(scene, this._virtualCamera)

      renderer.xr.enabled = currentXrEnabled
      renderer.shadowMap.autoUpdate = currentShadowAutoUpdate
      renderer.setRenderTarget(currentRenderTarget)

      const viewport = camera.viewport
      if (viewport !== undefined) {
        renderer.state.viewport(viewport)
      }

      scope.visible = true
    }
  }

  getRenderTarget(): WebGLRenderTarget {
    return this._renderTarget
  }

  dispose() {
    this._renderTarget.dispose()
    ;(this.material as ShaderMaterial).dispose()
  }

  static ReflectorShader = {
    name: 'ReflectorShader',
    uniforms: {
      color: { value: null },
      tDiffuse: { value: null },
      textureMatrix: { value: null }
    },
    vertexShader: /* glsl */ `
      uniform mat4 textureMatrix;
      varying vec4 vUv;
      #include <common>
      #include <logdepthbuf_pars_vertex>
      void main() {
        vUv = textureMatrix * vec4( position, 1.0 );
        gl_Position = projectionMatrix * modelViewMatrix * vec4( position, 1.0 );
        #include <logdepthbuf_vertex>
      }
    `,
    fragmentShader: /* glsl */ `
      uniform vec3 color;
      uniform sampler2D tDiffuse;
      varying vec4 vUv;
      #include <logdepthbuf_pars_fragment>
      float blendOverlay( float base, float blend ) {
        return( base < 0.5 ? ( 1.0 * base * blend ) : ( 1.0 - 1.0 * ( 1.0 - base ) * ( 1.0 - blend ) ) );
      }
      vec3 blendOverlay( vec3 base, vec3 blend ) {
        return vec3( blendOverlay( base.r, blend.r ), blendOverlay( base.g, blend.g ), blendOverlay( base.b, blend.b ) );
      }
      void main() {
        #include <logdepthbuf_fragment>
        vec4 base = texture2DProj( tDiffuse, vUv );
        gl_FragColor = vec4( blendOverlay( base.rgb, color ), 0.3 );
        #include <tonemapping_fragment>
        #include <colorspace_fragment>
      }
    `
  }
}

export { Reflector }
