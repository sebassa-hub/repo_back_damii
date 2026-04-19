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

    async fetchApi(endpoint) {
        const response = await fetch(endpoint, {
            headers: {
                'Authorization': `Bearer ${this.token}`,
                'Content-Type': 'application/json'
            }
        });
        
        if (response.status === 401 || response.status === 403) {
            this.logout();
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
        document.querySelectorAll('main > div > div').forEach(el => el.classList.add('hidden'));
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
        
        routes.forEach(route => {
            const riskColor = route.riskLevel > 5 ? 'text-red-600' : 'text-green-600';
            const verified = route.isVerified ? 
                '<span class="text-blue-500" title="Verificada"><i class="fas fa-check-double"></i></span>' : '';

            grid.innerHTML += `
                <div class="bg-white rounded-xl border border-gray-200 shadow-sm hover:shadow-md transition-all p-5">
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
    }
};

document.addEventListener('DOMContentLoaded', () => app.init());
