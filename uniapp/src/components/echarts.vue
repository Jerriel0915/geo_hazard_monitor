<template>
	<view class="echarts-wrap" :style="{ width: width, height: height }">
		<canvas class="ec-canvas" :id="canvasId" :canvasId="canvasId" @touchstart="touchStart" @touchmove="touchMove"
			@touchend="touchEnd" @error="error"></canvas>
	</view>
</template>
<script>
	import WxCanvas from '@/components/wx-canvas.js';
	import * as echarts from '@/components/echarts.esm.min.js';
	export default {
		props: {
			onInit: {
				required: true,
				type: Function,
				default: null
			},
			canvasId: {
				type: String,
				default: 'ec-canvas'
			},
			width: {
				type: String,
				default: '100%'
			},
			height: {
				type: String,
				default: '400rpx'
			},
			lazyLoad: {
				type: Boolean,
				default: false
			},
			disableTouch: {
				type: Boolean,
				default: false
			},
			throttleTouch: {
				type: Boolean,
				default: false
			}
		},
		mounted() {
			this.echarts = echarts;
			if (!this.echarts) {
				console.warn('组件需绑定 echarts 变量，例：<ec-canvas id="mychart-dom-bar" ' +
					'canvas-id="mychart-bar" :echarts="echarts"></ec-canvas>');
				return;
			}
			if (!this.lazyLoad) this.init();
		},
		methods: {
			init() {
				const canvasId = this.canvasId;
				this.ctx = uni.createCanvasContext(canvasId, this);
				const canvas = new WxCanvas(this.ctx, canvasId);
				this.echarts.setCanvasCreator(() => canvas);
				const query = uni.createSelectorQuery().in(this);
				query.select('.echarts-wrap').boundingClientRect(res => {
					if (!res) {
						setTimeout(() => this.init(), 200);
						return;
					}
					console.log('[echarts.vue] container size:', res.width, res.height);
					// 转换为像素单位 (1rpx = 0.5px 在大部分设备上)
					const dpr = uni.getSystemInfoSync().pixelRatio || 1;
					const width = res.width;
					const height = res.height;
					this.chart = this.onInit(canvas, width, height);
				}).exec();
			},
			canvasToTempFilePath(opt) {
				const { canvasId } = this;
				this.ctx.draw(true, () => {
					uni.canvasToTempFilePath({
						canvasId,
						...opt
					});
				});
			},
			touchStart(e) {
				const { disableTouch, chart } = this;
				if (disableTouch || !chart || !e.mp || !e.mp.touches || !e.mp.touches.length) return;
				const touch = e.mp.touches[0];
				// 只派发 mousedown，不额外派发 mousemove
				// 避免干扰 dataZoom 拖拽行为；tooltip 由 touchMove 触发
				chart._zr.handler.dispatch('mousedown', {
					zrX: touch.x,
					zrY: touch.y
				});
			},
			touchMove(e) {
				const { disableTouch, throttleTouch, chart, lastMoveTime } = this;
				if (disableTouch || !chart || !e.mp || !e.mp.touches || !e.mp.touches.length) return;
				if (throttleTouch) {
					const currMoveTime = Date.now();
					if (currMoveTime - lastMoveTime < 50) return;
					this.lastMoveTime = currMoveTime;
				}
				const touch = e.mp.touches[0];
				chart._zr.handler.dispatch('mousemove', {
					zrX: touch.x,
					zrY: touch.y
				});
			},
			touchEnd(e) {
				const { disableTouch, chart } = this;
				if (disableTouch || !chart) return;
				const touch = (e.mp && e.mp.changedTouches && e.mp.changedTouches[0]) || {};
				chart._zr.handler.dispatch('mouseup', {
					zrX: touch.x || 0,
					zrY: touch.y || 0
				});
				chart._zr.handler.dispatch('click', {
					zrX: touch.x || 0,
					zrY: touch.y || 0
				});
			}
		}
	};
</script>
<style scoped>
	.echarts-wrap {
		display: inline-block;
		width: 100%;
		height: 400rpx;
		position: relative;
	}

	.ec-canvas {
		width: 100%;
		height: 100%;
		display: block;
	}
</style>