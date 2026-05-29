const API_BASE = '/api/v1';

// ===== State =====
let projectsData = [];
let collectionsData = [];
let projectDocsState = { page: 0, size: 20, totalPages: 0, totalCount: 0 };
let collectionDocsState = { page: 0, size: 20, totalPages: 0, totalCount: 0 };
let tasksInterval = null;

// ===== API =====
async function apiCall(endpoint, method = 'GET', body = null) {
    const options = {
        method: method,
        headers: { 'Content-Type': 'application/json' }
    };
    if (body) options.body = JSON.stringify(body);

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, options);
        const contentType = response.headers.get('content-type');
        let data = null;

        if (response.status !== 204 && response.status !== 202) {
            if (contentType && contentType.includes('application/json')) {
                data = await response.json();
            } else {
                data = await response.text();
            }
        }

        if (!response.ok && response.status !== 202) {
            const msg = data && data.message ? data.message : response.statusText;
            throw new Error(`HTTP ${response.status}: ${msg}`);
        }

        return { status: response.status, data: data };
    } catch (error) {
        alert(`Error: ${error.message}`);
        throw error;
    }
}

// ===== Projects =====
async function loadProjects() {
    try {
        const result = await apiCall('/projects', 'GET');
        const prevSelected = document.getElementById('projectSelect').value;
        projectsData = (result.data && result.data.projects) ? result.data.projects : [];
        populateProjectSelect();
        // Restore selection
        if (prevSelected) {
            const stillExists = projectsData.find(p => String(p.id) === prevSelected);
            if (stillExists) {
                document.getElementById('projectSelect').value = prevSelected;
                updateProjectInfo(parseInt(prevSelected));
            } else {
                document.getElementById('projectSelect').value = '';
                hideProjectInfo();
                document.getElementById('projectDocumentsList').innerHTML = '';
                document.getElementById('projectDocsPagination').innerHTML = '';
            }
        }
    } catch (error) {
        console.error('Failed to load projects', error);
    }
}

function populateProjectSelect() {
    const select = document.getElementById('projectSelect');
    select.innerHTML = '<option value="">-- Select Project --</option>' +
        projectsData.map(p => `<option value="${p.id}">${escapeHtml(p.name)} (ID: ${p.id})</option>`).join('');
}

document.getElementById('projectSelect').addEventListener('change', (e) => {
    const projectId = e.target.value;
    if (projectId) {
        updateProjectInfo(parseInt(projectId));
        projectDocsState.page = 0;
        loadProjectDocuments(parseInt(projectId));
    } else {
        hideProjectInfo();
        document.getElementById('projectDocumentsList').innerHTML = '';
        document.getElementById('projectDocsPagination').innerHTML = '';
    }
});

function updateProjectInfo(projectId) {
    const p = projectsData.find(x => x.id === projectId);
    if (!p) return;
    document.getElementById('pi-id').textContent = p.id;
    document.getElementById('pi-docs').textContent = p.documentIds ? p.documentIds.length : 0;
    document.getElementById('pi-active').innerHTML = p.active
        ? '<span class="status-yes">Yes</span>'
        : '<span class="status-no">No</span>';
    document.getElementById('pi-synced').textContent = formatDate(p.syncedAt);
    document.getElementById('pi-indexed').textContent = formatDate(p.indexedAt);
    document.getElementById('projectInfo').style.display = 'block';
}

function hideProjectInfo() {
    document.getElementById('projectInfo').style.display = 'none';
}

document.getElementById('createProjectForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const project = {
        name: formData.get('name'),
        url: formData.get('url'),
        defaultBranch: formData.get('defaultBranch'),
        sourceType: formData.get('sourceType')
    };
    try {
        await apiCall('/projects', 'POST', project);
        e.target.reset();
        await loadProjects();
    } catch (error) {
        console.error(error);
    }
});

function getSelectedProjectId() {
    const val = document.getElementById('projectSelect').value;
    return val ? parseInt(val) : null;
}

function getUsername() {
    return document.getElementById('username').value;
}

function getPassword() {
    return document.getElementById('password').value;
}

async function syncProject() {
    const id = getSelectedProjectId();
    const username = getUsername();
    const password = getPassword();
    const credentials = {
        username: username,
        password: password
    };
    if (!id) { alert('Please select a project'); return; }
    try {
        let result;
        if (username) {
            result = await apiCall(`/projects/${id}/sync?username=${username}&password=${password}`, 'POST');
        } else {
            result = await apiCall(`/projects/${id}/sync`, 'POST');
        }

        if (result.status === 202) alert('Sync task queued');
        else if (result.status === 409) alert('Sync already in progress');
        loadTasks();
    } catch (error) { console.error(error); }
}

async function indexProject() {
    const id = getSelectedProjectId();
    if (!id) { alert('Please select a project'); return; }
    try {
        const result = await apiCall(`/projects/${id}/index`, 'POST');
        if (result.status === 202) alert('Index task queued');
        else if (result.status === 409) alert('Index already in progress');
        loadTasks();
    } catch (error) { console.error(error); }
}

async function activateProject() {
    const id = getSelectedProjectId();
    if (!id) { alert('Please select a project'); return; }
    try {
        await apiCall(`/projects/${id}/activate`, 'GET');
        alert('Project activated');
        await loadProjects(); // Reloads but keeps selection
    } catch (error) { console.error(error); }
}

async function deactivateProject() {
    const id = getSelectedProjectId();
    if (!id) { alert('Please select a project'); return; }
    try {
        await apiCall(`/projects/${id}/deactivate`, 'GET');
        alert('Project deactivated');
        await loadProjects(); // Reloads but keeps selection
    } catch (error) { console.error(error); }
}

async function deleteProject() {
    const id = getSelectedProjectId();
    if (!id) { alert('Please select a project'); return; }
    if (!confirm('Delete this project? This cannot be undone.')) return;
    try {
        await apiCall(`/projects/${id}`, 'DELETE');
        alert('Project deleted');
        document.getElementById('projectSelect').value = '';
        hideProjectInfo();
        document.getElementById('projectDocumentsList').innerHTML = '';
        document.getElementById('projectDocsPagination').innerHTML = '';
        await loadProjects();
    } catch (error) { console.error(error); }
}

// ===== Project Documents with Pagination =====
async function loadProjectDocuments(projectId) {
    if (!projectId) projectId = getSelectedProjectId();
    if (!projectId) return;

    try {
        const result = await apiCall(
            `/projects/${projectId}/documents?page=${projectDocsState.page}&size=${projectDocsState.size}`,
            'GET'
        );
        const docsList = document.getElementById('projectDocumentsList');

        if (result.data && result.data.documents && result.data.documents.length > 0) {
            docsList.innerHTML = result.data.documents.map(d =>
                `<div class="doc-item">
                    <div class="doc-info">
                        <div class="doc-name">ID: ${d.id} | ${escapeHtml(d.localPath)}</div>
                        <div class="doc-meta">Indexed: ${formatDate(d.indexedAt)}</div>
                    </div>
                    <button class="btn-arrow" onclick="addDocumentToCollection(${d.id})" title="Add to collection">→</button>
                </div>`
            ).join('');

            projectDocsState.totalCount = result.data.totalCount || result.data.count || result.data.documents.length;
            projectDocsState.totalPages = Math.max(1, Math.ceil(projectDocsState.totalCount / projectDocsState.size));
            renderProjectPagination();
        } else {
            docsList.innerHTML = '<div class="empty-state">No documents found</div>';
            projectDocsState.totalCount = 0;
            projectDocsState.totalPages = 0;
            renderProjectPagination();
        }
    } catch (error) {
        console.error(error);
    }
}

function renderProjectPagination() {
    const container = document.getElementById('projectDocsPagination');
    if (projectDocsState.totalPages <= 1) {
        container.innerHTML = '';
        return;
    }
    container.innerHTML = `
        <button onclick="changeProjectPage(-1)" ${projectDocsState.page === 0 ? 'disabled' : ''}>Prev</button>
        <span class="page-info">${projectDocsState.page + 1} / ${projectDocsState.totalPages}</span>
        <button onclick="changeProjectPage(1)" ${projectDocsState.page >= projectDocsState.totalPages - 1 ? 'disabled' : ''}>Next</button>
    `;
}

function changeProjectPage(delta) {
    const newPage = projectDocsState.page + delta;
    if (newPage >= 0 && newPage < projectDocsState.totalPages) {
        projectDocsState.page = newPage;
        loadProjectDocuments();
    }
}

// ===== Collections =====
async function loadCollections() {
    try {
        const result = await apiCall('/collections', 'GET');
        const prevSelected = document.getElementById('collectionSelect').value;
        collectionsData = (result.data && result.data.collections) ? result.data.collections : [];
        populateCollectionSelect();
        // Restore selection
        if (prevSelected) {
            const stillExists = collectionsData.find(c => String(c.id) === prevSelected);
            if (stillExists) {
                document.getElementById('collectionSelect').value = prevSelected;
                updateCollectionInfo(parseInt(prevSelected));
            } else {
                document.getElementById('collectionSelect').value = '';
                hideCollectionInfo();
                document.getElementById('collectionDocumentsList').innerHTML = '';
                document.getElementById('collectionDocsPagination').innerHTML = '';
            }
        }
    } catch (error) {
        console.error('Failed to load collections', error);
    }
}

function populateCollectionSelect() {
    const select = document.getElementById('collectionSelect');
    select.innerHTML = '<option value="">-- Select Collection --</option>' +
        collectionsData.map(c => `<option value="${c.id}">${escapeHtml(c.name)} (ID: ${c.id})</option>`).join('');
}

document.getElementById('collectionSelect').addEventListener('change', (e) => {
    const collectionId = e.target.value;
    if (collectionId) {
        updateCollectionInfo(parseInt(collectionId));
        collectionDocsState.page = 0;
        loadCollectionDocuments(parseInt(collectionId));
    } else {
        hideCollectionInfo();
        document.getElementById('collectionDocumentsList').innerHTML = '';
        document.getElementById('collectionDocsPagination').innerHTML = '';
    }
});

function updateCollectionInfo(collectionId) {
    const c = collectionsData.find(x => x.id === collectionId);
    if (!c) return;
    document.getElementById('ci-id').textContent = c.id;
    document.getElementById('ci-docs').textContent = c.documentIds ? c.documentIds.length : 0;
    document.getElementById('ci-active').innerHTML = c.active
        ? '<span class="status-yes">Yes</span>'
        : '<span class="status-no">No</span>';
    document.getElementById('ci-indexed').textContent = formatDate(c.indexedAt);
    document.getElementById('collectionInfo').style.display = 'block';
}

function hideCollectionInfo() {
    document.getElementById('collectionInfo').style.display = 'none';
}

document.getElementById('createCollectionForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    try {
        await apiCall('/collections', 'POST', { name: formData.get('name') });
        e.target.reset();
        await loadCollections();
    } catch (error) {
        console.error(error);
    }
});

function getSelectedCollectionId() {
    const val = document.getElementById('collectionSelect').value;
    return val ? parseInt(val) : null;
}

async function indexCollection() {
    const id = getSelectedCollectionId();
    if (!id) { alert('Please select a collection'); return; }
    try {
        const result = await apiCall(`/collections/${id}/index`, 'POST');
        if (result.status === 202) alert('Index task queued');
        else if (result.status === 409) alert('Index already in progress');
        loadTasks();
    } catch (error) { console.error(error); }
}

async function activateCollection() {
    const id = getSelectedCollectionId();
    if (!id) { alert('Please select a collection'); return; }
    try {
        await apiCall(`/collections/${id}/activate`, 'GET');
        alert('Collection activated');
        await loadCollections(); // Reloads but keeps selection
    } catch (error) { console.error(error); }
}

async function deactivateCollection() {
    const id = getSelectedCollectionId();
    if (!id) { alert('Please select a collection'); return; }
    try {
        await apiCall(`/collections/${id}/deactivate`, 'GET');
        alert('Collection deactivated');
        await loadCollections(); // Reloads but keeps selection
    } catch (error) { console.error(error); }
}

async function deleteCollection() {
    const id = getSelectedCollectionId();
    if (!id) { alert('Please select a collection'); return; }
    if (!confirm('Delete this collection? This cannot be undone.')) return;
    try {
        await apiCall(`/collections/${id}`, 'DELETE');
        alert('Collection deleted');
        document.getElementById('collectionSelect').value = '';
        hideCollectionInfo();
        document.getElementById('collectionDocumentsList').innerHTML = '';
        document.getElementById('collectionDocsPagination').innerHTML = '';
        await loadCollections();
    } catch (error) { console.error(error); }
}

// ===== Collection Documents with Pagination =====
async function loadCollectionDocuments(collectionId) {
    if (!collectionId) collectionId = getSelectedCollectionId();
    if (!collectionId) return;

    try {
        const result = await apiCall(
            `/collections/${collectionId}/documents?page=${collectionDocsState.page}&size=${collectionDocsState.size}`,
            'GET'
        );
        const docsList = document.getElementById('collectionDocumentsList');

        if (result.data && result.data.documents && result.data.documents.length > 0) {
            docsList.innerHTML = result.data.documents.map(d =>
                `<div class="doc-item">
                    <div class="doc-info">
                        <div class="doc-name">ID: ${d.id} | Project: ${d.projectId} | ${escapeHtml(d.localPath)}</div>
                        <div class="doc-meta">Indexed: ${formatDate(d.indexedAt)}</div>
                    </div>
                    <button class="btn-remove" onclick="removeDocumentFromCollection(${d.id})" title="Remove from collection">×</button>
                </div>`
            ).join('');

            collectionDocsState.totalCount = result.data.totalCount || result.data.count || result.data.documents.length;
            collectionDocsState.totalPages = Math.max(1, Math.ceil(collectionDocsState.totalCount / collectionDocsState.size));
            renderCollectionPagination();
        } else {
            docsList.innerHTML = '<div class="empty-state">No documents in collection</div>';
            collectionDocsState.totalCount = 0;
            collectionDocsState.totalPages = 0;
            renderCollectionPagination();
        }
    } catch (error) {
        console.error(error);
    }
}

function renderCollectionPagination() {
    const container = document.getElementById('collectionDocsPagination');
    if (collectionDocsState.totalPages <= 1) {
        container.innerHTML = '';
        return;
    }
    container.innerHTML = `
        <button onclick="changeCollectionPage(-1)" ${collectionDocsState.page === 0 ? 'disabled' : ''}>Prev</button>
        <span class="page-info">${collectionDocsState.page + 1} / ${collectionDocsState.totalPages}</span>
        <button onclick="changeCollectionPage(1)" ${collectionDocsState.page >= collectionDocsState.totalPages - 1 ? 'disabled' : ''}>Next</button>
    `;
}

function changeCollectionPage(delta) {
    const newPage = collectionDocsState.page + delta;
    if (newPage >= 0 && newPage < collectionDocsState.totalPages) {
        collectionDocsState.page = newPage;
        loadCollectionDocuments();
    }
}

async function addDocumentToCollection(documentId) {
    const collectionId = getSelectedCollectionId();
    if (!collectionId) {
        alert('Please select a collection first');
        return;
    }
    try {
        await apiCall(`/collections/${collectionId}/documents`, 'POST', { documentIds: [documentId] });
        alert('Document added to collection');
        if (getSelectedCollectionId() === collectionId) {
            loadCollectionDocuments(collectionId);
        }
        // Update collection info doc count
        await loadCollections();
    } catch (error) { console.error(error); }
}

async function removeDocumentFromCollection(documentId) {
    const collectionId = getSelectedCollectionId();
    if (!collectionId) { alert('No collection selected'); return; }
    if (!confirm('Remove this document from the collection?')) return;
    try {
        await apiCall(`/collections/${collectionId}/documents`, 'DELETE', { documentIds: [documentId] });
        alert('Document removed');
        loadCollectionDocuments(collectionId);
        await loadCollections();
    } catch (error) { console.error(error); }
}

// ===== Tasks =====
async function loadTasks() {
    try {
        const result = await apiCall('/tasks', 'GET');
        const tasksList = document.getElementById('tasksList');
        if (result.data && result.data.tasks && result.data.tasks.length > 0) {
            tasksList.innerHTML = result.data.tasks.map(t =>
                `<div class="task-item">
                    <span class="task-id">#${t.id}</span>
                    <span class="task-status ${t.status}">${t.status}</span>
                    <div class="task-time">${t.type} | ${formatDate(t.updatedAt)}</div>
                </div>`
            ).join('');
        } else {
            tasksList.innerHTML = '<div class="empty-state">No tasks</div>';
        }
    } catch (error) {
        console.error(error);
    }
}

function startTasksPolling() {
    if (tasksInterval) clearInterval(tasksInterval);
    tasksInterval = setInterval(loadTasks, 5000);
}

// ===== Retrieve =====
document.getElementById('retrieveForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const query = new FormData(e.target).get('query');
    try {
        const result = await apiCall('/retrieve', 'POST', { query: query });
        const resultsDiv = document.getElementById('retrieveResults');

        if (result.data && result.data.localPathChunks) {
            let html = '';
            for (const [localPath, chunks] of Object.entries(result.data.localPathChunks)) {
                html += `<pre>${escapeHtml(localPath)}</pre>`
                html += chunks.map(c =>
                    `<div class="chunk-item">
                        <pre>${escapeHtml(c.content)}</pre>
                    </div>`
                ).join('');
            }
            resultsDiv.innerHTML = html || '<div class="empty-state">No results</div>';
        } else {
            resultsDiv.innerHTML = '<div class="empty-state">No relevant chunks found</div>';
        }
    } catch (error) { console.error(error); }
});

// ===== Utils =====
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatDate(dateStr) {
    if (!dateStr) return 'Never';
    try {
        const d = new Date(dateStr);
        return d.toLocaleString('ru-RU', {
            year: 'numeric', month: '2-digit', day: '2-digit',
            hour: '2-digit', minute: '2-digit'
        });
    } catch {
        return dateStr;
    }
}

// ===== Init =====
loadProjects();
loadCollections();
loadTasks();
startTasksPolling();
