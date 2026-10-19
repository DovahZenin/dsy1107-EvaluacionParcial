import { useEffect, useState } from 'react';
import { login, logout, procesarRetorno, getTokens, decodificarJwt, limpiarSesion } from './auth.js';
import * as api from './api.js';

export default function App() {
  const [tokens, setTokens] = useState(getTokens());
  const [error, setError] = useState(null);
  const [errorApi, setErrorApi] = useState(null);
  
  // Estados para Solicitante
  const [misSolicitudes, setMisSolicitudes] = useState([]);
  const [tipo, setTipo] = useState('VACACIONES');
  const [descripcion, setDescripcion] = useState('');

  // Estados para Aprobador
  const [todasSolicitudes, setTodasSolicitudes] = useState([]);
  const [comentarios, setComentarios] = useState({});

  useEffect(() => {
    procesarRetorno()
      .then((nuevos) => nuevos && setTokens(nuevos))
      .catch((e) => setError(e.message));
  }, []);

  const accessClaims = decodificarJwt(tokens?.access_token);
  const scopeSet = new Set((accessClaims?.scope || '').split(' ').filter(Boolean));
  const grupos = new Set(accessClaims?.['cognito:groups'] || []);
  const idClaims = decodificarJwt(tokens?.id_token);
  const nombreUsuario = idClaims?.email || idClaims?.name || idClaims?.['cognito:username'] || accessClaims?.sub;
  
  const esSolicitante = scopeSet.has('solicitudes/write');
  const esAprobador = scopeSet.has('solicitudes/approve');
  const rol = esAprobador && esSolicitante
    ? 'Aprobador y Solicitante'
    : esAprobador
      ? 'Aprobador'
      : 'Solicitante';

  useEffect(() => {
    if (tokens) {
      if (esSolicitante) cargarMisSolicitudes();
      if (esAprobador) cargarTodasSolicitudes();
    }
  }, [tokens]);

  const manejarErrorApi = (res) => {
    if (res.status === 401) {
      limpiarSesion();
      setTokens(null);
      setError('Sesión expirada o inválida. Vuelve a iniciar sesión.');
    } else if (res.status === 403) {
      setErrorApi('No tienes permisos para esta operación.');
    } else if (res.status === 0) {
      setErrorApi('Sin conexión con la API: ' + (res.data?.message ?? ''));
    } else {
      setErrorApi('Error de la API (' + res.status + ').');
    }
  };

  const cargarMisSolicitudes = async () => {
    const res = await api.obtenerMisSolicitudes();
    if (res.ok) {
      setMisSolicitudes(res.data);
      setErrorApi(null);
    } else {
      manejarErrorApi(res);
    }
  };

  const cargarTodasSolicitudes = async () => {
    const res = await api.obtenerTodasSolicitudes();
    if (res.ok) {
      setTodasSolicitudes(res.data);
      setErrorApi(null);
    } else {
      manejarErrorApi(res);
    }
  };

  const handleCrear = async (e) => {
    e.preventDefault();
    const res = await api.crearSolicitud({ tipo, descripcion });
    if (res.ok) {
      setDescripcion('');
      cargarMisSolicitudes();
    } else {
      manejarErrorApi(res);
    }
  };

  const handleEvaluar = async (id, estado) => {
    const comentario = comentarios[id] || '';
    const res = await api.evaluarSolicitud(id, estado, comentario);
    if (res.ok) {
      cargarTodasSolicitudes();
    } else {
      manejarErrorApi(res);
    }
  };

  if (!tokens) {
    return (
      <main style={{ textAlign: 'center', marginTop: '5rem' }}>
        <h1>Sistema de Gestión de Solicitudes - Pedidos360</h1>
        {error && <p style={{ color: 'red' }}>{error}</p>}
        <button onClick={login} style={{ padding: '1rem 2rem', fontSize: '1rem', cursor: 'pointer' }}>
          Iniciar Sesión con Cognito
        </button>
      </main>
    );
  }

  if (!esSolicitante && !esAprobador) {
    return (
      <main style={{ textAlign: 'center', marginTop: '5rem' }}>
        <h2>Sin permisos asignados</h2>
        <p style={{ color: '#b3261e' }}>
          Tu usuario no tiene scopes de solicitudes. Pide a un administrador asignarte al grupo
          &quot;solicitantes&quot; o &quot;aprobadores&quot; y vuelve a iniciar sesión.
        </p>
        <button onClick={logout} style={{ background: '#b3261e', color: '#fff', border: 'none', padding: '0.75rem 1.5rem', borderRadius: '4px', cursor: 'pointer' }}>
          Cerrar Sesión
        </button>
      </main>
    );
  }

  return (
    <main style={{ maxWidth: '800px', margin: '0 auto', padding: '2rem' }}>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>Panel de Usuario ({nombreUsuario})</h2>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <span style={{ border: '1px solid #2e3448', borderRadius: '12px', padding: '0.25rem 0.75rem', fontSize: '0.85rem' }}>
            Rol: {rol}{grupos.size > 0 ? ` (${[...grupos].join(', ')})` : ''}
          </span>
          <button onClick={logout} style={{ background: '#b3261e', color: '#fff', border: 'none', padding: '0.5rem 1rem', borderRadius: '4px', cursor: 'pointer' }}>
            Cerrar Sesión
          </button>
        </div>
      </header>
      {errorApi && <p style={{ color: '#ff6b6b' }}>{errorApi}</p>}
      <hr style={{ margin: '1rem 0' }} />

      {/* VISTA DE SOLICITANTE */}
      {esSolicitante && (
        <section style={{ marginBottom: '2rem' }}>
          <h3>Crear Nueva Solicitud</h3>
          <form onSubmit={handleCrear} style={{ display: 'flex', flexDirection: 'column', gap: '1rem', background: '#1f2433', border: '1px solid #2e3448', padding: '1rem', borderRadius: '8px' }}>
            <div>
              <label>Tipo de Solicitud: </label>
              <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
                <option value="VACACIONES">Vacaciones</option>
                <option value="PERMISO_ADMINISTRATIVO">Permiso Administrativo</option>
                <option value="LICENCIA_MEDICA">Licencia Médica</option>
                <option value="TRABAJO_REMOTO">Trabajo Remoto</option>
              </select>
            </div>
            <div>
              <label>Descripción / Motivo: </label>
              <input type="text" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} required style={{ width: '100%', padding: '0.5rem' }} />
            </div>
            <button type="submit" style={{ background: '#2f5bea', color: '#fff', border: 'none', padding: '0.7rem', borderRadius: '4px', cursor: 'pointer' }}>
              Enviar Solicitud
            </button>
          </form>

          <h3>Mis Solicitudes Ingresadas</h3>
          <table border="1" cellPadding="8" style={{ width: '100%', borderCollapse: 'collapse', border: '1px solid #2e3448' }}>
            <thead style={{ background: '#1f2433' }}>
              <tr>
                <th>ID</th>
                <th>Tipo</th>
                <th>Descripción</th>
                <th>Estado</th>
                <th>Comentario Aprobador</th>
              </tr>
            </thead>
            <tbody>
              {misSolicitudes.map((s) => (
                <tr key={s.id}>
                  <td>{s.id}</td>
                  <td>{s.tipo}</td>
                  <td>{s.descripcion}</td>
                  <td><strong>{s.estado}</strong></td>
                  <td>{s.comentarioAprobador || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      {/* VISTA DE APROBADOR */}
      {esAprobador && (
        <section>
          <h3>Panel de Aprobación (Aprobador)</h3>
          <table border="1" cellPadding="8" style={{ width: '100%', borderCollapse: 'collapse', border: '1px solid #2e3448' }}>
            <thead style={{ background: '#1f2433' }}>
              <tr>
                <th>ID</th>
                <th>Usuario</th>
                <th>Tipo</th>
                <th>Descripción</th>
                <th>Estado Actual</th>
                <th>Comentario / Acción</th>
              </tr>
            </thead>
            <tbody>
              {todasSolicitudes.map((s) => (
                <tr key={s.id}>
                  <td>{s.id}</td>
                  <td>{s.usuarioId}</td>
                  <td>{s.tipo}</td>
                  <td>{s.descripcion}</td>
                  <td><strong>{s.estado}</strong></td>
                  <td>
                    <input
                      type="text"
                      placeholder="Comentario..."
                      value={comentarios[s.id] || ''}
                      onChange={(e) => setComentarios({ ...comentarios, [s.id]: e.target.value })}
                      style={{ display: 'block', marginBottom: '5px', width: '90%' }}
                    />
                    <button onClick={() => handleEvaluar(s.id, 'APROBADA')} style={{ background: '#1a7f45', color: '#fff', border: 'none', padding: '0.3rem 0.6rem', marginRight: '5px', cursor: 'pointer' }}>
                      Aprobar
                    </button>
                    <button onClick={() => handleEvaluar(s.id, 'RECHAZADA')} style={{ background: '#b3261e', color: '#fff', border: 'none', padding: '0.3rem 0.6rem', cursor: 'pointer' }}>
                      Rechazar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
    </main>
  );
}