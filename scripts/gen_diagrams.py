# -*- coding: utf-8 -*-
"""生成PRD文档内嵌流程图PNG（matplotlib绘制，微软雅黑字体）

输出：docs/assets/flow-main.png          用户租房主流程
      docs/assets/flow-closed-loop.png   三端数据闭环图
      docs/assets/flow-architecture.png  整体技术架构图
"""
import os
import sys

sys.stdout.reconfigure(encoding="utf-8")

from matplotlib import font_manager, pyplot as plt
from matplotlib.patches import FancyBboxPatch, Polygon

FP = r"C:\Windows\Fonts\msyh.ttc"
font_manager.fontManager.addfont(FP)
plt.rcParams["font.family"] = font_manager.FontProperties(fname=FP).get_name()
plt.rcParams["axes.unicode_minus"] = False
plt.rcParams["savefig.facecolor"] = "white"

OUT_DIR = os.path.normpath(os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "..", "docs", "assets"))
os.makedirs(OUT_DIR, exist_ok=True)

# 统一配色（浅色填充 + 深色描边）
C_BOX = dict(fc="#E8F1FB", ec="#2E6DA4")     # 普通流程节点
C_DIA = dict(fc="#FFF3E0", ec="#E08A00")     # 判断菱形
C_TERM = dict(fc="#E8F5E9", ec="#3E8E41")    # 开始/结束
C_GROUP = dict(fc="#F6F8FA", ec="#7A8B99")   # 端分组框
C_DB = dict(fc="#FDF2E9", ec="#C77C2E")      # 数据库
ARROW = "#444444"


def box(ax, cx, cy, text, w=4.6, h=1.0, style=C_BOX, fs=11, bold=False):
    ax.add_patch(FancyBboxPatch(
        (cx - w / 2, cy - h / 2), w, h,
        boxstyle="round,pad=0.02,rounding_size=0.08",
        linewidth=1.8, **style))
    ax.text(cx, cy, text, ha="center", va="center", fontsize=fs,
            fontweight="bold" if bold else "normal")


def term(ax, cx, cy, text, w=2.6, h=0.9):
    ax.add_patch(FancyBboxPatch(
        (cx - w / 2, cy - h / 2), w, h,
        boxstyle="round,pad=0.02,rounding_size=0.45",
        linewidth=1.8, **C_TERM))
    ax.text(cx, cy, text, ha="center", va="center", fontsize=11, fontweight="bold")


def diamond(ax, cx, cy, text, w=4.4, h=1.8):
    ax.add_patch(Polygon(
        [(cx, cy + h / 2), (cx + w / 2, cy), (cx, cy - h / 2), (cx - w / 2, cy)],
        closed=True, linewidth=1.8, **C_DIA))
    ax.text(cx, cy, text, ha="center", va="center", fontsize=11)


def group(ax, x, y, w, h, title, title_y):
    ax.add_patch(FancyBboxPatch(
        (x, y), w, h, boxstyle="round,pad=0.02,rounding_size=0.12",
        linewidth=2.0, **C_GROUP))
    ax.text(x + w / 2, title_y, title, ha="center", va="center",
            fontsize=12, fontweight="bold")


def arrow(ax, x1, y1, x2, y2, label=None, lx=None, ly=None, rot=0, double=False):
    style = "<|-|>" if double else "-|>"
    ax.annotate("", xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle=style, color=ARROW, lw=1.6,
                                shrinkA=0, shrinkB=0, mutation_scale=16))
    if label:
        ax.text(lx, ly, label, fontsize=10, color="#333333",
                ha="center", va="center", rotation=rot)


def line(ax, x1, y1, x2, y2):
    ax.plot([x1, x2], [y1, y2], color=ARROW, lw=1.6)


def new_fig(w, h, xlim, ylim):
    fig, ax = plt.subplots(figsize=(w, h))
    ax.set_xlim(*xlim)
    ax.set_ylim(*ylim)
    ax.set_aspect("equal")
    ax.axis("off")
    return fig, ax


def save(fig, name):
    path = os.path.join(OUT_DIR, name)
    fig.savefig(path, dpi=150, bbox_inches="tight", pad_inches=0.08)
    plt.close(fig)
    print("saved:", path)


# ============ 图1：用户租房主流程 ============
def draw_main_flow():
    fig, ax = new_fig(7.0, 12.25, (0, 10), (0, 17.5))

    term(ax, 5, 16.2, "开始")
    box(ax, 5, 14.4, "注册/登录")
    box(ax, 5, 12.8, "浏览首页房源列表")
    box(ax, 5, 11.2, "查看房源详情")
    diamond(ax, 5, 8.9, "房源状态是否可租？")
    box(ax, 5, 6.8, "点击「立即预约租房」")
    box(ax, 5, 5.2, "提交预约信息")
    box(ax, 5, 3.6, "生成租房记录\n房源状态变更为「已租」", h=1.2)
    box(ax, 5, 2.0, "个人中心查看租房记录")
    term(ax, 5, 0.7, "结束")

    # 主链路箭头
    arrow(ax, 5, 15.75, 5, 14.9)
    arrow(ax, 5, 13.9, 5, 13.3)
    arrow(ax, 5, 12.3, 5, 11.7)
    arrow(ax, 5, 10.7, 5, 9.8)
    arrow(ax, 5, 8.0, 5, 7.3, label="可租", lx=5.4, ly=7.65)
    arrow(ax, 5, 6.3, 5, 5.7)
    arrow(ax, 5, 4.7, 5, 4.2)
    arrow(ax, 5, 3.0, 5, 2.5)
    arrow(ax, 5, 1.5, 5, 1.15)

    # 已租回环分支
    line(ax, 7.2, 8.9, 8.6, 8.9)
    line(ax, 8.6, 8.9, 8.6, 12.8)
    arrow(ax, 8.6, 12.8, 7.32, 12.8)
    ax.text(7.9, 9.2, "已租", fontsize=10, color="#333333", ha="center", va="center")
    ax.text(8.95, 10.85, "返回列表", fontsize=10, color="#333333",
            ha="center", va="center", rotation=90)

    save(fig, "flow-main.png")


# ============ 图2：三端数据闭环图 ============
def draw_closed_loop():
    fig, ax = new_fig(10.4, 5.71, (0, 16.4), (0, 9))

    # 用户端APP
    group(ax, 0.5, 2.2, 4.4, 4.6, "用户端APP（移动端H5）", 6.15)
    box(ax, 2.7, 4.7, "浏览房源", w=3.2, h=0.95)
    box(ax, 2.7, 2.75, "预约租房", w=3.2, h=0.95)
    arrow(ax, 2.7, 4.225, 2.7, 3.225)

    # 数据库
    ax.add_patch(FancyBboxPatch(
        (6.4, 3.4), 3.4, 2.2, boxstyle="round,pad=0.02,rounding_size=0.12",
        linewidth=2.0, **C_DB))
    ax.text(8.1, 4.75, "MySQL 数据库", fontsize=12, fontweight="bold",
            ha="center", va="center")
    ax.text(8.1, 4.2, "业务数据统一存储", fontsize=9.5, ha="center", va="center")

    # 管理端Web
    group(ax, 11.6, 5.0, 4.3, 3.3, "管理端Web后台", 7.75)
    for i, t in enumerate(["房源增删改查", "用户信息管理", "基础业务统计"]):
        box(ax, 13.75, 6.85 - i * 0.9, t, w=3.5, h=0.72, fs=10)

    # BI看板
    group(ax, 11.6, 0.8, 4.3, 3.3, "BI数据看板", 3.5)
    for i, t in enumerate(["成交/预约图表", "房源占比图表", "KPI数据卡片"]):
        box(ax, 13.75, 2.65 - i * 0.8, t, w=3.5, h=0.66, fs=10)

    # 数据流向
    arrow(ax, 4.9, 4.5, 6.4, 4.5, label="写入业务数据", lx=5.65, ly=4.9)
    arrow(ax, 9.8, 5.4, 11.6, 5.4, label="读取/管控", lx=10.7, ly=5.75, double=True)
    line(ax, 8.1, 3.4, 8.1, 2.5)
    arrow(ax, 8.1, 2.5, 11.58, 2.5, label="统计查询", lx=9.85, ly=2.82)

    save(fig, "flow-closed-loop.png")


# ============ 图3：整体技术架构图 ============
def draw_architecture():
    fig, ax = new_fig(10.4, 5.71, (0, 16.4), (0, 9))

    # 前端层
    group(ax, 0.5, 1.0, 4.7, 7.0, "前端层 · Vue3 三端", 7.35)
    for i, t in enumerate(["用户端H5\nVue3 + Vant4 + Pinia",
                           "管理端Web\nVue3 + Element Plus",
                           "BI看板Web\nVue3 + Element Plus + ECharts"]):
        box(ax, 2.85, 5.7 - i * 1.55, t, w=3.9, h=1.0, fs=10)

    # 后端层
    group(ax, 6.9, 3.1, 4.1, 3.0, "后端层 · Spring Boot", 5.5)
    box(ax, 8.95, 4.5, "RESTful API 统一接口", w=3.4, h=0.85, fs=10)
    box(ax, 8.95, 3.62, "JWT 登录认证", w=3.4, h=0.85, fs=10)

    # 数据层
    ax.add_patch(FancyBboxPatch(
        (12.6, 3.5), 3.2, 2.2, boxstyle="round,pad=0.02,rounding_size=0.12",
        linewidth=2.0, **C_DB))
    ax.text(14.2, 5.05, "数据层", fontsize=12, fontweight="bold",
            ha="center", va="center")
    ax.text(14.2, 4.35, "MySQL 8.0", fontsize=11, ha="center", va="center")

    # 调用关系
    arrow(ax, 5.2, 4.5, 6.9, 4.5, label="Axios / HTTP", lx=6.05, ly=4.9)
    arrow(ax, 11.0, 4.6, 12.6, 4.6, label="MyBatis-Plus", lx=11.8, ly=5.05)

    save(fig, "flow-architecture.png")


if __name__ == "__main__":
    draw_main_flow()
    draw_closed_loop()
    draw_architecture()
