import { getAccessToken } from './auth.js';
import { CONFIG } from './config.js';

const API_URL = CONFIG.apiGatewayUrl;

async function peticion(endpoint, options = {}) {
  const token = getAccessToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  try {
    const response = await fetch(`${API_URL}${endpoint}`, {
      ...options,
      headers,
    });

    if (response.status === 204) return { ok: true, status: 204 };
    
    const data = await response.json().catch(() => ({}));
    return { ok: response.ok, status: response.status, data };
  } catch (error) {
    return { ok: false, status: 0, data: { message: error.message } };
  }
}

// Operaciones del Solicitante
export const obtenerMisSolicitudes = () => peticion('/api/solicitudes/mis-solicitudes');

export const crearSolicitud = (solicitud) => peticion('/api/solicitudes', {
  method: 'POST',
  body: JSON.stringify(solicitud),
});

export const eliminarSolicitud = (id) => peticion(`/api/solicitudes/${id}`, {
  method: 'DELETE',
});

// Operaciones del Aprobador
export const obtenerTodasSolicitudes = () => peticion('/api/solicitudes/todas');

export const evaluarSolicitud = (id, estado, comentario) => peticion(`/api/solicitudes/${id}/evaluar`, {
  method: 'PATCH',
  body: JSON.stringify({ estado, comentario }),
});