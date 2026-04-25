const app = {
    token: localStorage.getItem('adminToken'),
    
    init() {
        if (!this.token) {
            window.location.href = '/admin/login.html';
            return;
        }
        
        // Show default tab
        this.showTab('users');
    },

    logout() {
        localStorage.removeItem('adminToken');
        window.location.href = '/admin/login.html';
    },

    async fetchApi(endpoint, options = {}) {
        const fetchOptions = {
            ...options,
            headers: {
                'Authorization': `Bearer ${this.token}`,
                'Content-Type': 'application/json',
                ...options.headers
            }
        };

        const response = await fetch(endpoint, fetchOptions);
        
        if (response.status === 401 || response.status === 403) {
            this.logout();
        }

        if (response.status === 204) {
            return null;
        }

        if(!response.ok) {
           throw new Error('Error de API');
        }
        
        return await response.json();
    },

    showTab(tabName) {
        // Toggle active links
        document.querySelectorAll('.nav-link').forEach(el => el.classList.remove('active', 'bg-indigo-600', 'text-white'));
        const activeLink = document.getElementById(`tab-${tabName}`);
        activeLink.classList.remove('text-gray-600', 'hover:bg-indigo-50', 'hover:text-indigo-700');
        activeLink.classList.add('active', 'bg-indigo-600', 'text-white');

        // Toggle active views
        document.querySelectorAll('main > div > div[id^="view-"]').forEach(el => el.classList.add('hidden'));
        document.getElementById(`view-${tabName}`).classList.remove('hidden');

        // Set title
        const titles = {
            'users': 'Gestión de Usuarios Activos',
            'routes': 'Panel de Rutas de Transporte',
            'interactions': 'Interacciones de Usuarios',
            'optimization': 'Optimización Global de Datos'
        };
        document.getElementById('page-title').textContent = titles[tabName];

        // Fetch Data
        if (tabName === 'users') this.loadUsers();
        if (tabName === 'routes') this.loadRoutes();
        if (tabName === 'interactions') {
            if(!this.currentInteractionSubtab) this.currentInteractionSubtab = 'ratings';
            this.setInteractionSubtab(this.currentInteractionSubtab);
        }
        // Optimization doesn't need to fetch data on load, except maybe status if a task is running
    },

    async loadUsers() {
        const users = await this.fetchApi('/api/v1/admin/users');
        const tbody = document.getElementById('table-users');
        tbody.innerHTML = '';
        
        users.forEach(user => {
            const roleBadge = user.role === 'ADMIN' ? 
                '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-purple-100 text-purple-800">Admin</span>' : 
                '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">User</span>';
                
            const activeBadge = user.isActive ? 
                '<span class="text-green-500"><i class="fas fa-check-circle"></i> Activo</span>' : 
                '<span class="text-red-500"><i class="fas fa-times-circle"></i> Inactivo</span>';

            tbody.innerHTML += `
                <tr class="hover:bg-gray-50 transition">
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">#${user.id || '?'}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${user.name}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${user.email}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm">${roleBadge}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm">${activeBadge}</td>
                </tr>
            `;
        });
    },

    currentRouteFilter: 'all',
    currentSearchQuery: '',
    currentRoutePage: 0,
    totalRoutePages: 0,
    searchTimeout: null,

    onSearchInput() {
        if(this.searchTimeout) clearTimeout(this.searchTimeout);
        this.searchTimeout = setTimeout(() => {
            this.searchRoutes();
        }, 400); // 400ms debounce
    },

    searchRoutes() {
        this.currentSearchQuery = document.getElementById('route-search-input').value.trim();
        this.currentRoutePage = 0;
        this.loadRoutes();
    },

    setRouteFilter(filterType) {
        this.currentRouteFilter = filterType;
        this.currentSearchQuery = ''; // Reset search on filter change
        const searchInput = document.getElementById('route-search-input');
        if(searchInput) searchInput.value = '';
        
        this.currentRoutePage = 0; // reset to first page on filter change
        
        // Update UI buttons
        document.querySelectorAll('.route-filter').forEach(el => {
            el.classList.remove('bg-indigo-100', 'text-indigo-700');
            el.classList.add('text-gray-500');
        });
        const activeBtn = document.getElementById(`filter-${filterType}`);
        if(activeBtn) {
            activeBtn.classList.remove('text-gray-500', 'hover:text-gray-700');
            activeBtn.classList.add('bg-indigo-100', 'text-indigo-700');
        }
        
        this.loadRoutes();
    },

    prevPage() {
        if (this.currentRoutePage > 0) {
            this.currentRoutePage--;
            this.loadRoutes();
        }
    },

    nextPage() {
        if (this.currentRoutePage < this.totalRoutePages - 1) {
            this.currentRoutePage++;
            this.loadRoutes();
        }
    },

    async loadRoutes() {
        let endpoint = `/api/v1/admin/routes?page=${this.currentRoutePage}&size=12`;
        if (this.currentSearchQuery) {
            endpoint += `&search=${encodeURIComponent(this.currentSearchQuery)}`;
        } else if (this.currentRouteFilter !== 'all') {
            endpoint += `&filter=${this.currentRouteFilter}`;
        }
        
        const responseData = await this.fetchApi(endpoint);
        const routes = responseData.content || [];
        
        // Manejar estructura de paginación de Spring Boot (top-level vs nested 'page')
        const pageInfo = responseData.page || responseData;
        this.totalRoutePages = pageInfo.totalPages || 1;
        const totalElements = pageInfo.totalElements ?? routes.length ?? 0;

        // UI Updates for pagination
        document.getElementById('page-current').textContent = (this.currentRoutePage + 1);
        document.getElementById('page-total').textContent = this.totalRoutePages;
        document.getElementById('item-total').textContent = totalElements;
        document.getElementById('btn-prev-page').disabled = this.currentRoutePage === 0;
        document.getElementById('btn-next-page').disabled = this.currentRoutePage >= this.totalRoutePages - 1;

        const grid = document.getElementById('grid-routes');
        grid.innerHTML = '';
        window.currentRoutes = routes; // Store for edit reference
        
        routes.forEach(route => {
            const riskColor = route.riskLevel > 5 ? 'text-red-600' : 'text-green-600';
            const verified = route.isVerified ? 
                '<span class="text-blue-500" title="Verificada"><i class="fas fa-check-double"></i></span>' : '';

            grid.innerHTML += `
                <div class="bg-white rounded-xl border border-gray-200 shadow-sm hover:shadow-md hover:border-indigo-300 transition-all p-5 cursor-pointer" onclick="app.openRouteModal(${route.id})">
                    <div class="flex justify-between items-start mb-4">
                        <div>
                            <h3 class="text-lg font-bold text-gray-900">${route.name} ${verified}</h3>
                            <p class="text-sm text-gray-500">Red: ${route.network || 'Desconocida'}</p>
                        </div>
                        <span class="inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-white bg-indigo-500 rounded">
                            Ref: ${route.routeRef || 'N/A'}
                        </span>
                    </div>
                    <div class="mt-4 pt-4 border-t border-gray-100">
                        <div class="flex items-center justify-between">
                            <span class="text-sm font-medium text-gray-500">Nivel de Riesgo</span>
                            <span class="text-sm font-bold ${riskColor}">${route.riskLevel || '0.0'}/10</span>
                        </div>
                    </div>
                    <div class="mt-3 text-right">
                        <span class="text-xs text-indigo-600 font-semibold flex items-center justify-end"><i class="fas fa-edit mr-1"></i> Editar / Ver</span>
                    </div>
                </div>
            `;
        });
    },

    currentInteractionSubtab: null,

    setInteractionSubtab(tab) {
        this.currentInteractionSubtab = tab;
        document.querySelectorAll('.interaction-tab').forEach(el => {
            el.classList.remove('border-indigo-500', 'text-indigo-600');
            el.classList.add('border-transparent', 'text-gray-500');
        });
        const active = document.getElementById(`subtab-${tab}`);
        if(active) {
            active.classList.remove('border-transparent', 'text-gray-500');
            active.classList.add('border-indigo-500', 'text-indigo-600');
        }
        this.loadInteractions(tab);
    },

    async loadInteractions(tab) {
        const table = document.getElementById('table-interactions');
        table.innerHTML = '<div class="p-4 text-gray-500">Cargando...</div>';
        
        try {
            let html = '';
            if (tab === 'ratings') {
                const data = await this.fetchApi('/api/v1/admin/ratings');
                html += `<thead class="bg-gray-50"><tr><th class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Ruta / Empresa</th><th class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Usuario</th><th class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Rating</th><th class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Comentario</th><th class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Fecha</th></tr></thead><tbody class="bg-white divide-y divide-gray-200">`;
                data.forEach(item => {
                    const userName = item.user ? item.user.name : 'Anónimo';
                    const routeInfo = item.route ? `Ruta: ${item.route.routeRef || 'ND'} - Empresa: ${item.route.network || 'Desconocida'}` : 'Desconocida';
                    const comment = item.comment ? item.comment : '<em class="text-gray-400">Sin comentario</em>';
                    html += `<tr class="hover:bg-gray-50">
                        <td class="px-6 py-4 text-sm font-medium text-gray-900">${routeInfo}</td>
                        <td class="px-6 py-4 text-sm text-gray-500">${userName}</td>
                        <td class="px-6 py-4 text-sm text-yellow-500 font-bold">${item.rating} / 5 <i class="fas fa-star"></i></td>
                        <td class="px-6 py-4 text-sm text-gray-700 italic max-w-xs truncate" title="${comment}">${comment}</td>
                        <td class="px-6 py-4 text-sm text-gray-500">${new Date(item.createdAt).toLocaleDateString()}</td>
                    </tr>`;
                });
                html += `</tbody>`;
            }
            table.innerHTML = html;
        } catch (e) {
            table.innerHTML = `<div class="p-4 text-red-500">Error cargando datos: ${e.message}</div>`;
        }
    },

    // Modal Methods
    openRouteModal(id) {
        const route = window.currentRoutes.find(r => r.id === id);
        if(!route) return;

        document.getElementById('edit-route-id').value = route.id;
        document.getElementById('edit-route-name').value = route.name || '';
        document.getElementById('edit-route-ref').value = route.routeRef || '';
        document.getElementById('edit-route-network').value = route.network || '';
        document.getElementById('edit-route-risk').value = route.riskLevel || 0;
        document.getElementById('edit-route-verified').checked = route.isVerified || false;

        this.switchModalTab('form');
        document.getElementById('route-modal').classList.remove('hidden');
        
        // Also fetch stops dynamically
        this.loadStopsForRoute(id);
    },

    closeRouteModal() {
        document.getElementById('route-modal').classList.add('hidden');
    },

    switchModalTab(tab) {
        document.querySelectorAll('.modal-tab').forEach(el => {
            el.classList.remove('border-indigo-500', 'text-indigo-600');
            el.classList.add('border-transparent', 'text-gray-500');
        });
        
        const activeItem = document.getElementById(`mtab-${tab}`);
        activeItem.classList.remove('border-transparent', 'text-gray-500');
        activeItem.classList.add('border-indigo-500', 'text-indigo-600');

        document.getElementById('mcontent-form').classList.add('hidden');
        document.getElementById('mcontent-stops').classList.add('hidden');
        
        document.getElementById(`mcontent-${tab}`).classList.remove('hidden');

        // Wake up map when explicitly switching to stops tab
        if (tab === 'stops' && this.mapInstance) {
            setTimeout(() => this.mapInstance.invalidateSize(), 150);
        }
    },

    async saveRoute() {
        const id = document.getElementById('edit-route-id').value;
        const payload = {
            name: document.getElementById('edit-route-name').value,
            routeRef: document.getElementById('edit-route-ref').value,
            network: document.getElementById('edit-route-network').value,
            riskLevel: parseFloat(document.getElementById('edit-route-risk').value),
            isVerified: document.getElementById('edit-route-verified').checked
        };

        const btn = document.getElementById('btn-save-route');
        btn.textContent = 'Guardando...';
        btn.disabled = true;

        try {
            await this.fetchApi(`/api/v1/admin/routes/${id}`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });
            this.closeRouteModal();
            this.loadRoutes(); // refresh UI
        } catch(e) {
            alert('Error al guardar la ruta: ' + e.message);
        } finally {
            btn.textContent = 'Guardar Cambios';
            btn.disabled = false;
        }
    },

    // Leaflet Map state
    mapInstance: null,
    mapMarkers: [],
    mapPolyline: null,

    renderMap(stops) {
        if (!this.mapInstance) {
            this.mapInstance = L.map('route-map').setView([-12.046374, -77.042793], 11);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© OpenStreetMap'
            }).addTo(this.mapInstance);
        }

        // Clear existing markers and lines
        this.mapMarkers.forEach(m => this.mapInstance.removeLayer(m));
        if (this.mapPolyline) this.mapInstance.removeLayer(this.mapPolyline);
        this.mapMarkers = [];

        const coordinates = [];
        
        stops.forEach(routeStop => {
            const stop = routeStop.stop;
            if (!stop || !stop.latitude || !stop.longitude) return;
            
            const latLng = [stop.latitude, stop.longitude];
            coordinates.push(latLng);
            
            const marker = L.circleMarker(latLng, {radius: 6, color: 'indigo'})
                .bindPopup(`<b>#${routeStop.stopOrder}</b>: ${stop.name}`)
                .addTo(this.mapInstance);
            this.mapMarkers.push(marker);
        });

        if (coordinates.length > 0) {
            // Draw sequenced circuit polyline
            this.mapPolyline = L.polyline(coordinates, {color: '#4f46e5', weight: 4, opacity: 0.7}).addTo(this.mapInstance);
            this.mapInstance.fitBounds(this.mapPolyline.getBounds(), {padding: [20, 20]});
        }
    },

    async loadStopsForRoute(id) {
        const container = document.getElementById('stops-list');
        container.innerHTML = '<li class="py-4 flex text-center text-sm text-indigo-500">Cargando paraderos y redibujando mapa...</li>';
        
        try {
            const stops = await this.fetchApi(`/api/v1/admin/routes/${id}/stops`);
            
            this.renderMap(stops);

            if (stops.length === 0) {
                container.innerHTML = '<li class="py-4 flex text-center text-sm text-gray-500">Esta ruta no tiene paraderos asignados.</li>';
                return;
            }

            container.innerHTML = '';
            let htmlContent = '';
            stops.forEach(routeStop => {
                const stop = routeStop.stop;
                if(!stop) return;
                
                // Color code markers based on IDA or VUELTA for leafet map if present
                if (routeStop.direction === 'VUELTA') {
                    const markerVuelta = L.circleMarker([stop.latitude, stop.longitude], {radius: 6, color: 'orange'})
                        .bindPopup(`<b>#${routeStop.stopOrder} (Vuelta)</b>: ${stop.name}`)
                        .addTo(this.mapInstance);
                    this.mapMarkers.push(markerVuelta);
                }

                const dirSelection = routeStop.direction === 'VUELTA' ? 
                    `<option value="IDA">IDA</option><option value="VUELTA" selected>VUELTA</option>` : 
                    `<option value="IDA" selected>IDA</option><option value="VUELTA">VUELTA</option>`;

                // Construct embedded editing UI
                htmlContent += `
                    <li class="py-4 grid gap-2 bg-white border-b border-gray-100 last:border-0 pl-1">
                        <div class="flex items-center justify-between">
                            <div class="flex items-center space-x-2 flex-1">
                                <input type="number" id="rs-order-${routeStop.id}" value="${routeStop.stopOrder || ''}" class="bg-indigo-50 text-indigo-700 font-bold px-1 py-1 rounded text-xs w-12 text-center border-gray-200">
                                <input type="text" id="stop-name-${stop.id}" value="${stop.name}" class="text-sm font-medium text-gray-900 border border-transparent hover:border-gray-300 focus:border-indigo-500 rounded px-1 py-0.5 w-full flex-1">
                                <select id="rs-dir-${routeStop.id}" class="text-xs border-gray-300 rounded px-1 py-0.5 w-24">${dirSelection}</select>
                            </div>
                            <button onclick="app.deleteRouteStop(${routeStop.id}, ${id})" class="ml-2 text-red-500 hover:text-red-700 bg-red-50 p-1.5 rounded" title="Borrar del Circuito">
                                <i class="fas fa-trash-alt"></i>
                            </button>
                        </div>
                        <div class="flex items-center space-x-2 text-xs">
                            <span class="text-gray-500 font-medium">Lat:</span>
                            <input type="number" step="0.000001" id="stop-lat-${stop.id}" value="${stop.latitude}" class="border border-gray-300 rounded px-1 py-0.5 w-24">
                            <span class="text-gray-500 font-medium ml-2">Lng:</span>
                            <input type="number" step="0.000001" id="stop-lng-${stop.id}" value="${stop.longitude}" class="border border-gray-300 rounded px-1 py-0.5 w-24">
                            
                            <button onclick="app.updateStopReal(${stop.id}, ${routeStop.id}, ${id})" class="ml-auto bg-indigo-50 text-indigo-600 hover:bg-indigo-100 px-2 py-1 rounded font-medium transition cursor-pointer">
                                Guardar
                            </button>
                        </div>
                    </li>
                `;
            });
            container.innerHTML = htmlContent;
        } catch(e) {
            container.innerHTML = '<li class="py-4 flex text-center text-sm text-red-500">Error al cargar paraderos.</li>';
        }
    },

    async createNewStop() {
        const routeId = document.getElementById('edit-route-id').value;
        const name = document.getElementById('new-stop-name').value;
        const order = parseInt(document.getElementById('new-stop-order').value) || 0;
        const lat = parseFloat(document.getElementById('new-stop-lat').value);
        const lng = parseFloat(document.getElementById('new-stop-lng').value);
        const dir = document.getElementById('new-stop-direction').value;

        if (!name || isNaN(lat) || isNaN(lng)) return alert("Rellena el nombre, latitud y longitud correctamente.");

        try {
            await this.fetchApi(`/api/v1/admin/routes/${routeId}/stops`, {
                method: 'POST',
                body: JSON.stringify({ name: name, latitude: lat, longitude: lng, stopOrder: order, direction: dir })
            });
            // Clear inputs
            document.getElementById('new-stop-name').value = '';
            document.getElementById('new-stop-order').value = '';
            
            this.loadStopsForRoute(routeId);
        } catch(e) {
            alert('Error creando paradero: ' + e.message);
        }
    },

    async updateStopReal(stopId, routeStopId, routeId) {
        const name = document.getElementById(`stop-name-${stopId}`).value;
        const lat = parseFloat(document.getElementById(`stop-lat-${stopId}`).value);
        const lng = parseFloat(document.getElementById(`stop-lng-${stopId}`).value);
        const order = parseInt(document.getElementById(`rs-order-${routeStopId}`).value) || 0;
        const dir = document.getElementById(`rs-dir-${routeStopId}`).value;
        
        try {
            // Update stop physical data
            await this.fetchApi(`/api/v1/admin/stops/${stopId}`, {
                method: 'PUT',
                body: JSON.stringify({ name: name, latitude: lat, longitude: lng })
            });

            // Update routestop relation
            await this.fetchApi(`/api/v1/admin/route-stops/${routeStopId}`, {
                method: 'PUT',
                body: JSON.stringify({ stopOrder: order, direction: dir })
            });

            this.loadStopsForRoute(routeId);
        } catch(e) {
            alert('Error al actualizar el paradero: ' + e.message);
        }
    },

    async deleteRouteStop(routeStopId, routeId) {
        if(!confirm("¿Deseas borrar esta parada del circuito? Se rearmará el trazado en el mapa.")) return;
        try {
            await this.fetchApi(`/api/v1/admin/route-stops/${routeStopId}`, {
                method: 'DELETE'
            });
            this.loadStopsForRoute(routeId);
        } catch(e) {
            alert('Error al borrar: ' + e.message);
        }
    },

    // --- Optimization Logic ---
    optimizationInterval: null,

    async runOptimization(taskType) {
        if(!confirm(`¿Estás seguro de ejecutar el proceso: ${taskType}? Esto puede tardar varios minutos.`)) return;

        try {
            await this.fetchApi(`/api/v1/admin/optimize/run-all/${taskType}`, { method: 'POST' });
            
            // Iniciar Polling
            if(this.optimizationInterval) clearInterval(this.optimizationInterval);
            
            this.optimizationInterval = setInterval(() => {
                this.checkOptimizationStatus(`global_${taskType.replace('-', '_')}`);
            }, 1000);

            document.getElementById('opt-progress-text').textContent = 'Iniciando proceso...';
            document.getElementById('opt-progress-bar').style.width = '0%';
        } catch(e) {
            alert('Error al iniciar optimización: ' + e.message);
        }
    },

    async checkOptimizationStatus(taskId) {
        try {
            const statusInfo = await this.fetchApi(`/api/v1/admin/optimize/status/${taskId}`);
            const bar = document.getElementById('opt-progress-bar');
            const text = document.getElementById('opt-progress-text');

            if (statusInfo.status === 'NOT_STARTED') return;

            let percentage = 0;
            if (statusInfo.total > 0) {
                percentage = Math.round((statusInfo.current / statusInfo.total) * 100);
            }

            bar.style.width = `${percentage}%`;
            text.textContent = `[${percentage}%] ${statusInfo.message} (${statusInfo.current} de ${statusInfo.total})`;

            if (statusInfo.status === 'COMPLETED' || statusInfo.status === 'ERROR') {
                clearInterval(this.optimizationInterval);
                text.textContent += ' - ¡Proceso Finalizado!';
                if(statusInfo.status === 'ERROR') bar.classList.replace('bg-indigo-600', 'bg-red-500');
                else bar.classList.replace('bg-indigo-600', 'bg-green-500');
                
                setTimeout(() => {
                    bar.style.width = '0%';
                    bar.classList.remove('bg-green-500', 'bg-red-500');
                    bar.classList.add('bg-indigo-600');
                    text.textContent = 'Esperando iniciar nuevo proceso...';
                }, 5000);
            }
        } catch(e) {
            console.error('Error polling status:', e);
            clearInterval(this.optimizationInterval);
        }
    }
};

document.addEventListener('DOMContentLoaded', () => app.init());
