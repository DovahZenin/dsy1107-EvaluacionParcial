import { useEffect, useState, useRef } from 'react';
import { CONFIG } from './config';
import { generateCodeVerifier, generateCodeChallenge } from './pkce';

export default function App() {
  const [tokens, setTokens] = useState(null);
  const [apiResponse, setApiResponse] = useState(null);
  const fetchedRef = useRef(false);

  useEffect(() => {
    // Si ya hay tokens en sesión previa, los cargamos
    const storedAccessToken = sessionStorage.getItem('access_token');
    if (storedAccessToken) {
      setTokens({ access_token: storedAccessToken });
    }

    const handleCallback = async () => {
      const urlParams = new URLSearchParams(window.location.search);
      const code = urlParams.get('code');
      const verifier = sessionStorage.getItem('code_verifier');

      if (!code || !verifier || fetchedRef.current) return;
      fetchedRef.current = true;

      const payload = new URLSearchParams({
        grant_type: 'authorization_code',
        client_id: CONFIG.clientId,
        code: code,
        redirect_uri: CONFIG.redirectUri,
        code_verifier: verifier
      });

      try {
        // Paso 3 del Diagrama: Canje de código por tokens
        const res = await fetch(`${CONFIG.cognitoDomain}/oauth2/token`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: payload
        });
        const data = await res.json();

        if (res.ok) {
          setTokens(data);
          // Guardar access_token para autorizar peticiones a la API
          sessionStorage.setItem('access_token', data.access_token);
          sessionStorage.removeItem('code_verifier');
          window.history.replaceState({}, document.title, "/");
        } else {
          console.error("Error devuelto por Cognito:", data);
        }
      } catch (err) {
        console.error("Error al obtener tokens:", err);
      }
    };

    handleCallback();
  }, []);

  // Paso 1 del Diagrama: GET /oauth2/authorize con PKCE
  const login = async () => {
    const verifier = generateCodeVerifier();
    const challenge = await generateCodeChallenge(verifier);
    sessionStorage.setItem('code_verifier', verifier);

    const loginUrl = `${CONFIG.cognitoDomain}/oauth2/authorize?` +
      `client_id=${CONFIG.clientId}&` +
      `response_type=code&` +
      `scope=${encodeURIComponent('openid email profile aws.cognito.signin.user.admin')}&` +
      `redirect_uri=${encodeURIComponent(CONFIG.redirectUri)}&` +
      `code_challenge=${challenge}&` +
      `code_challenge_method=S256`;

    window.location.href = loginUrl;
  };

  // Paso 11 del Diagrama: Cerrar sesión SSO en Cognito
  const logout = () => {
    sessionStorage.clear();
    const logoutUrl = `${CONFIG.cognitoDomain}/logout?` +
      `client_id=${CONFIG.clientId}&` +
      `logout_uri=${encodeURIComponent(CONFIG.redirectUri)}`;
    
    window.location.href = logoutUrl;
  };

  // Paso 6 del Diagrama: Petición con Authorization: Bearer <access_token>
  const fetchProtectedApi = async () => {
    const accessToken = sessionStorage.getItem('access_token');
    if (!accessToken) return alert("Inicia sesión primero");

    try {
      const res = await fetch(`${CONFIG.apiGatewayUrl}/datos`, {
        headers: { Authorization: `Bearer ${accessToken}` }
      });
      const data = await res.json();
      setApiResponse(data);
    } catch (err) {
      console.error("Error consumiendo la API:", err);
    }
  };

  return (
    <div style={{ padding: '2rem', fontFamily: 'sans-serif' }}>
      <h1>Cognito Integration Demo</h1>
      {!tokens ? (
        <button onClick={login}>Iniciar Sesión con Cognito</button>
      ) : (
        <div>
          <p>✅ ¡Autenticado con éxito!</p>
          <button onClick={fetchProtectedApi} style={{ marginRight: '10px' }}>
            Consumir API Protegida (/datos)
          </button>
          <button onClick={logout} style={{ background: '#ff4d4d', color: '#fff', border: 'none', padding: '0.4rem 0.8rem', cursor: 'pointer' }}>
            Cerrar Sesión
          </button>
        </div>
      )}
      {apiResponse && (
        <pre style={{ background: '#f4f4f4', padding: '1rem', marginTop: '1rem', borderRadius: '4px' }}>
          {JSON.stringify(apiResponse, null, 2)}
        </pre>
      )}
    </div>
  );
}