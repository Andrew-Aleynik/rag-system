const API_BASE = '/api/v1';

async function apiCall(endpoint, method = 'GET', body = null) {
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
        }
    };

    if (body) {
        options.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, options);
        const contentType = response.headers.get('content-type');
        let data;

        if (response.status === 204 || response.status === 202) {
            data = null;
        } else if (contentType && contentType.includes('application/json')) {
            data = await response.json();
        } else {
            data = await response.text();
        }

        displayResponse(response.status, data);

        if (!response.ok && response.status !== 202) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        return {status: response.status, data: data};
    } catch (error) {
        displayResponse(0, {error: error.message});
        throw error;
    }
}

function displayResponse(status, data) {
    const responseArea = document.getElementById('responseArea');
    const dataStr = data !== null ? JSON.stringify(data, null, 2) : '(no content)';
    responseArea.innerHTML = `Status: ${status}\n\nResponse:\n${dataStr}`;
}

function showResult(message, isError = false) {
    const responseArea = document.getElementById('responseArea');
    responseArea.innerHTML = `[${isError ? 'ERROR' : 'SUCCESS'}] ${message}\n\n${responseArea.innerHTML}`;
}

// Projects
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
        showResult('Project created successfully!');
        e.target.reset();
        loadProjects();
    } catch (error) {
        showResult(`Failed to create project: ${error.message}`, true);
    }
});

document.getElementById('updateProjectForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const id = formData.get('id');
    const project = {};
    const name = formData.get('name');
    const defaultBranch = formData.get('defaultBranch');
    if (name) project.name = name;
    if (defaultBranch) project.defaultBranch = defaultBranch;

    if (Object.keys(project).length === 0) {
        showResult('Please provide at least one field to update', true);
        return;
    }

    try {
        await apiCall(`/projects/${id}`, 'PUT', project);
        showResult('Project updated successfully!');
        e.target.reset();
        loadProjects();
    } catch (error) {
        showResult(`Failed to update project: ${error.message}`, true);
    }
});

document.getElementById('getProjectForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const projectId = formData.get('projectId');
    try {
        await apiCall(`/projects/${projectId}`, 'GET');
    } catch (error) {
        showResult(`Failed to get project: ${error.message}`, true);
    }
});

async function loadProjects() {
    try {
        const result = await apiCall('/projects', 'GET');
        const projectsList = document.getElementById('projectsList');
        if (result.data && result.data.projects && result.data.projects.length > 0) {
            projectsList.innerHTML = '<ul>' + result.data.projects.map(p =>
                `<li>ID: ${p.id} - ${p.name} (${p.type}) | Active: ${p.active} | Synced: ${p.syncedAt || 'Never'} | Indexed: ${p.indexedAt || 'Never'}</li>`
            ).join('') + '</ul>';
        } else {
            projectsList.innerHTML = '<p>No projects found</p>';
        }
    } catch (error) {
        showResult(`Failed to load projects: ${error.message}`, true);
    }
}

async function syncProject() {
    const projectId = document.querySelector('#projectActionForm [name="projectId"]').value;
    if (!projectId) {
        showResult('Please enter a project ID', true);
        return;
    }
    try {
        const result = await apiCall(`/projects/${projectId}/sync`, 'POST');
        if (result.status === 202) {
            showResult('Sync task queued successfully');
        } else if (result.status === 409) {
            showResult('Sync already in progress', true);
        }
        loadTasks();
    } catch (error) {
        showResult(`Failed to sync project: ${error.message}`, true);
    }
}

async function indexProject() {
    const projectId = document.querySelector('#projectActionForm [name="projectId"]').value;
    if (!projectId) {
        showResult('Please enter a project ID', true);
        return;
    }
    try {
        const result = await apiCall(`/projects/${projectId}/index`, 'POST');
        if (result.status === 202) {
            showResult('Index task queued successfully');
        } else if (result.status === 409) {
            showResult('Index already in progress', true);
        }
        loadTasks();
    } catch (error) {
        showResult(`Failed to index project: ${error.message}`, true);
    }
}

async function activateProject() {
    const projectId = document.querySelector('#projectActionForm [name="projectId"]').value;
    if (!projectId) {
        showResult('Please enter a project ID', true);
        return;
    }
    try {
        await apiCall(`/projects/${projectId}/activate`, 'GET');
        showResult('Project activated successfully');
        loadProjects();
    } catch (error) {
        showResult(`Failed to activate project: ${error.message}`, true);
    }
}

async function deactivateProject() {
    const projectId = document.querySelector('#projectActionForm [name="projectId"]').value;
    if (!projectId) {
        showResult('Please enter a project ID', true);
        return;
    }
    try {
        await apiCall(`/projects/${projectId}/deactivate`, 'GET');
        showResult('Project deactivated successfully');
        loadProjects();
    } catch (error) {
        showResult(`Failed to deactivate project: ${error.message}`, true);
    }
}

async function deleteProject() {
    const projectId = document.querySelector('#projectActionForm [name="projectId"]').value;
    if (!projectId) {
        showResult('Please enter a project ID', true);
        return;
    }
    if (!confirm('Delete this project? This action cannot be undone.')) return;
    try {
        await apiCall(`/projects/${projectId}`, 'DELETE');
        showResult('Project deleted successfully');
        loadProjects();
    } catch (error) {
        showResult(`Failed to delete project: ${error.message}`, true);
    }
}

async function getProjectDocuments() {
    const projectId = document.querySelector('#projectDocumentsForm [name="projectId"]').value;
    if (!projectId) {
        showResult('Please enter a project ID', true);
        return;
    }
    try {
        const result = await apiCall(`/projects/${projectId}/documents`, 'GET');
        const docsList = document.getElementById('projectDocumentsList');
        if (result.data && result.data.documents && result.data.documents.length > 0) {
            docsList.innerHTML = '<ul>' + result.data.documents.map(d =>
                `<li>ID: ${d.id} | ${d.fileName} | Path: ${d.localPath} | Hash: ${d.fileHash}</li>`
            ).join('') + '</ul>';
        } else {
            docsList.innerHTML = '<p>No documents found in collection</p>';
        }
    } catch (error) {
        showResult(`Failed to get project documents: ${error.message}`, true);
    }
}

// Collections
document.getElementById('createCollectionForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const collection = {
        name: formData.get('name')
    };
    try {
        await apiCall('/collections', 'POST', collection);
        showResult('Collection created successfully!');
        e.target.reset();
        loadCollections();
    } catch (error) {
        showResult(`Failed to create collection: ${error.message}`, true);
    }
});

document.getElementById('updateCollectionForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const id = formData.get('id');
    const name = formData.get('name');

    if (!name) {
        showResult('Please provide a name to update', true);
        return;
    }

    try {
        await apiCall(`/collections/${id}`, 'PUT', {name: name});
        showResult('Collection updated successfully!');
        e.target.reset();
        loadCollections();
    } catch (error) {
        showResult(`Failed to update collection: ${error.message}`, true);
    }
});

document.getElementById('getCollectionForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const collectionId = formData.get('collectionId');
    try {
        await apiCall(`/collections/${collectionId}`, 'GET');
    } catch (error) {
        showResult(`Failed to get collection: ${error.message}`, true);
    }
});

async function loadCollections() {
    try {
        const result = await apiCall('/collections', 'GET');
        const collectionsList = document.getElementById('collectionsList');
        if (result.data && result.data.collections && result.data.collections.length > 0) {
            collectionsList.innerHTML = '<ul>' + result.data.collections.map(c =>
                `<li>ID: ${c.id} - ${c.name} | Active: ${c.active} | Indexed: ${c.indexedAt || 'Never'}</li>`
            ).join('') + '</ul>';
        } else {
            collectionsList.innerHTML = '<p>No collections found</p>';
        }
    } catch (error) {
        showResult(`Failed to load collections: ${error.message}`, true);
    }
}

async function indexCollection() {
    const collectionId = document.querySelector('#collectionActionForm [name="collectionId"]').value;
    if (!collectionId) {
        showResult('Please enter a collection ID', true);
        return;
    }
    try {
        const result = await apiCall(`/collections/${collectionId}/index`, 'POST');
        if (result.status === 202) {
            showResult('Index task queued successfully');
        } else if (result.status === 409) {
            showResult('Index already in progress', true);
        }
        loadTasks();
    } catch (error) {
        showResult(`Failed to index collection: ${error.message}`, true);
    }
}

async function activateCollection() {
    const collectionId = document.querySelector('#collectionActionForm [name="collectionId"]').value;
    if (!collectionId) {
        showResult('Please enter a collection ID', true);
        return;
    }
    try {
        await apiCall(`/collections/${collectionId}/activate`, 'GET');
        showResult('Collection activated successfully');
        loadCollections();
    } catch (error) {
        showResult(`Failed to activate collection: ${error.message}`, true);
    }
}

async function deactivateCollection() {
    const collectionId = document.querySelector('#collectionActionForm [name="collectionId"]').value;
    if (!collectionId) {
        showResult('Please enter a collection ID', true);
        return;
    }
    try {
        await apiCall(`/collections/${collectionId}/deactivate`, 'GET');
        showResult('Collection deactivated successfully');
        loadCollections();
    } catch (error) {
        showResult(`Failed to deactivate collection: ${error.message}`, true);
    }
}

async function deleteCollection() {
    const collectionId = document.querySelector('#collectionActionForm [name="collectionId"]').value;
    if (!collectionId) {
        showResult('Please enter a collection ID', true);
        return;
    }
    if (!confirm('Delete this collection? This action cannot be undone.')) return;
    try {
        await apiCall(`/collections/${collectionId}`, 'DELETE');
        showResult('Collection deleted successfully');
        loadCollections();
    } catch (error) {
        showResult(`Failed to delete collection: ${error.message}`, true);
    }
}

async function getCollectionDocuments() {
    const collectionId = document.querySelector('#collectionDocumentsForm [name="collectionId"]').value;
    if (!collectionId) {
        showResult('Please enter a collection ID', true);
        return;
    }
    try {
        const result = await apiCall(`/collections/${collectionId}/documents`, 'GET');
        const docsList = document.getElementById('collectionDocumentsList');
        if (result.data && result.data.documents && result.data.documents.length > 0) {
            docsList.innerHTML = '<ul>' + result.data.documents.map(d =>
                `<li>ID: ${d.id} | ${d.fileName} | Path: ${d.localPath} | Hash: ${d.fileHash}</li>`
            ).join('') + '</ul>';
        } else {
            docsList.innerHTML = '<p>No documents found in collection</p>';
        }
    } catch (error) {
        showResult(`Failed to get collection documents: ${error.message}`, true);
    }
}

async function addDocumentsToCollection() {
    const collectionId = document.querySelector('#manageCollectionDocumentsForm [name="collectionId"]').value;
    const idsStr = document.querySelector('#manageCollectionDocumentsForm [name="documentIds"]').value;
    if (!collectionId || !idsStr) {
        showResult('Please enter both collection ID and document IDs', true);
        return;
    }
    const documentIds = idsStr.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
    if (documentIds.length === 0) {
        showResult('Please provide valid document IDs', true);
        return;
    }
    try {
        await apiCall(`/collections/${collectionId}/documents`, 'POST', {documentIds: documentIds});
        showResult('Documents added to collection successfully');
        getCollectionDocuments();
    } catch (error) {
        showResult(`Failed to add documents: ${error.message}`, true);
    }
}

async function removeDocumentsFromCollection() {
    const collectionId = document.querySelector('#manageCollectionDocumentsForm [name="collectionId"]').value;
    const idsStr = document.querySelector('#manageCollectionDocumentsForm [name="documentIds"]').value;
    if (!collectionId || !idsStr) {
        showResult('Please enter both collection ID and document IDs', true);
        return;
    }
    const documentIds = idsStr.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
    if (documentIds.length === 0) {
        showResult('Please provide valid document IDs', true);
        return;
    }
    if (!confirm('Remove these documents from the collection?')) return;
    try {
        await apiCall(`/collections/${collectionId}/documents`, 'DELETE', {documentIds: documentIds});
        showResult('Documents removed from collection successfully');
        getCollectionDocuments();
    } catch (error) {
        showResult(`Failed to remove documents: ${error.message}`, true);
    }
}

// Tasks
async function loadTasks() {
    try {
        const result = await apiCall('/tasks', 'GET');
        const tasksList = document.getElementById('tasksList');
        if (result.data && result.data.tasks && result.data.tasks.length > 0) {
            tasksList.innerHTML = '<ul>' + result.data.tasks.map(t =>
                `<li>ID: ${t.id} | Type: ${t.type} | Status: ${t.status} | Updated: ${t.updatedAt}</li>`
            ).join('') + '</ul>';
        } else {
            tasksList.innerHTML = '<p>No tasks found</p>';
        }
    } catch (error) {
        showResult(`Failed to load tasks: ${error.message}`, true);
    }
}

// Retrieve
document.getElementById('retrieveForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const query = formData.get('query');

    try {
        const result = await apiCall('/retrieve', 'POST', {query: query});
        const resultsDiv = document.getElementById('retrieveResults');

        if (result.data && result.data.chunks && result.data.chunks.length > 0) {
            resultsDiv.innerHTML = '<h4>Results:</h4><ul>' + result.data.chunks.map(c =>
                `<li><b>Doc ${c.documentId}, Chunk ${c.index}</b> | Tokens: ${c.sizeTokens} | Structural: ${c.structural}<br><pre>${c.content}</pre></li>`
            ).join('') + '</ul>';
        } else {
            resultsDiv.innerHTML = '<p>No relevant chunks found</p>';
        }
    } catch (error) {
        showResult(`Failed to retrieve: ${error.message}`, true);
    }
});

// Load initial data
loadProjects();
loadCollections();
loadTasks();
