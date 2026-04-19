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
            'ratings': 'Actividad y Calificaciones'
        };
        document.getElementById('page-title').textContent = titles[tabName];

        // Fetch Data
        if (tabName === 'users') this.loadUsers();
        if (tabName === 'routes') this.loadRoutes();
        if (tabName === 'ratings') this.loadRatings();
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

    async loadRoutes() {
        const routes = await this.fetchApi('/api/v1/admin/routes');
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

    async loadRatings() {
        const ratings = await this.fetchApi('/api/v1/admin/ratings');
        const tbody = document.getElementById('table-ratings');
        tbody.innerHTML = '';
        
        ratings.forEach(item => {
            let stars = '';
            for(let i=0; i<5; i++) {
                if(i < item.rating) stars += '<i class="fas fa-star text-yellow-400"></i>';
                else stars += '<i class="far fa-star text-gray-300"></i>';
            }

            const userName = item.user ? item.user.name : '<span class="text-gray-400">Anónimo</span>';
            const routeName = item.route ? item.route.name : 'Ruta Desconocida';
            const comment = item.comment || '<em class="text-gray-400">Sin comentarios</em>';
            
            tbody.innerHTML += `
                <tr class="hover:bg-gray-50 transition">
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${routeName}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${userName}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm">${stars}</td>
                    <td class="px-6 py-4 text-sm text-gray-500 max-w-xs truncate" title="${item.comment}">${comment}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${new Date(item.createdAt).toLocaleDateString()}</td>
                </tr>
            `;
        });
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

    async loadStopsForRoute(id) {
        const container = document.getElementById('stops-list');
        container.innerHTML = '<li class="py-4 flex text-center text-sm text-indigo-500">Cargando paraderos...</li>';
        
        try {
            const stops = await this.fetchApi(`/api/v1/admin/routes/${id}/stops`);
            if (stops.length === 0) {
                container.innerHTML = '<li class="py-4 flex text-center text-sm text-gray-500">Esta ruta no tiene paraderos asignados.</li>';
                return;
            }

            container.innerHTML = '';
            stops.forEach(routeStop => {
                const stop = routeStop.stop;
                if(!stop) return;
                container.innerHTML += `
                    <li class="py-3 flex justify-between items-center bg-white">
                        <div class="flex items-center">
                            <span class="bg-gray-100 text-gray-600 font-bold px-3 py-1 rounded w-8 text-center mr-3 text-xs">#${routeStop.stopOrder || '-'}</span>
                            <div>
                                <p class="text-sm font-medium text-gray-900">${stop.name}</p>
                                <p class="text-xs text-gray-500"><i class="fas fa-map-marker-alt"></i> Lat: ${stop.lat}, Lng: ${stop.lng}</p>
                            </div>
                        </div>
                    </li>
                `;
            });
        } catch(e) {
            container.innerHTML = '<li class="py-4 flex text-center text-sm text-red-500">Error al cargar paraderos.</li>';
        }
    }
};

document.addEventListener('DOMContentLoaded', () => app.init());
