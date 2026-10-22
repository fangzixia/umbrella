'use strict';

import { createGraphCanvas } from './graph-canvas.js';

let graph = {
  name: '图谱',
  initialized: false,
  chatAvailable: false,
  masterChatAvailable: false,
  fleetSyncRunning: false,
  fleetRegisteredCount: 0,
  fleetTargetCount: 0,
  fleetFullySynced: false,
  nodeAgentReady: null,
  nodes: [],
  edges: [],
};
let fleetPollTimer = null;
let chatMode = 'node';
const MASTER_CHAT_KEY = '__master__';
let connectionError = '';
let loading = false;
let selected;
let connected = false;
const conversations = new Map();
/** @type {Awaited<ReturnType<createGraphCanvas>> | null} */
let graphCanvas = null;
let lastInspectorTab = 'info';

function textElement(tag, text, className) {
  const el = document.createElement(tag);
  el.textContent = text;
  if (className) el.className = className;
  return el;
}

function isAgent(node) {
  return node.type.toLowerCase() === 'agent';
}

function updateCanvasHint() {
  const el = document.getElementById('canvas-hint');
  if (!graphCanvas) {
    el.textContent = '正在初始化图谱画布…';
    return;
  }
  el.textContent = '力导向 · 滚轮缩放 · 拖动画布 · ☰ 打开节点面板。';
}

function draw({ fit = true } = {}) {
  document.title = graph.name ? `Umbrella · ${graph.name}` : 'Umbrella · 图谱';
  const nameEl = document.getElementById('graph-name');
  const countsEl = document.getElementById('counts');
  if (nameEl) nameEl.textContent = loading ? '加载图谱…' : graph.name || '图谱';
  if (countsEl) {
    countsEl.textContent =
      graph.nodes.length > 0 ? `${graph.nodes.length} 节点 · ${graph.edges.length} 关系` : '';
  }
  updateConnectionChrome();
  document.getElementById('empty').hidden = graph.nodes.length > 0;
  document.getElementById('empty').textContent = loading
    ? '正在加载后台图谱…'
    : connectionError || (!graph.initialized ? '后台图谱尚未初始化。' : '图谱中暂无节点。');
  if (graphCanvas && graph.nodes.length) {
    graphCanvas.loadGraph(graph, { refit: fit ? 'all' : 'none' });
  }
  if (!selected || !graph.nodes.some((n) => n.id === selected)) {
    selected = graph.nodes.find(isAgent)?.id ?? graph.nodes[0]?.id ?? null;
  }
  refreshDetailsPanel(selected);
  renderChat();
  updateCanvasHint();
}

function refreshDetailsPanel(id) {
  const details = document.getElementById('details');
  details.replaceChildren();
  if (!id) {
    details.append(textElement('p', '选择图上的节点，查看它的属性、能力和关联。', 'muted'));
    return;
  }
  const node = graph.nodes.find((n) => n.id === id);
  if (!node) {
    details.append(textElement('p', '选择图上的节点，查看它的属性、能力和关联。', 'muted'));
    return;
  }
  details.append(
    textElement('div', isAgent(node) ? '✳' : '◇', 'node-icon'),
    textElement('div', isAgent(node) ? 'AGENT' : node.type.toUpperCase(), 'node-type'),
    textElement('h2', node.displayName),
    textElement('div', node.id, 'node-id'),
    textElement('div', '', 'separator'),
    textElement('h3', '基本信息'),
  );
  const dl = document.createElement('dl');
  for (const [key, value] of Object.entries({ 类型: node.type, ...node.properties })) {
    const item = document.createElement('div');
    item.className = 'property';
    item.append(textElement('dt', key), textElement('dd', String(value)));
    dl.append(item);
  }
  details.append(dl, textElement('h3', isAgent(node) ? 'Agent 能力' : '节点能力'));
  if (!node.capabilities.length) details.append(textElement('p', '该节点未声明能力。', 'muted'));
  node.capabilities.forEach((cap) => {
    const item = document.createElement('div');
    item.className = 'cap';
    item.append(textElement('strong', cap.name), textElement('p', cap.description || '暂无描述'));
    details.append(item);
  });
  details.append(textElement('h3', '关联节点'));
  const edges = graph.edges.filter((e) => e.source === id || e.target === id);
  if (!edges.length) details.append(textElement('p', '暂无关联。', 'muted'));
  edges.forEach((edge) => {
    const other = graph.nodes.find((n) => n.id === (edge.source === id ? edge.target : edge.source));
    if (!other) return;
    const row = textElement('div', `${edge.source === id ? '→' : '←'} ${other.displayName}`, 'relation');
    row.append(textElement('small', edge.relationType));
    details.append(row);
  });
}

function select(id, center = true) {
  selected = id;
  void refreshNodeReady(id);
  if (graphCanvas && id) {
    if (center) graphCanvas.focusNode(id, { highlightNeighbors: true });
    else graphCanvas.selectNode(id);
  }
  refreshDetailsPanel(id);
  renderChat();
}

function emptyGraph() {
  return {
    name: '图谱',
    initialized: false,
    chatAvailable: false,
    masterChatAvailable: false,
    fleetSyncRunning: false,
    fleetRegisteredCount: 0,
    fleetTargetCount: 0,
    fleetFullySynced: false,
    nodeAgentReady: null,
    nodes: [],
    edges: [],
  };
}

function parseViewPayload(data) {
  if (
    typeof data.name !== 'string' ||
    !data.name.trim() ||
    typeof data.initialized !== 'boolean' ||
    typeof data.chatAvailable !== 'boolean' ||
    typeof data.masterChatAvailable !== 'boolean' ||
    typeof data.fleetSyncRunning !== 'boolean' ||
    typeof data.fleetRegisteredCount !== 'number' ||
    typeof data.fleetTargetCount !== 'number' ||
    typeof data.fleetFullySynced !== 'boolean' ||
    !Array.isArray(data.nodes) ||
    !Array.isArray(data.edges) ||
    !data.nodes.every(
      (n) =>
        typeof n.id === 'string' &&
        typeof n.type === 'string' &&
        typeof n.displayName === 'string' &&
        Array.isArray(n.capabilities),
    )
  ) {
    throw Error('后台图谱数据格式异常，请检查应用版本。');
  }
  const ready = data.nodeAgentReady;
  return { ...data, nodeAgentReady: ready === null || ready === undefined ? null : !!ready };
}

function updateConnectionChrome() {
  const text = document.getElementById('status-text');
  const dot = document.getElementById('status-dot');
  if (!text) return;
  if (!connected) {
    text.textContent = loading ? '连接中…' : connectionError ? '未连接' : '未连接';
    dot?.classList.remove('ok');
    return;
  }
  if (!graph.initialized) {
    text.textContent = '未初始化';
    dot?.classList.remove('ok');
    return;
  }
  dot?.classList.add('ok');
  if (graph.fleetTargetCount > 0) {
    if (graph.fleetFullySynced) text.textContent = `Agent ${graph.fleetRegisteredCount}/${graph.fleetTargetCount} 就绪`;
    else if (graph.fleetSyncRunning) text.textContent = `Agent 同步 ${graph.fleetRegisteredCount}/${graph.fleetTargetCount}`;
    else text.textContent = `Agent ${graph.fleetRegisteredCount}/${graph.fleetTargetCount}`;
  } else text.textContent = '已连接';
}

function scheduleFleetPoll() {
  clearFleetPoll();
  if (!connected || !graph.initialized || graph.fleetFullySynced) return;
  fleetPollTimer = setInterval(() => {
    if (!document.hidden) void load(false);
  }, 5000);
}

function clearFleetPoll() {
  if (fleetPollTimer) {
    clearInterval(fleetPollTimer);
    fleetPollTimer = null;
  }
}

async function refreshNodeReady(nodeId) {
  if (!connected || !nodeId) {
    graph.nodeAgentReady = null;
    return;
  }
  try {
    const response = await fetch('/api/graph/view?nodeId=' + encodeURIComponent(nodeId), {
      headers: { Accept: 'application/json' },
      cache: 'no-store',
      signal: AbortSignal.timeout(15000),
    });
    if (!response.ok) return;
    const data = parseViewPayload(await response.json());
    graph.fleetSyncRunning = data.fleetSyncRunning;
    graph.fleetRegisteredCount = data.fleetRegisteredCount;
    graph.fleetTargetCount = data.fleetTargetCount;
    graph.fleetFullySynced = data.fleetFullySynced;
    if (nodeId === selected) graph.nodeAgentReady = data.nodeAgentReady;
    updateConnectionChrome();
    scheduleFleetPoll();
    if (activeChatKey() === nodeId) renderChat();
  } catch {
    /* ignore */
  }
}

async function load(refreshFromSource = false) {
  const button = document.getElementById('reload');
  const previousSelection = selected;
  button.disabled = true;
  loading = true;
  connected = false;
  connectionError = '';
  graph = emptyGraph();
  updateConnectionChrome();
  draw();
  try {
    if (refreshFromSource) {
      const refresh = await fetch('/api/graph/refresh', {
        method: 'POST',
        headers: { Accept: 'application/json' },
        cache: 'no-store',
        signal: AbortSignal.timeout(120000),
      });
      if (!refresh.ok) throw Error(`图谱刷新失败（HTTP ${refresh.status}），请检查后台后重试。`);
    }
    const viewUrl =
      previousSelection && !refreshFromSource
        ? '/api/graph/view?nodeId=' + encodeURIComponent(previousSelection)
        : '/api/graph/view';
    const response = await fetch(viewUrl, {
      headers: { Accept: 'application/json' },
      cache: 'no-store',
      signal: AbortSignal.timeout(15000),
    });
    if (!response.ok) throw Error(`图谱加载失败（HTTP ${response.status}），请检查后台后重试。`);
    graph = parseViewPayload(await response.json());
    connected = true;
    updateConnectionChrome();
    scheduleFleetPoll();
  } catch (error) {
    connectionError =
      error.name === 'TimeoutError'
        ? '连接后台超时，请重试。'
        : error instanceof TypeError
          ? '无法连接后台，请确认 Umbrella 应用已启动后重试。'
          : error instanceof SyntaxError
            ? '后台未返回图谱数据，请从 Umbrella 应用首页打开页面。'
            : error.message;
    updateConnectionChrome();
    clearFleetPoll();
  } finally {
    loading = false;
    selected = previousSelection;
    if (selected && graph.nodeAgentReady === null) void refreshNodeReady(selected);
    draw({ fit: !previousSelection });
    button.disabled = false;
  }
}

function conversation(id) {
  if (!conversations.has(id))
    conversations.set(id, { messages: [], draft: '', pending: false, error: '', pendingText: '' });
  return conversations.get(id);
}

function activeChatKey() {
  return chatMode === 'master' ? MASTER_CHAT_KEY : selected;
}

function chatUnavailable() {
  if (!connected) return loading ? '正在连接后台…' : '后台未连接，对话暂不可用。';
  if (chatMode === 'master') {
    if (!graph.masterChatAvailable) return '总控 Agent 尚未就绪，请先启用并配置 DashScope。';
    return '';
  }
  if (!selected) return '选择图上的节点，即可与该节点的 Agent 对话。';
  if (!graph.chatAvailable) return '对话模型尚未配置，请先启用并配置 DashScope。';
  if (graph.nodeAgentReady === false) {
    const progress = graph.fleetTargetCount > 0 ? '（' + graph.fleetRegisteredCount + '/' + graph.fleetTargetCount + ' 已就绪）' : '';
    return graph.fleetSyncRunning
      ? '该节点 Agent 仍在后台初始化' + progress + '，请稍后再试。'
      : '该节点 Agent 尚未就绪' + progress + '，请稍后重试或点击「刷新图谱」。';
  }
  if (graph.nodeAgentReady === null && graph.fleetTargetCount > 0 && !graph.fleetFullySynced)
    return '正在确认该节点 Agent 是否就绪…';
  return '';
}

function renderChat() {
  const node = graph.nodes.find((n) => n.id === selected);
  const key = activeChatKey();
  const state = key ? conversation(key) : null;
  document.getElementById('chat-target').textContent =
    chatMode === 'master' ? '总控 Agent · 编排' : node ? `${node.displayName} · Agent` : '选择一个节点';
  const messages = document.getElementById('chat-messages');
  messages.replaceChildren();
  if (!state?.messages.length) {
    const empty = textElement(
      'div',
      chatMode === 'master'
        ? '与总控 Agent 对话：可分析全图、向各节点 Agent 下达任务并汇总。'
        : node
          ? `与 ${node.displayName} 的 Agent 开始对话。它会结合该节点的属性、能力与关联回答。`
          : '点击图谱中的节点，选择对话对象。',
      'chat-empty',
    );
    messages.append(empty);
  }
  const appendMessage = (role, content) => {
    const bubble = document.createElement('article');
    bubble.className = `chat-message ${role}`;
    const label = role === 'user' ? '你' : chatMode === 'master' ? '总控 Agent' : `${node.displayName} · Agent`;
    bubble.append(textElement('span', label, 'message-role'), textElement('p', content));
    messages.append(bubble);
  };
  state?.messages.forEach((turn) => appendMessage(turn.role, turn.content));
  if (state?.pending) {
    appendMessage('user', state.pendingText);
    messages.append(textElement('div', 'Agent 正在回复…', 'chat-waiting'));
  }
  const input = document.getElementById('chat-input');
  input.value = state?.draft ?? '';
  input.disabled = !!chatUnavailable() || !!state?.pending || (chatMode === 'node' && !node);
  document.getElementById('send-chat').disabled = input.disabled || !input.value.trim();
  document.getElementById('clear-chat').disabled = !key || !!state?.pending || !state?.messages.length;
  document.getElementById('chat-status').textContent =
    state?.error ||
    chatUnavailable() ||
    (chatMode === 'master' ? '总控对话记录仅保留在本页。' : '对话记录仅保留在当前页面，各节点独立。');
  document.getElementById('chat-status').classList.toggle('chat-error', !!state?.error);
  messages.scrollTop = messages.scrollHeight;
}

function openInspector(mode, focus = false) {
  lastInspectorTab = mode;
  document.querySelector('main').classList.add('inspector-open');
  setInspectorTab(mode, focus);
}

function closeInspector() {
  document.querySelector('main').classList.remove('inspector-open');
}

function toggleInspectorPanel() {
  const main = document.querySelector('main');
  if (main.classList.contains('inspector-open')) closeInspector();
  else openInspector(lastInspectorTab);
}

function setInspectorTab(mode, focus = false) {
  lastInspectorTab = mode;
  chatMode = mode;
  const masterTab = document.getElementById('master-chat-tab');
  const chatTab = document.getElementById('chat-tab');
  const infoTab = document.getElementById('info-tab');
  masterTab.setAttribute('aria-selected', String(mode === 'master'));
  chatTab.setAttribute('aria-selected', String(mode === 'node'));
  infoTab.setAttribute('aria-selected', String(mode === 'info'));
  masterTab.tabIndex = mode === 'master' ? 0 : -1;
  chatTab.tabIndex = mode === 'node' ? 0 : -1;
  infoTab.tabIndex = mode === 'info' ? 0 : -1;
  document.getElementById('chat-pane').hidden = mode === 'info';
  document.getElementById('details').hidden = mode !== 'info';
  if (focus) (mode === 'master' ? masterTab : mode === 'node' ? chatTab : infoTab).focus();
  renderChat();
}

function bindUi() {
  document.getElementById('reload').addEventListener('click', () => load(true));
  document.getElementById('reset').addEventListener('click', () => graphCanvas?.fitAll());
  document.getElementById('inspector-close').addEventListener('click', () => closeInspector());
  document.getElementById('open-inspector').addEventListener('click', () => toggleInspectorPanel());
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden && connected && !graph.fleetFullySynced) void load(false);
  });
  for (const [id, mode] of [
    ['master-chat-tab', 'master'],
    ['chat-tab', 'node'],
    ['info-tab', 'info'],
  ]) {
    document.getElementById(id).addEventListener('click', () => openInspector(mode));
  }
  document.getElementById('chat-input').addEventListener('input', (event) => {
    const key = activeChatKey();
    if (!key) return;
    conversation(key).draft = event.target.value;
    document.getElementById('send-chat').disabled = !event.target.value.trim() || conversation(key).pending;
  });
  document.getElementById('chat-input').addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
      event.preventDefault();
      document.getElementById('chat-form').requestSubmit();
    }
  });
  document.getElementById('clear-chat').addEventListener('click', () => {
    const key = activeChatKey();
    if (!key || conversation(key).pending) return;
    conversations.delete(key);
    renderChat();
    document.getElementById('chat-input').focus();
  });
  document.getElementById('chat-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const key = activeChatKey();
    if (!key || chatUnavailable()) return;
    const state = conversation(key);
    const message = state.draft.trim();
    if (state.pending || !message) return;
    state.pending = true;
    state.pendingText = message;
    state.error = '';
    renderChat();
    try {
      const isMaster = chatMode === 'master';
      const url = isMaster ? '/api/graph/master/chat' : '/api/graph/chat';
      const body = isMaster
        ? {
            message,
            history: state.messages.slice(-40).map((turn) => ({
              role: turn.role,
              content: turn.content.slice(0, 8000),
            })),
          }
        : {
            nodeId: selected,
            message,
            history: state.messages.slice(-40).map((turn) => ({
              role: turn.role,
              content: turn.content.slice(0, 8000),
            })),
          };
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify(body),
        signal: AbortSignal.timeout(100000),
      });
      let data;
      try {
        data = await response.json();
      } catch {
        throw Error('对话服务未返回有效回复，请确认应用已启动。');
      }
      if (!response.ok) throw Error(data.error || 'Agent 暂时无法回复，请稍后重试。');
      if (isMaster) {
        if (typeof data.message !== 'string' || !data.message.trim()) throw Error('总控未返回有效回复。');
      } else {
        if (data.nodeId !== selected || typeof data.message !== 'string' || !data.message.trim())
          throw Error('回复与当前节点不匹配，请重试。');
      }
      state.messages.push({ role: 'user', content: message }, { role: 'assistant', content: data.message });
      state.draft = '';
      if (!isMaster && selected) graph.nodeAgentReady = true;
    } catch (error) {
      state.error =
        error.name === 'TimeoutError'
          ? 'Agent 响应超时，请稍后重试。'
          : error instanceof TypeError
            ? '无法连接对话服务，请检查应用连接后重试。'
            : error.message;
      if (
        !isMaster &&
        selected &&
        (state.error.includes('尚未就绪') || state.error.includes('初始化'))
      ) {
        graph.nodeAgentReady = false;
        void refreshNodeReady(selected);
      }
    } finally {
      state.pending = false;
      state.pendingText = '';
      if (activeChatKey() === key) {
        renderChat();
        if (!document.getElementById('chat-input').disabled) document.getElementById('chat-input').focus();
      }
    }
  });
}

async function bootstrap() {
  bindUi();
  const container = document.getElementById('graph-container');
  try {
    graphCanvas = await createGraphCanvas(container, {
      onSelect: (nodeId) => {
        select(nodeId, false);
      },
    });
  } catch {
    /* error UI in container */
  }
  await load(false);
}

bootstrap();
