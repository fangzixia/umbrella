'use strict';

const CDN = {
  cytoscape: 'https://cdn.jsdelivr.net/npm/cytoscape@3.30.4/+esm',
  coseBilkent: 'https://cdn.jsdelivr.net/npm/cytoscape-cose-bilkent@4.1.0/+esm',
};

let cytoscapeModulePromise;

function registerFromVendor() {
  const cyFactory = globalThis.cytoscape;
  if (!cyFactory) return null;
  if (globalThis.cytoscapeCoseBilkent && !cyFactory._umbrellaCose) {
    cyFactory.use(globalThis.cytoscapeCoseBilkent);
    cyFactory._umbrellaCose = true;
  }
  return cyFactory;
}

async function loadCytoscapeFromCdn() {
  const [cytoscape, coseBilkent] = await Promise.all([
    import(CDN.cytoscape),
    import(CDN.coseBilkent),
  ]);
  const cyFactory = cytoscape.default;
  cyFactory.use(coseBilkent.default);
  return cyFactory;
}

async function loadCytoscape() {
  if (!cytoscapeModulePromise) {
    cytoscapeModulePromise = (async () => {
      const vendor = registerFromVendor();
      if (vendor) return vendor;
      return loadCytoscapeFromCdn();
    })();
  }
  return cytoscapeModulePromise;
}

function isAgentType(type) {
  return String(type || '').toLowerCase() === 'agent';
}

function labelOpacityForZoom(zoom) {
  if (zoom >= 0.55) return 1;
  if (zoom >= 0.35) return 0.65;
  return 0;
}

function forceLayoutOptions(nodeCount) {
  const scale = Math.sqrt(Math.max(nodeCount, 1));
  return {
    name: 'cose-bilkent',
    animate: true,
    animationDuration: 700,
    fit: false,
    padding: 60,
    randomize: false,
    nodeDimensionsIncludeLabels: true,
    nodeRepulsion: Math.round(Math.min(20000, Math.max(10000, 8000 + scale * 120))),
    idealEdgeLength: Math.round(Math.min(160, Math.max(80, 50 + scale * 2))),
    edgeElasticity: 0.42,
    gravity: 0.12,
    numIter: Math.round(Math.min(3500, Math.max(2200, 1800 + scale * 40))),
  };
}

/** 祖先链 + 子树，定位时少把同层兄弟全塞进视口 */
function focusScope(node) {
  return node.union(node.predecessors('node')).union(node.successors('node'));
}

/**
 * @param {HTMLElement} container
 * @param {{ onSelect: (nodeId: string) => void }} hooks
 */
export async function createGraphCanvas(container, hooks) {
  let cytoscape;
  try {
    cytoscape = await loadCytoscape();
  } catch (error) {
    container.replaceChildren();
    const msg = document.createElement('p');
    msg.className = 'graph-load-error';
    msg.textContent =
      '无法加载图谱渲染组件。请确认 static/vendor 下依赖完整，或本机可访问 jsDelivr CDN。';
    container.append(msg);
    throw error;
  }

  let cy;

  const stylesheet = [
    {
      selector: 'node',
      style: {
        label: 'data(label)',
        'text-valign': 'bottom',
        'text-halign': 'center',
        'font-size': 9,
        'font-family': 'Consolas, "Microsoft YaHei", sans-serif',
        color: '#d7f58b',
        'text-margin-y': 6,
        'text-opacity': 0,
        'text-max-width': 120,
        'text-wrap': 'ellipsis',
        width: 14,
        height: 14,
        'background-color': '#3f5a49',
        'border-width': 1,
        'border-color': '#607d64',
      },
    },
    {
      selector: 'node.agent',
      style: {
        'background-color': '#829e54',
        'border-color': '#d7f58b',
        width: 16,
        height: 16,
      },
    },
    {
      selector: 'node:selected',
      style: {
        'border-width': 3,
        'border-color': '#d7f58b',
        'background-color': '#2e3e23',
      },
    },
    {
      selector: 'node.highlight',
      style: {
        'border-width': 2,
        'border-color': '#a8c976',
      },
    },
    {
      selector: 'edge',
      style: {
        width: 1,
        'line-color': '#526e56',
        'target-arrow-color': '#526e56',
        'target-arrow-shape': 'triangle',
        'curve-style': 'bezier',
        opacity: 0.45,
        label: 'data(relationType)',
        'font-size': 8,
        color: '#81977e',
        'text-opacity': 0,
        'text-rotation': 'autorotate',
      },
    },
    {
      selector: 'edge:selected',
      style: { opacity: 0.9, width: 1.5, 'line-color': '#d7f58b', 'target-arrow-color': '#d7f58b' },
    },
    {
      selector: 'edge.adjacent-edge',
      style: {
        opacity: 0.9,
        width: 2,
        'line-color': '#a8c976',
        'target-arrow-color': '#a8c976',
      },
    },
    {
      selector: 'node.faded',
      style: { opacity: 0.22 },
    },
    {
      selector: 'edge.faded',
      style: { opacity: 0.12 },
    },
  ];

  cy = cytoscape({
    container,
    style: stylesheet,
    minZoom: 0.02,
    maxZoom: 4,
    wheelSensitivity: 0.2,
    layout: { name: 'preset' },
  });

  function applyZoomLabels() {
    const opacity = labelOpacityForZoom(cy.zoom());
    cy.style()
      .selector('node')
      .style('text-opacity', opacity)
      .selector('edge')
      .style('text-opacity', opacity >= 0.75 ? 0.75 : 0)
      .update();
  }

  cy.on('zoom', applyZoomLabels);

  function clearAdjacentHighlight() {
    cy.$('.highlight').removeClass('highlight');
    cy.$('.adjacent-edge').removeClass('adjacent-edge');
    cy.$('.faded').removeClass('faded');
  }

  function highlightAdjacentTo(node) {
    clearAdjacentHighlight();
    if (!node || node.empty()) return;
    const hood = node.closedNeighborhood();
    hood.nodes().not(node).addClass('highlight');
    hood.edges().addClass('adjacent-edge');
    cy.nodes().not(hood.nodes()).addClass('faded');
    cy.edges().not(hood.edges()).addClass('faded');
  }

  cy.on('tap', 'node', (event) => {
    const node = event.target;
    highlightAdjacentTo(node);
    hooks.onSelect?.(node.id());
  });

  cy.on('tap', (event) => {
    if (event.target === cy) clearAdjacentHighlight();
  });

  return {
    loadGraph(graph, { refit = 'all' } = {}) {
      if (!graph?.nodes?.length) {
        cy.elements().remove();
        return;
      }
      const nodes = graph.nodes.map((node) => ({
        data: {
          id: node.id,
          label: node.displayName || node.id,
          agent: isAgentType(node.type),
        },
        classes: isAgentType(node.type) ? 'agent' : '',
      }));
      const edges = graph.edges.map((edge) => ({
        data: {
          id: edge.id || `${edge.source}->${edge.target}`,
          source: edge.source,
          target: edge.target,
          relationType: edge.relationType || '',
        },
      }));
      clearAdjacentHighlight();
      cy.batch(() => {
        cy.elements().remove();
        cy.add([...nodes, ...edges]);
      });
      const layout = cy.elements().layout(forceLayoutOptions(graph.nodes.length));
      layout.one('layoutstop', () => {
        applyZoomLabels();
        if (refit === 'none') return;
        cy.animate({ fit: { eles: undefined, padding: 40 }, duration: refit === 'all' ? 320 : 0 });
      });
      layout.run();
    },

    fitAll() {
      cy.animate({ fit: { eles: undefined, padding: 40 }, duration: 280 });
      applyZoomLabels();
    },

    focusNode(nodeId, { highlightNeighbors = true } = {}) {
      const node = cy.$id(nodeId);
      if (!node || node.empty()) return;
      cy.elements().unselect();
      node.select();
      if (highlightNeighbors) highlightAdjacentTo(node);
      else clearAdjacentHighlight();
      const scope = focusScope(node);
      cy.animate({
        fit: { eles: scope, padding: 100 },
        duration: 320,
      });
      applyZoomLabels();
    },

    selectNode(nodeId) {
      const node = cy.$id(nodeId);
      if (!node || node.empty()) return;
      cy.elements().unselect();
      node.select();
      highlightAdjacentTo(node);
    },

    destroy() {
      cy.destroy();
    },
  };
}
