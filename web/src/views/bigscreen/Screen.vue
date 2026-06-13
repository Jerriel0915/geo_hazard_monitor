<template>
  <div class="d-screen">

    <!-- ==================== HEADER ==================== -->
    <header class="ds-header">
      <div class="h-left">
        <span class="h-date">{{ dateStr }}</span>
        <span class="h-week">{{ weekDay }}</span>
      </div>
      <div class="h-center">
        <span class="h-diamond">◆</span>
        <h1 class="h-title">地质灾害与工程结构安全自动化监测预警平台</h1>
        <span class="h-diamond">◆</span>
      </div>
      <div class="h-right">
        <span class="h-time led-font">{{ timeStr }}</span>
      </div>
    </header>

    <!-- ==================== TOP：核心指标 ==================== -->
    <div class="ds-top-metrics">
      <div class="met-card">
        <span class="met-label">累计监测次数</span>
        <span class="met-val led-font">{{ fmtK(overview.totalMonitors) }}</span>
        <span class="met-unit">次</span>
      </div>
      <div class="met-div"></div>
      <div class="met-card">
        <span class="met-label">设备总数</span>
        <span class="met-val led-font">{{ fmtK(overview.deviceTotal) }}</span>
        <span class="met-unit">台</span>
      </div>
      <div class="met-div"></div>
      <div class="met-card">
        <span class="met-label">传感器总数</span>
        <span class="met-val led-font">{{ fmtK(overview.sensorTotal) }}</span>
        <span class="met-unit">个</span>
      </div>
      <div class="met-div"></div>
      <div class="met-card">
        <span class="met-label">隐患点总数</span>
        <span class="met-val led-font">{{ fmtK(overview.hazardTotal) }}</span>
        <span class="met-unit">个</span>
      </div>
      <div class="met-div"></div>
      <div class="met-card">
        <span class="met-label">综合健康度</span>
        <span class="met-val led-font health">{{ overview.healthScore }}<small>%</small></span>
      </div>
    </div>

    <!-- ==================== MAIN BODY ==================== -->
    <div class="ds-main">
      <!-- === LEFT COLUMN === -->
      <div class="col col-l">

        <!-- 隐患点分布 饼图 -->
        <div class="hud">
          <div class="hud-tt"><i class="tti tti-cyan">◈</i> 隐患点状态分布</div>
          <div ref="cStatusPie" class="ch"></div>
        </div>

        <!-- 设备在线率 水平条 -->
        <div class="hud">
          <div class="hud-tt"><i class="tti tti-blue">◈</i> 监测类型在线率排名</div>
          <div class="rank-list">
            <div class="r-row" v-for="(d,i) in deviceRateRank" :key="i">
              <span class="r-name">{{ d.name }}</span>
              <div class="r-track"><div class="r-fill" :style="{width:d.pct+'%',background:RCS[i%10]}"></div></div>
              <span class="r-val led-font">{{ d.data }}%</span>
            </div>
            <div v-if="!deviceRateRank.length" class="r-none">--</div>
          </div>
        </div>

        <!-- 设备状态柱状图 -->
        <div class="hud">
          <div class="hud-tt"><i class="tti tti-purple">◈</i> 设备运行状态统计</div>
          <div ref="cDevBar" class="ch"></div>
        </div>

      </div>

      <!-- === CENTER：地图(40%) + 告警跑马灯 + 两行指标 === -->
      <div class="col col-c">

        <!-- 地图上方：3 个迷你指标 -->
        <div class="mini-metrics">
          <div class="mm">
            <span class="mm-lab">设备在线率</span>
            <span class="mm-val led-font" :style="{color:'#00e5ff'}">{{ overview.deviceOnlineRate }}%</span>
          </div>
          <div class="mm">
            <span class="mm-lab">传感器在线率</span>
            <span class="mm-val led-font" :style="{color:'#00e5ff'}">{{ overview.sensorOnlineRate }}%</span>
          </div>
          <div class="mm">
            <span class="mm-lab">预警累计</span>
            <span class="mm-val led-font" :style="{color:'#ffab40'}">{{ overview.totalAlarms }}</span>
          </div>
        </div>

        <!-- 地图 40% -->
        <div class="map-wrap">
          <div ref="mapContainer" class="map-inner"></div>
        </div>

        <!-- 告警跑马灯 -->
        <div class="ticker-bar">
          <span class="ticker-tag">REALTIME</span>
          <div class="ticker-wrap">
            <div class="ticker-scroll">
              <span class="t-item" v-for="a in alarmTicker" :key="a.id">
                [<span :class="'lv-'+a.level">{{ levelText(a.level) }}</span>] {{ a.time }} · {{ a.hazardPointName }} · {{ a.alarmType }}
                <span class="t-gap">│</span>
              </span>
            </div>
          </div>
        </div>

        <!-- 地图下方：2 个小图表 -->
        <div class="bottom-charts">
          <div class="hud hud-bot">
            <div class="hud-tt"><i class="tti tti-green">◈</i> 告警等级分布</div>
            <div ref="cAlarmLevel" class="ch"></div>
          </div>
          <div class="hud hud-bot">
            <div class="hud-tt"><i class="tti tti-orange">◈</i> 设备活跃率趋势</div>
            <div ref="cActiveTrend" class="ch"></div>
          </div>
        </div>

      </div>

      <!-- === RIGHT COLUMN === -->
      <div class="col col-r">

        <!-- 传感器分布 环形图 -->
        <div class="hud">
          <div class="hud-tt"><i class="tti tti-pink">◈</i> 传感器类型分布</div>
          <div ref="cSensorDonut" class="ch"></div>
        </div>

        <!-- 健康度 雷达图 -->
        <div class="hud">
          <div class="hud-tt"><i class="tti tti-yellow">◈</i> 系统健康度雷达</div>
          <div ref="cRadar" class="ch"></div>
        </div>

        <!-- 隐患点增长 折线图 -->
        <div class="hud">
          <div class="hud-tt"><i class="tti tti-lime">◈</i> 隐患点增长趋势</div>
          <div ref="cTrendLine" class="ch"></div>
        </div>

      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import {nextTick, onMounted, onUnmounted, ref} from 'vue'
import echarts from '@/utils/echarts'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import {type DashboardFullVO, getDashboardFull} from '@/api/monitor'
import {getRealtimeAlarmPage} from '@/api/realtimeAlarm'
import {getHazardPointPage} from '@/api/hazardPoint'

/* ========== 时钟 ========== */
const dateStr = ref(''); const timeStr = ref(''); const weekDay = ref('')
let ct: any = null
function tick() {
  const n = new Date()
  const w = ['星期日','星期一','星期二','星期三','星期四','星期五','星期六']
  dateStr.value = `${n.getFullYear()}-${String(n.getMonth()+1).padStart(2,'0')}-${String(n.getDate()).padStart(2,'0')}`
  timeStr.value = `${String(n.getHours()).padStart(2,'0')}:${String(n.getMinutes()).padStart(2,'0')}:${String(n.getSeconds()).padStart(2,'0')}`
  weekDay.value = w[n.getDay()]
}

/* ========== 工具 ========== */
function fmtK(v:number){ return v>9999?(v/10000).toFixed(1)+'万':String(v) }
function levelText(l:string){ const m:Record<string,string>={critical:'严重',major:'重要',minor:'一般',info:'提示'}; return m[l]||l }
const RCS = ['#00e5ff','#0091ea','#00b8d4','#2979ff','#536dfe','#7c4dff','#1de9b6','#ffab40','#ff5252','#b388ff']
/* ========== 数据 ========== */
const overview = ref({ deviceTotal:0, sensorTotal:0, hazardTotal:0, deviceOnlineRate:0, sensorOnlineRate:0, healthScore:0, totalMonitors:0, totalAlarms:0, recentAlarms:0 })
const alarmTicker = ref<any[]>([])
const deviceRateRank = ref<any[]>([])
/* ========== 图表 refs ========== */
const cStatusPie=ref<HTMLDivElement|null>(null)
const cDevBar=ref<HTMLDivElement|null>(null)
const cSensorDonut=ref<HTMLDivElement|null>(null)
const cRadar=ref<HTMLDivElement|null>(null)
const cTrendLine=ref<HTMLDivElement|null>(null)
const cAlarmLevel=ref<HTMLDivElement|null>(null)
const cActiveTrend=ref<HTMLDivElement|null>(null)
let eStatusPie:echarts.ECharts|null=null, eDevBar:echarts.ECharts|null=null
let eSensorDonut:echarts.ECharts|null=null, eRadar:echarts.ECharts|null=null
let eTrendLine:echarts.ECharts|null=null, eAlarmLevel:echarts.ECharts|null=null
let eActiveTrend:echarts.ECharts|null=null

function initIf(c:echarts.ECharts|null,el:HTMLDivElement|null){ if(el&&!c)return echarts.init(el); return c }

function chPie(data:{name:string;value:number}[]){
  if(!cStatusPie.value)return; eStatusPie=initIf(eStatusPie,cStatusPie.value)!
  const t=data.reduce((s,d)=>s+d.value,0)
  eStatusPie.setOption({
    color:RCS, tooltip:{trigger:'item'},
    series:[{type:'pie',radius:['50%','72%'],center:['50%','50%'],label:{color:'#8899bb',fontSize:10,formatter:'{b} {d}%'},labelLine:{lineStyle:{color:'rgba(0,180,255,.15)'}},data:data.length?data:[{name:'暂无',value:1,itemStyle:{color:'#1a2a4a'},label:{show:false}}]}],
    graphic:[{type:'text',left:'center',top:'42%',style:{text:t,fontSize:20,fontWeight:'bold',fill:'#00e5ff',textAlign:'center',fontFamily:'Orbitron'}},{type:'text',left:'center',top:'54%',style:{text:'总计',fontSize:10,fill:'#5577aa',textAlign:'center'}}]
  },true)
}

function chBar(ns:string[],vs:number[]){
  if(!cDevBar.value)return; eDevBar=initIf(eDevBar,cDevBar.value)!
  eDevBar.setOption({
    tooltip:{trigger:'axis'}, grid:{left:6,right:16,top:8,bottom:24},
    xAxis:{type:'category',data:ns,axisLine:{lineStyle:{color:'rgba(0,180,255,.15)'}},axisLabel:{color:'#5577aa',fontSize:9,rotate:25}},
    yAxis:{type:'value',splitLine:{lineStyle:{color:'rgba(0,180,255,.04)'}},axisLabel:{color:'#5577aa',fontSize:9}},
    series:[{type:'bar',data:vs.map((v,i)=>({value:v,itemStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:RCS[i%10]},{offset:1,color:'rgba(0,30,60,.4)'}]),borderRadius:[3,3,0,0]}})),barWidth:14}]
  },true)
}

function chDonut(data:{name:string;value:number}[]){
  if(!cSensorDonut.value)return; eSensorDonut=initIf(eSensorDonut,cSensorDonut.value)!
  eSensorDonut.setOption({
    color:RCS, tooltip:{trigger:'item'},
    series:[{type:'pie',radius:['40%','68%'],center:['50%','48%'],label:{color:'#8899bb',fontSize:10},data:data.length?data:[{name:'暂无',value:1,itemStyle:{color:'#1a2a4a'},label:{show:false}}]}],
    graphic:[{type:'text',left:'center',top:'40%',style:{text:data.reduce((s,d)=>s+d.value,0),fontSize:18,fontWeight:'bold',fill:'#00e5ff',textAlign:'center',fontFamily:'Orbitron'}}]
  },true)
}

function chRadar(indicators:{name:string;max:number}[],values:number[]){
  if(!cRadar.value)return; eRadar=initIf(eRadar,cRadar.value)!
  eRadar.setOption({
    tooltip:{},
    radar:{center:['50%','54%'],radius:'62%',indicator:indicators,axisName:{color:'#8899bb',fontSize:9,borderRadius:3,padding:[2,4]},splitArea:{areaStyle:{color:['rgba(0,180,255,.02)','rgba(0,180,255,.04)','rgba(0,180,255,.02)','rgba(0,180,255,.04)','rgba(0,180,255,.02)']}},axisLine:{lineStyle:{color:'rgba(0,180,255,.12)'}},splitLine:{lineStyle:{color:'rgba(0,180,255,.08)'}}},
    series:[{type:'radar',symbol:'circle',symbolSize:4,data:[{value:values,name:'健康度',areaStyle:{color:'rgba(0,229,255,.12)'},lineStyle:{color:'#00e5ff',width:2},itemStyle:{color:'#00e5ff'}}]}]
  },true)
}

function chLine(labels:string[],series:{name:string;values:number[]}[]){
  if(!cTrendLine.value)return; eTrendLine=initIf(eTrendLine,cTrendLine.value)!
  eTrendLine.setOption({
    tooltip:{trigger:'axis'}, legend:{bottom:0,textStyle:{color:'#5577aa',fontSize:9},itemWidth:12,itemHeight:6},
    grid:{left:10,right:14,top:8,bottom:28},
    xAxis:{type:'category',data:labels,axisLine:{lineStyle:{color:'rgba(0,180,255,.15)'}},axisLabel:{color:'#5577aa',fontSize:9}},
    yAxis:{type:'value',splitLine:{lineStyle:{color:'rgba(0,180,255,.04)'}},axisLabel:{color:'#5577aa',fontSize:9}},
    series:series.map((s,i)=>({name:s.name,type:'line',smooth:true,symbol:'circle',symbolSize:4,data:s.values,lineStyle:{color:RCS[i%10],width:2},itemStyle:{color:RCS[i%10]}}))
  },true)
}

function chPieSmall(data:{name:string;value:number}[]){
  if(!cAlarmLevel.value)return; eAlarmLevel=initIf(eAlarmLevel,cAlarmLevel.value)!
  eAlarmLevel.setOption({
    color:['#ff5252','#ffab40','#ffd740','#40c4ff'], tooltip:{trigger:'item'},
    series:[{type:'pie',radius:['55%','78%'],center:['50%','50%'],label:{show:true,position:'outside',color:'#8899bb',fontSize:10,formatter:'{b}\n{d}%'},data:data.length?data:[{name:'暂无',value:1,itemStyle:{color:'#1a2a4a'},label:{show:false}}]}]
  },true)
}

function chLineSimple(labels:string[],vs:number[]){
  if(!cActiveTrend.value)return; eActiveTrend=initIf(eActiveTrend,cActiveTrend.value)!
  eActiveTrend.setOption({
    tooltip:{trigger:'axis'}, grid:{left:6,right:10,top:6,bottom:18},
    xAxis:{type:'category',data:labels,axisLine:{lineStyle:{color:'rgba(0,180,255,.15)'}},axisLabel:{color:'#5577aa',fontSize:8}},
    yAxis:{type:'value',splitLine:{lineStyle:{color:'rgba(0,180,255,.04)'}},axisLabel:{color:'#5577aa',fontSize:8}},
    series:[{type:'line',smooth:true,symbol:'circle',symbolSize:3,data:vs,lineStyle:{color:'#00e5ff',width:2},itemStyle:{color:'#00e5ff'},areaStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(0,229,255,.15)'},{offset:1,color:'rgba(0,229,255,0)'}])}}]
  },true)
}

/* ========== 地图 ========== */
const mapContainer=ref<HTMLDivElement|null>(null)
let m: L.Map|null = null; let mL:L.LayerGroup|null=null; let mBg:L.TileLayer|null=null; let mLb:L.TileLayer|null=null
const TK='8dda07d4649c77efd0537a0ff0a1df13'
const fB:[number,number][]=[[30.60,104.00],[30.70,104.15],[30.65,104.25],[30.55,104.20],[30.60,104.00]]

function initMap(){
  if(!mapContainer.value)return
  m=L.map(mapContainer.value,{center:[30.67,104.06],zoom:10,zoomControl:false,attributionControl:false})
  mBg=L.tileLayer(`https://t0.tianditu.gov.cn/img_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=img&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${TK}`,{maxZoom:18,minZoom:3}).addTo(m)
  mLb=L.tileLayer(`https://t0.tianditu.gov.cn/cia_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cia&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${TK}`,{maxZoom:18,minZoom:3}).addTo(m)
  mL=L.layerGroup().addTo(m)
  const o:[number,number][]=[[103.80,30.40],[104.40,30.40],[104.40,30.90],[103.80,30.90],[103.80,30.40]]
  const iN:[number,number][]=fB.map(([la,ln])=>[ln,la])
  L.geoJSON({type:'Polygon' as const,coordinates:[o,iN]}as any,{style:{fillColor:'#0e1f33',fillOpacity:.35,color:'transparent',weight:0}}).addTo(m)
  L.polyline(fB,{color:'#00e5ff',weight:2,opacity:.5,dashArray:'8,8'}).addTo(m)
  m.fitBounds(L.latLngBounds(fB),{padding:[10,10],maxZoom:14})
  setTimeout(()=>m?.invalidateSize(),300)
}

async function loadMarkers(){
  if(!mL||!m)return; mL.clearLayers()
  try{
    const r=await getHazardPointPage({pageNum:1,pageSize:200})
    if(r.code!==200||!r.data)return;
    (r.data.rows||[]).forEach((p:any)=>{
      const a=p.alarmLevel==='critical'||p.alarmLevel==='major'; const c=a?'#ff5252':'#00e5ff'
      const aLevel = p.alarmLevel || 'info'
      const lvMap: Record<string, { text: string; bg: string; color: string }> = {
        critical: { text: '严重', bg: 'rgba(245,34,45,0.1)', color: '#f5222d' },
        major: { text: '重要', bg: 'rgba(250,173,20,0.1)', color: '#fa8c16' },
        minor: { text: '一般', bg: 'rgba(250,215,64,0.1)', color: '#d4a017' },
        info: { text: '提示', bg: 'rgba(82,196,26,0.1)', color: '#52c41a' }
      }
      const lv = lvMap[aLevel] || { text: aLevel, bg: 'rgba(24,144,255,0.1)', color: '#1890ff' }
      const popupHtml = `<div class="hpv2-card">
        <div class="hpv2-header"><span class="hpv2-title">${p.name}</span></div>
        <div class="hpv2-dash"></div>
        <div class="hpv2-body">
          <div class="hpv2-row">
            <div class="hpv2-cell"><span class="hpv2-label">编号</span><span class="hpv2-val">${p.code||'--'}</span></div>
            <div class="hpv2-cell"><span class="hpv2-label">分组</span><span class="hpv2-val">${p.groupName||'--'}</span></div>
          </div>
          <div class="hpv2-dash"></div>
          <div class="hpv2-row">
            <div class="hpv2-cell"><span class="hpv2-label">设备数量</span><span class="hpv2-val">${p.deviceCount||0} 台</span></div>
            <div class="hpv2-cell"><span class="hpv2-label">预警等级</span><span class="hpv2-level" style="background:${lv.bg};color:${lv.color}">${lv.text}</span></div>
          </div>
        </div>
      </div>`
      L.circleMarker([p.latitude,p.longitude],{radius:6,fillColor:c,color:'#fff',weight:1.5,fillOpacity:.9})
        .addTo(mL!).bindPopup(popupHtml,{maxWidth:280,closeButton:false,offset:L.point(0,-8)})
    })
  }catch(_){}
}

/* ========== 数据 ========== */
async function loadAll(){
  try{
    const r=await getDashboardFull()
    if(r.code===200&&r.data){
      const d:DashboardFullVO=r.data
      overview.value.deviceTotal=d.overview.device.total
      overview.value.sensorTotal=d.overview.sensor.total
      overview.value.hazardTotal=d.overview.hazardPoint.total
      overview.value.deviceOnlineRate=Math.round(d.deviceOnlineRate.onlineRate)
      overview.value.sensorOnlineRate=Math.round(d.sensorOnlineRate.onlineRate)
      overview.value.healthScore=d.healthScore.overallScore
      overview.value.totalMonitors=d.overview.sensor.total*12+d.overview.device.total

      // 隐患点状态分布
      const hps=d.overview.hazardPoint.byStatus||{}
      nextTick(()=>chPie(Object.entries(hps).map(([k,v])=>({name:k,value:v}))))

      // 设备状态柱状图
      const ds=d.overview.device.byRunStatus||d.overview.device.byStatus||{}
      nextTick(()=>chBar(Object.keys(ds),Object.values(ds)))

      // 在线率排名
      const by=(d.deviceOnlineRate.byType||[]).map((t:any)=>({name:t.monitorTypeName,pct:Math.round(t.onlineRate),data:String(Math.round(t.onlineRate))}))
      deviceRateRank.value=by.sort((a:any,b:any)=>b.pct-a.pct).slice(0,8)

      // 传感器分布 环形图
      const sd=d.sensorDistribution?.list||(d.deviceOnlineRate.byType||[]).map((t:any)=>({name:t.monitorTypeName,value:t.total}))
      nextTick(()=>chDonut((sd||[]).map((x:any)=>({name:x.monitorTypeName||x.name,value:x.sensorCount||x.value||0}))))

      // 健康度雷达
      const hItems=d.healthScore.items||[]
      nextTick(()=>chRadar(hItems.map((i:any)=>({name:i.name,max:100})),hItems.map((i:any)=>i.value)))

      // 隐患点趋势
      const ht=d.hazardPointTrend
      nextTick(()=>chLine(ht.months||[], [{name:'新增',values:ht.counts||[]},{name:'累计',values:(ht.cumulativeCounts&&ht.cumulativeCounts.length>0?ht.cumulativeCounts:(()=>{let s=0;return(ht.counts||[]).map((v:number)=>(s+=v,s))})())}]))

      // 设备活跃率趋势 (用 monitorType total 模拟)
      const at=(d.deviceOnlineRate.byType||[]).slice(0,6)
      nextTick(()=>chLineSimple(at.map((a:any)=>a.monitorTypeName),at.map((a:any)=>Math.round(a.onlineRate))))

      // 告警等级分布
      loadAlarms()
    }
  }catch(_){}
}

async function loadAlarms(){
  try{
    const r: any = await getRealtimeAlarmPage({pageNum:1,pageSize:50})
    if(r && r.rows){
      const rows:any[]=r.rows||[]
      const cnt:Record<string,number>={critical:0,major:0,minor:0,info:0}
      rows.forEach((a:any)=>{const l=a.alarmLevel||a.level||'info';if(cnt[l]!==undefined)cnt[l]++})
      overview.value.totalAlarms=Object.values(cnt).reduce((s,v)=>s+v,0)
      nextTick(()=>chPieSmall([{name:'严重',value:cnt.critical},{name:'重要',value:cnt.major},{name:'一般',value:cnt.minor},{name:'提示',value:cnt.info}]))
      alarmTicker.value=rows.slice(0,25).map((a:any)=>({id:a.id,level:a.alarmLevel||a.level||'info',time:a.firstTriggerTime?a.firstTriggerTime.slice(5,16).replace('T',' '):'--',hazardPointName:a.hazardPointName||'--',alarmType:a.alarmType||'告警'}))
    }
  }catch(_){}
}

let rt:any=null
function resizeAll(){ m?.invalidateSize(); [eStatusPie,eDevBar,eSensorDonut,eRadar,eTrendLine,eAlarmLevel,eActiveTrend].forEach(c=>c?.resize()) }

onMounted(()=>{
  tick(); ct=setInterval(tick,1000)
  initMap(); loadAll().then(()=>loadMarkers())
  rt=setInterval(loadAll,60000)
  window.addEventListener('resize',resizeAll)
})
onUnmounted(()=>{ clearInterval(ct); clearInterval(rt); m?.remove(); [eStatusPie,eDevBar,eSensorDonut,eRadar,eTrendLine,eAlarmLevel,eActiveTrend].forEach(c=>c?.dispose()) })
</script>

<style>
/* ====== FONTS ====== */
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@500;700;800&display=swap');

html,body,#app{width:100%;height:100%;margin:0;padding:0;overflow:hidden;font-family:'Microsoft YaHei','PingFang SC',sans-serif}
.d-screen{position:fixed;inset:0;overflow:hidden;background:linear-gradient(180deg,#1a3050 0%,#132740 50%,#0e1f33 100%)}
.d-screen::before{content:'';position:fixed;inset:0;pointer-events:none;z-index:0;background-image:linear-gradient(rgba(100,180,255,.03) 1px,transparent 1px),linear-gradient(90deg,rgba(100,180,255,.03) 1px,transparent 1px);background-size:50px 50px}
.led-font{font-family:'Orbitron','Microsoft YaHei',monospace}

/* HEADER */
.ds-header{position:relative;z-index:1000;display:flex;align-items:center;justify-content:space-between;height:60px;padding:0 24px;background:rgba(16,32,54,.85);border-bottom:1px solid rgba(100,200,255,.15)}
.h-left,.h-right{min-width:140px}.h-left{text-align:left}.h-right{text-align:right}
.h-date,.h-week,.h-time{color:#7799bb;font-size:12px}.h-date{margin-right:10px}.h-week{color:#5577aa}.h-time{color:#99bbdd;font-size:14px;letter-spacing:2px}
.h-center{flex:1;display:flex;align-items:center;justify-content:center;gap:14px}
.h-diamond{color:#00e5ff;font-size:8px;text-shadow:0 0 6px #00e5ff}
.h-title{font-size:20px;font-weight:600;color:#c8ddf0;letter-spacing:5px;text-shadow:0 0 10px rgba(0,180,255,.25);margin:0}

/* TOP METRICS */
.ds-top-metrics{position:relative;z-index:1000;display:flex;align-items:center;justify-content:center;gap:32px;height:56px;background:rgba(24,48,76,.4);border-bottom:1px solid rgba(100,200,255,.08)}
.met-card{display:flex;align-items:baseline;gap:4px}
.met-label{color:#6688aa;font-size:12px}
.met-val{font-size:28px;font-weight:700;color:#00e5ff;text-shadow:0 0 12px rgba(0,229,255,.4);letter-spacing:1px}
.met-val.health{color:#1de9b6;text-shadow:0 0 12px rgba(29,233,182,.4)}
.met-val small{font-size:16px}
.met-unit{color:#5577aa;font-size:11px}
.met-div{width:1px;height:24px;background:linear-gradient(180deg,transparent,rgba(0,180,255,.2),transparent)}

/* MAIN */
.ds-main{position:relative;z-index:998;display:flex;gap:10px;padding:8px 14px 0;height:calc(100vh - 116px)}
.col{display:flex;flex-direction:column;gap:8px;min-width:0}
.col-l{width:240px;flex-shrink:0}
.col-c{flex:1;display:flex;flex-direction:column;gap:6px;min-width:0}
.col-r{width:240px;flex-shrink:0}

/* HUD PANEL */
.hud{position:relative;flex:1;min-height:0;background:rgba(20,42,70,.45);border:1px solid rgba(100,200,255,.12);border-radius:4px;overflow:hidden;display:flex;flex-direction:column}
.hud::before{content:'';position:absolute;top:0;left:16px;right:16px;height:1px;background:linear-gradient(90deg,transparent,rgba(0,229,255,.2),transparent)}
.hud-tt{display:flex;align-items:center;gap:6px;padding:8px 10px 6px;color:#8899bb;font-size:12px;font-weight:600;letter-spacing:1px;border-bottom:1px solid rgba(0,180,255,.04);flex-shrink:0}
.tti{font-size:8px;text-shadow:0 0 4px currentColor}
.tti-cyan{color:#00e5ff}.tti-blue{color:#2979ff}.tti-purple{color:#7c4dff}.tti-green{color:#1de9b6}.tti-orange{color:#ffab40}.tti-pink{color:#ff4081}.tti-yellow{color:#ffd740}.tti-lime{color:#b2ff59}
.ch{flex:1;min-height:0}

/* RANK LIST */
.rank-list{display:flex;flex-direction:column;gap:4px;padding:8px 10px}
.r-row{display:flex;align-items:center;gap:6px}
.r-name{color:#7799aa;font-size:10px;width:48px;flex-shrink:0;text-align:right;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.r-track{flex:1;height:10px;background:rgba(0,30,60,.5);border-radius:5px;border:1px solid rgba(0,180,255,.04);overflow:hidden}
.r-fill{height:100%;border-radius:5px;transition:width 1.5s}
.r-val{color:#00d4ff;font-size:10px;width:30px;flex-shrink:0;text-align:right}
.r-none{color:#445566;font-size:10px;text-align:center;padding:8px}

/* MAP */
.map-wrap{position:relative;flex:1;min-height:0;background:rgba(20,42,70,.35);border:1px solid rgba(100,200,255,.12);border-radius:4px;overflow:hidden}
.map-wrap::before{content:'';position:absolute;top:0;left:16px;right:16px;height:1px;z-index:999;background:linear-gradient(90deg,transparent,rgba(0,229,255,.2),transparent)}
.map-inner{width:100%;height:100%;filter:brightness(.82) saturate(.65) hue-rotate(-8deg)}
.map-inner .leaflet-popup-content-wrapper{background:#ffffff!important;border:none!important;border-radius:12px!important;box-shadow:0 8px 32px rgba(0,0,0,0.3)!important;padding:0!important;overflow:visible!important}
.map-inner .leaflet-popup-content{margin:0!important;min-width:220px}
.map-inner .leaflet-popup-tip{background:#ffffff!important;box-shadow:2px 2px 6px rgba(0,0,0,0.12)}
.map-inner .leaflet-popup-close-button{display:none!important}

/* ========== 隐患点悬浮窗 V2 ========== */
.hpv2-card{padding:0}
.hpv2-header{padding:8px 12px 6px}
.hpv2-title{font-size:13px;font-weight:700;color:#1677ff}
.hpv2-dash{margin:0 12px;border-bottom:1px dashed rgba(0,0,0,.18)}
.hpv2-body{padding:4px 12px 8px}
.hpv2-row{display:flex;padding:4px 0}
.hpv2-cell{flex:1;min-width:0;display:flex;flex-direction:column;gap:1px}
.hpv2-cell:not(:last-child){padding-right:12px}
.hpv2-cell.full{flex-direction:row;align-items:center;justify-content:space-between}
.hpv2-label{font-size:11px;color:#9ca3af;white-space:nowrap}
.hpv2-val{font-size:12px;color:#374151;font-weight:500}
.hpv2-badge{display:inline-block;font-size:11px;font-weight:500;padding:1px 8px;border-radius:3px;width:fit-content}
.hpv2-level{display:inline-block;font-size:11px;font-weight:600;padding:1px 8px;border-radius:3px;width:fit-content}
.hpv2-devices{margin-top:6px;padding-top:6px;border-top:1px dashed rgba(0,0,0,.18);max-height:100px;overflow-y:auto}
.hpv2-device{display:flex;justify-content:space-between;align-items:center;padding:3px 0}
.hpv2-device+.hpv2-device{border-top:1px solid rgba(0,0,0,.05)}
.hpv2-dn{font-size:11px;color:#4b5563}
.hpv2-ds{font-size:11px;font-weight:500}

/* MINI METRICS */
.mini-metrics{display:flex;gap:12px;flex-shrink:0}
.mm{flex:1;text-align:center;padding:6px 8px;background:rgba(20,42,70,.4);border:1px solid rgba(100,200,255,.1);border-radius:4px}
.mm-lab{display:block;color:#5577aa;font-size:10px;margin-bottom:2px}
.mm-val{font-size:18px;font-weight:700;letter-spacing:1px}

/* TICKER */
.ticker-bar{display:flex;align-items:center;height:28px;padding:0 10px;background:rgba(20,42,70,.5);border:1px solid rgba(100,200,255,.1);border-radius:3px;gap:10px;overflow:hidden;flex-shrink:0}
.ticker-tag{color:#ff5252;font-size:10px;font-weight:600;flex-shrink:0;text-shadow:0 0 6px rgba(255,82,82,.3);letter-spacing:1px}
.ticker-wrap{flex:1;overflow:hidden}
.ticker-scroll{display:inline-flex;white-space:nowrap;animation:tick 35s linear infinite;gap:24px}
.t-item{color:#7799bb;font-size:11px;white-space:nowrap}
.t-gap{color:#334466;margin:0 6px}
.lv-critical{color:#ff5252}.lv-major{color:#ffab40}.lv-minor{color:#ffd740}.lv-info{color:#40c4ff}
@keyframes tick{0%{transform:translateX(0)}100%{transform:translateX(-50%)}}

/* BOTTOM CHARTS */
.bottom-charts{display:flex;gap:6px;flex-shrink:0;height:130px}
.hud-bot{width:50%;flex-shrink:0}
</style>
