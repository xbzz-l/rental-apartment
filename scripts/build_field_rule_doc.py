# -*- coding: utf-8 -*-
"""生成《字段和业务规则》—— 画布版
一块可缩放 / 拖拽的画布：平台框 → 一级模块框 → 功能模块框 → API 层 → 三张表结构卡 → 业务规则便签
"""

import html
import io
import os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# 交付物放在工作区「其他」目录下
OUT = os.path.abspath(os.path.join(BASE, "..", "..", "其他", "字段和业务规则.html"))

BW, BH = 1700, 1400

PLAT = {"x": 600, "y": 46, "w": 460, "h": 62,
        "label": "公寓租房管理平台", "sub": "Vue3 · Spring Boot 3 · MySQL 8"}

DOMAINS = [
    {"x": 120, "w": 420, "name": "TOC APP", "sub": "用户端 H5 · 租客", "no": "01",
     "mods": [("账号与认证", "user"), ("房源浏览", "apartment"), ("房源详情", "apartment"),
              ("我的租房", "booking"), ("个人中心", "user")]},
    {"x": 620, "w": 420, "name": "TOB 运营管理", "sub": "管理端 Web · 运营", "no": "02",
     "mods": [("管理员登录", "无表"), ("房源管理", "apartment"), ("用户管理", "user"),
              ("业务统计", "booking")]},
    {"x": 1120, "w": 420, "name": "TOB BI 数据看板", "sub": "可视化 Web · 管理层", "no": "03",
     "mods": [("管理员登录", "无表"), ("KPI 卡片", "apartment"),
              ("营业额趋势", "booking"), ("房源占比", "apartment")]},
]

D_Y = 186      # 一级模块 y
D_H = 58
M_Y0 = 286     # 首个功能模块 y
M_H = 46
M_STEP = 60
M_W = 300

API = {"x": 120, "y": 636, "w": 1420, "h": 46}

TABLES = [
    {"name": "user", "cn": "用户表", "x": 150, "y": 758, "w": 470,
     "fields": [
         ("id", "BIGINT", "1"), ("username", "VARCHAR(50)", "zhangsan"),
         ("password", "VARCHAR(100)", "$2a$10$N9qo…e1"), ("nickname", "VARCHAR(50)", "zhangsan"),
         ("real_name", "VARCHAR(50)", "张三"), ("age", "INT", "26"),
         ("phone", "VARCHAR(20)", "13800138000"), ("cohabitants", "VARCHAR(500)", "配偶 1 人"),
         ("avatar", "VARCHAR(255)", "/img/a/1.png"), ("role", "VARCHAR(20)", "user"),
         ("create_time", "DATETIME", "2026-10-09 10:12"),
     ]},
    {"name": "apartment", "cn": "房源表", "x": 1090, "y": 758, "w": 450,
     "fields": [
         ("id", "BIGINT", "1"), ("title", "VARCHAR(100)", "望京 SOHO 精装一居"),
         ("images", "TEXT", "/img/1.jpg,/img/1b.jpg"), ("layout", "VARCHAR(50)", "一室一厅"),
         ("area", "DECIMAL(8,2)", "48.50"), ("rent", "INT", "5200"),
         ("status", "VARCHAR(10)", "已租"), ("facilities", "VARCHAR(255)", "空调,洗衣机,WiFi"),
         ("address", "VARCHAR(255)", "朝阳区望京 SOHO T1"),
         ("create_time", "DATETIME", "2026-10-08 14:20"),
         ("update_time", "DATETIME", "2026-10-09 10:20"),
     ]},
    {"name": "booking", "cn": "租房记录表", "x": 670, "y": 758, "w": 380,
     "fields": [
         ("id", "BIGINT", "1"), ("user_id", "BIGINT", "1"),
         ("apartment_id", "BIGINT", "1"), ("status", "VARCHAR(10)", "已预约"),
         ("create_time", "DATETIME", "2026-10-09 10:20"),
     ]},
]

RULES = [
    ("房源状态机",
     "房源只有「可租 / 已租」两种状态。用户预约成功即变「已租」；取消预约后，若该房源已无其他有效预约，才恢复「可租」。"),
    ("预约即锁房",
     "提交预约时，在同一事务内插入预约记录、并把房源改为「已租」。任一步失败整体回滚，不会出现「房被占却没有记录」。"),
    ("取消预约",
     "只有本人、且状态为「已预约」的记录可以取消。取消后置为「已取消」，再确认该房源没有他人预约，才释放回「可租」。"),
    ("级联删除",
     "删除房源时，连带删除该房源下所有预约记录；删除用户时，连带删除其所有记录，并把其占用的房源释放为「可租」。"),
    ("营业额口径",
     "营业额 = 未取消的预约记录所对应房源的月租金之和，按预约时间归入月/日。Demo 无支付模块，租金即收入。"),
    ("账号与权限",
     "账号不可重复、密码加密存储；用户只能操作自己的数据。管理员是配置里的内置账号，不存于三张表中。"),
]

def esc(s):
    return html.escape(str(s), quote=False)


def nodes_html():
    out = []
    # 平台
    out.append(
        '<div class="nd plat" style="left:{x}px;top:{y}px;width:{w}px;height:{h}px">'
        '<div class="plat-t">{t}</div><div class="plat-s">{s}</div></div>'.format(
            x=PLAT["x"], y=PLAT["y"], w=PLAT["w"], h=PLAT["h"],
            t=esc(PLAT["label"]), s=esc(PLAT["sub"])))

    for d in DOMAINS:
        out.append(
            '<div class="nd dom" style="left:{x}px;top:{y}px;width:{w}px;height:{h}px">'
            '<span class="dom-no">{no}</span><div class="dom-n">{n}</div>'
            '<div class="dom-s">{s}</div></div>'.format(
                x=d["x"], y=D_Y, w=d["w"], h=D_H, no=d["no"],
                n=esc(d["name"]), s=esc(d["sub"])))
        mx = d["x"] + (d["w"] - M_W) // 2
        for i, (mn, tab) in enumerate(d["mods"]):
            out.append(
                '<div class="nd mod" style="left:{x}px;top:{y}px;width:{w}px;height:{h}px">'
                '<span class="m-n">{n}</span><span class="m-t">{t}</span></div>'.format(
                    x=mx, y=M_Y0 + i * M_STEP, w=M_W, h=M_H, n=esc(mn), t=esc(tab)))

    out.append(
        '<div class="nd api" style="left:{x}px;top:{y}px;width:{w}px;height:{h}px">'
        '<span class="api-k">API</span>统一 RESTful 接口层 · JWT 鉴权 · 三端共用同一套服务 · 管理端与 BI 共用内置管理员账号（不落表）'
        '</div>'.format(**API))

    for t in TABLES:
        rows = []
        for f, ty, val in t["fields"]:
            rows.append('<div class="row"><span class="f">%s</span>'
                        '<span class="ty">%s</span><span class="v">%s</span></div>'
                        % (esc(f), esc(ty), esc(val)))
        out.append(
            '<div class="nd tbl" style="left:{x}px;top:{y}px;width:{w}px">'
            '<div class="tbl-h"><span class="tbl-n">{n}</span><span class="tbl-c">{c}</span>'
            '<span class="tbl-k">PRIMARY KEY · id</span></div>'
            '<div class="row th"><span class="f">字段</span><span class="ty">类型</span>'
            '<span class="v">示例数据</span></div>{rows}</div>'.format(
                x=t["x"], y=t["y"], w=t["w"], n=esc(t["name"]), c=esc(t["cn"]),
                rows="".join(rows)))

    out.append(
        '<div class="nd dlay" style="left:120px;top:706px;width:1420px;height:430px">'
        '<div class="dlay-h"><span class="dl-k">数据层 · DATA LAYER</span>'
        '<span class="dl-n">三张表被三端各业务模块共用，模块右侧灰字为该模块涉及的表'
        '（多对多，并非一端一表）</span></div></div>')

    for i, (name, desc) in enumerate(RULES):
        out.append(
            '<div class="nd note" style="left:{x}px;top:{y}px;width:460px;height:92px">'
            '<span class="nt-i">{i:02d}</span><div class="nt-n">{n}</div>'
            '<div class="nt-d">{d}</div></div>'.format(
                x=120 + (i % 3) * 476, y=1168 + (i // 3) * 104,
                i=i + 1, n=esc(name), d=esc(desc)))

    out.append(
        '<div class="nd relnote" style="left:670px;top:982px;width:380px">'
        '<div class="rn-h">关系表 · RELATION TABLE</div>'
        '<div>一行 = 一次预约：user_id 是谁，apartment_id 是哪套房</div>'
        '<div>status 三态：已预约 / 已成交 / 已取消</div>'
        '<div>营业额统计只取非「已取消」的记录</div>'
        '</div>')

    out.append('<div class="canvas-label" style="left:120px;top:1146px">业务规则 · BUSINESS RULES</div>')
    out.append('<div class="canvas-label" style="left:120px;top:604px">数据访问 · API LAYER</div>')
    return "".join(out)


def edges_svg():
    p = []
    cx = PLAT["x"] + PLAT["w"] // 2
    bus_y = 148
    p.append("M%d %d V%d" % (cx, PLAT["y"] + PLAT["h"], bus_y))
    p.append("M%d %d H%d" % (DOMAINS[0]["x"] + DOMAINS[0]["w"] // 2, bus_y,
                             DOMAINS[-1]["x"] + DOMAINS[-1]["w"] // 2))
    for d in DOMAINS:
        dcx = d["x"] + d["w"] // 2
        p.append("M%d %d V%d" % (dcx, bus_y, D_Y))
        # 一级 → 首个功能模块
        p.append("M%d %d V%d" % (dcx, D_Y + D_H, M_Y0))
        # 模块之间
        for i in range(len(d["mods"]) - 1):
            y1 = M_Y0 + i * M_STEP + M_H
            p.append("M%d %d V%d" % (dcx, y1, M_Y0 + (i + 1) * M_STEP))
        # 组末 → API
        last_bottom = M_Y0 + (len(d["mods"]) - 1) * M_STEP + M_H
        p.append("M%d %d V%d" % (dcx, last_bottom, API["y"]))
    d_attr = " ".join(p)

    # 表间 ER 关系：user 1—N booking N—1 apartment
    p.append("M620 860 H670")
    p.append("M1050 860 H1090")
    d_attr = " ".join(p)   # 重新拼接，确保 ER 线进入路径

    # API → 数据层（唯一一条，表示所有模块共用同一数据层）
    arrows = ["M830 %d V700" % (API["y"] + API["h"] + 2)]
    er_labels = [("1", 628, 854), ("N", 652, 854), ("N", 1054, 854), ("1", 1076, 854)]
    er_text = "".join(
        '<text x="%d" y="%d" font-size="9" fill="#9aa1ab" '
        'font-family="ui-monospace,monospace">%s</text>' % (x, y, t)
        for t, x, y in er_labels)

    return ('<svg class="edges" width="%d" height="%d" viewBox="0 0 %d %d">'
            '<defs><marker id="ar" viewBox="0 0 10 10" refX="9" refY="5" '
            'markerWidth="6" markerHeight="6" orient="auto-start-reverse">'
            '<path d="M0 0 L10 5 L0 10 z" fill="#b4bcc7"/></marker></defs>'
            '<path d="%s" fill="none" stroke="#b3bcc8" stroke-width="1.4"/>'
            '<path d="%s" fill="none" stroke="#b4bcc7" stroke-width="1.4" '
            'marker-end="url(#ar)"/>%s</svg>'
            % (BW, BH, BW, BH, d_attr, " ".join(arrows), er_text))


CSS = """
:root{
  --ink:#15171c; --ink-2:#4d545e; --ink-3:#8a919c;
  --teal:#0e8f96; --teal-d:#0b7379;
  --mono:"JetBrains Mono","SFMono-Regular",Consolas,monospace;
}
*{box-sizing:border-box}
html,body{height:100%}
body{
  margin:0;overflow:hidden;background:#f4f6f8;color:var(--ink);
  font-family:-apple-system,BlinkMacSystemFont,"Segoe UI","PingFang SC","Microsoft YaHei",sans-serif;
  -webkit-font-smoothing:antialiased;
}
#stage{position:fixed;inset:0;overflow:hidden;cursor:grab;
  background-color:#f4f6f8;
  background-image:radial-gradient(#d9dee5 1px, transparent 1px);
  background-size:22px 22px;background-position:0 0}
#stage.grabbing{cursor:grabbing}
#board{
  position:absolute;left:0;top:0;width:{bw}px;height:{bh}px;
  transform-origin:0 0;background:#fff;
  box-shadow:0 2px 40px rgba(16,24,40,.10);
}

/* ---------- 通用节点 ---------- */
.nd{position:absolute;border-radius:4px;background:#fff}
.plat{background:#15171c;color:#fff;display:flex;flex-direction:column;align-items:center;
  justify-content:center;box-shadow:0 6px 18px rgba(16,24,40,.16);border-radius:6px}
.plat-t{font-size:19px;font-weight:600;letter-spacing:.04em}
.plat-s{font-size:10.5px;color:rgba(255,255,255,.55);font-family:var(--mono);margin-top:2px}

.dom{background:var(--teal);color:#fff;display:flex;flex-direction:column;align-items:center;
  justify-content:center;box-shadow:0 3px 10px rgba(14,143,150,.22)}
.dom-no{position:absolute;left:10px;top:50%;transform:translateY(-50%);
  font-family:var(--mono);font-size:10px;opacity:.55}
.dom-n{font-size:14.5px;font-weight:600;letter-spacing:.04em}
.dom-s{font-size:9.5px;opacity:.8;margin-top:1px;letter-spacing:.02em}

.mod{background:#fff;border:1px solid #c9ced6;display:flex;align-items:center;
  justify-content:center;transition:.15s}
.mod:hover{border-color:var(--teal);box-shadow:0 2px 8px rgba(14,143,150,.14)}
.m-n{font-size:13px;color:#2b3038}
.m-t{position:absolute;right:9px;font-family:var(--mono);font-size:9.5px;color:#9aa1ab}

.api{background:#fafbfc;border:1px dashed #c2c8d1;display:flex;align-items:center;
  justify-content:center;gap:10px;font-size:11.5px;color:var(--ink-3);font-family:var(--mono);letter-spacing:.02em}
.api-k{background:#e8ebef;color:#5b626c;border-radius:2px;padding:1px 7px;font-size:10px;letter-spacing:.1em}

/* ---------- 表卡片 ---------- */
.tbl{border:1px solid #c9ced6;border-radius:4px;overflow:hidden;background:#fff;
  box-shadow:0 2px 10px rgba(16,24,40,.05)}
.tbl-h{display:flex;align-items:baseline;gap:8px;padding:8px 12px;background:#eef8f8;
  border-bottom:1px solid #cfe6e6}
.tbl-n{font-family:var(--mono);font-size:13.5px;font-weight:600;color:var(--teal-d)}
.tbl-c{font-size:11.5px;color:var(--ink-2)}
.tbl-k{margin-left:auto;font-family:var(--mono);font-size:9px;color:#8d94a0;letter-spacing:.04em}
.row{display:grid;grid-template-columns:118px 104px 1fr;align-items:center;
  padding:0 12px;height:26px;border-bottom:1px solid #f1f3f5;font-size:11px}
.row:last-child{border-bottom:none;height:30px}
.row .f{font-family:var(--mono);font-size:11px;color:#232830;font-weight:500}
.row .ty{font-family:var(--mono);font-size:9.5px;color:#8d94a0}
.row .v{font-family:var(--mono);font-size:10.5px;color:#5c636d;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.row.th{background:#fbfcfd;height:24px;border-bottom:1px solid #e8ebef}
.row.th .f,.row.th .ty,.row.th .v{color:#7d8592;font-weight:400;font-size:9.5px;letter-spacing:.04em}
.tbl .row:nth-child(odd){background:#fcfdfd}

/* ---------- 数据层容器 ---------- */
.dlay{border:1px dashed #d0d5dc;background:transparent;border-radius:6px;z-index:0}
.dlay-h{display:flex;align-items:baseline;gap:12px;padding:10px 16px 0;flex-wrap:wrap}
.dl-k{display:flex;align-items:center;gap:7px;font-size:10.5px;color:#6f7784;font-weight:500;letter-spacing:.04em}
.dl-k::before{content://;width:3px;height:11px;background:var(--teal);border-radius:1px}
.dl-n{font-size:10px;color:#7d8592}

/* ---------- 关系表说明 ---------- */
.relnote{border:1px dashed #dde2e9;background:#fcfdfe;padding:9px 13px 10px;
  font-size:10px;color:#79818c;line-height:1.65}
.rn-h{font-family:var(--mono);font-size:9.5px;color:#8d94a0;letter-spacing:.1em;margin-bottom:5px}

/* ---------- 规则便签 ---------- */
.note{background:#fff;border:1px solid #dfe3e9;border-left:2px solid var(--teal);
  padding:9px 13px;border-radius:3px}
.nt-i{font-family:var(--mono);font-size:9px;color:#9aa1ab}
.nt-n{font-size:12px;font-weight:600;margin:0 0 3px;color:#1d2127}
.nt-d{font-size:10.5px;color:#5f6671;line-height:1.62}

.canvas-label{position:absolute;display:flex;align-items:center;gap:7px;
  font-size:10.5px;color:#6f7784;font-weight:500;letter-spacing:.04em}
.canvas-label::before{content:"";width:3px;height:11px;background:var(--teal);border-radius:1px}

/* ---------- HUD ---------- */
.hud{position:fixed;left:24px;top:22px;z-index:10;pointer-events:none}
.hud h1{font-size:15px;margin:0 0 3px;font-weight:600;letter-spacing:.01em}
.hud p{margin:0;font-size:11px;color:var(--ink-3)}
.hud .tags{display:flex;gap:6px;margin-top:8px}
.hud .tags span{background:#fff;border:1px solid #e2e6ec;border-radius:999px;
  padding:1px 9px;font-size:10.5px;color:var(--ink-3)}
.zoom{position:fixed;right:24px;bottom:22px;z-index:10;display:flex;align-items:center;gap:2px;
  background:#fff;border:1px solid #e2e6ec;border-radius:8px;padding:4px;box-shadow:0 2px 10px rgba(16,24,40,.06)}
.zoom button{border:none;background:transparent;color:var(--ink-2);width:26px;height:26px;
  border-radius:5px;cursor:pointer;font-size:14px;line-height:1}
.zoom button:hover{background:#f2f4f7}
.zoom .val{font-family:var(--mono);font-size:11px;color:var(--ink-3);min-width:44px;text-align:center}
.hint{position:fixed;right:24px;top:22px;z-index:10;font-size:10.5px;color:#a5acb6;
  background:#fff;border:1px solid #e8ebef;border-radius:6px;padding:4px 10px}

@media print{
  @page{size:A3 landscape;margin:8mm}
  body{overflow:visible;background:#fff}
  #stage{position:static;overflow:visible;background:#fff;cursor:default}
  #board{position:static;transform:scale(0.82)!important;transform-origin:0 0;box-shadow:none;margin:0}
  .hud,.zoom,.hint{display:none}
}
"""

JS = """
(function () {
  var BW = __BW__, BH = __BH__;
  var stage = document.getElementById('stage');
  var board = document.getElementById('board');
  var val = document.getElementById('zval');
  var s = 1, tx = 0, ty = 0;

  function apply() {
    board.style.transform = 'translate(' + tx + 'px,' + ty + 'px) scale(' + s + ')';
    val.textContent = Math.round(s * 100) + '%';
  }
  function fit() {
    var vw = stage.clientWidth, vh = stage.clientHeight;
    s = Math.min(vw / BW, vh / BH) * 0.95;
    tx = (vw - BW * s) / 2;
    ty = (vh - BH * s) / 2;
    apply();
  }
  function zoomAt(cx, cy, k) {
    var ns = Math.min(2.5, Math.max(0.15, s * k));
    tx = cx - (cx - tx) * (ns / s);
    ty = cy - (cy - ty) * (ns / s);
    s = ns; apply();
  }

  stage.addEventListener('wheel', function (e) {
    e.preventDefault();
    zoomAt(e.clientX, e.clientY, e.deltaY < 0 ? 1.08 : 0.926);
  }, { passive: false });

  var drag = false, sx = 0, sy = 0;
  stage.addEventListener('pointerdown', function (e) {
    drag = true; sx = e.clientX - tx; sy = e.clientY - ty;
    stage.classList.add('grabbing');
  });
  window.addEventListener('pointermove', function (e) {
    if (!drag) return;
    tx = e.clientX - sx; ty = e.clientY - sy; apply();
  });
  window.addEventListener('pointerup', function () {
    drag = false; stage.classList.remove('grabbing');
  });

  document.getElementById('zin').onclick = function () { zoomAt(innerWidth / 2, innerHeight / 2, 1.2); };
  document.getElementById('zout').onclick = function () { zoomAt(innerWidth / 2, innerHeight / 2, 0.83); };
  document.getElementById('zfit').onclick = fit;
  window.addEventListener('resize', fit);
  fit();
})();
"""


def build():
    doc = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>公寓租房管理平台 · 字段与业务规则</title>
<style>{css}</style>
</head>
<body>
<div id="stage">
  <div id="board">
    {edges}
    {nodes}
  </div>
</div>

<div class="hud">
  <h1>公寓租房管理平台 · 字段与业务规则</h1>
  <p>平台模块结构 → 接口层 → 数据表结构 → 业务规则</p>
  <div class="tags"><span>3 个一级模块</span><span>{n} 个功能模块</span><span>3 张数据表</span><span>6 条业务规则</span></div>
</div>
<div class="hint">滚轮缩放 · 拖拽平移</div>
<div class="zoom">
  <button id="zout" title="缩小">−</button>
  <span class="val" id="zval">100%</span>
  <button id="zin" title="放大">+</button>
  <button id="zfit" title="适应窗口" style="width:auto;padding:0 8px;font-size:11px">适应</button>
</div>

<script>{js}</script>
</body>
</html>
""".format(css=CSS.replace("{bw}", str(BW)).replace("{bh}", str(BH)),
           edges=edges_svg(), nodes=nodes_html(),
           n=sum(len(d["mods"]) for d in DOMAINS),
           js=JS.replace("__BW__", str(BW)).replace("__BH__", str(BH)))

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with io.open(OUT, "w", encoding="utf-8") as f:
        f.write(doc)
    print("written:", OUT, len(doc), "chars")


if __name__ == "__main__":
    build()
