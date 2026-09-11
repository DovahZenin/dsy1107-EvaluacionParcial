import { CONFIG } from './config.js';

const scopes = 'openid email profile solicitudes/read solicitudes/write solicitudes/approve';

const CLAVE_VERIFIER = 'dsy1107.pkce_verifier';
const CLAVE_STATE = 'dsy1107.state';
const CLAVE_TOKENS = 'dsy1107.tokens';

function base64Url(buffer) {
  return btoa(String.fromCharCode(...new Uint8Array(buffer)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

function aleatorioBase64Url(bytes = 32) {
  const buffer = new Uint8Array(bytes);
  crypto.getRandomValues(buffer);
  return base64Url(buffer);
}

async function calcularChallenge(verifier) {
  const hash = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
  return base64Url(hash);
}

export async function login() {
  const verifier = aleatorioBase64Url();
  const challenge = await calcularChallenge(verifier);
  const state = aleatorioBase64Url(16);

  sessionStorage.setItem(CLAVE_VERIFIER, verifier);
  sessionStorage.setItem(CLAVE_STATE, state);

  const params = new URLSearchParams({
    response_type: 'code',
    client_id: CONFIG.clientId,
    redirect_uri: CONFIG.redirectUri,
    scope: scopes,
    state,
    code_challenge: challenge,
    code_challenge_method: 'S256',
  });

  window.location.assign(`${CONFIG.cognitoDomain}/oauth2/authorize?${params}`);
}

export function getTokens() {
  const crudo = sessionStorage.getItem(CLAVE_TOKENS);
  return crudo ? JSON.parse(crudo) : null;
}

export function getAccessToken() {
  return getTokens()?.access_token ?? null;
}

function limpiarUrl() {
  window.history.replaceState({}, document.title, window.location.pathname);
}

export async function procesarRetorno() {
  const url = new URL(window.location.href);
  const code = url.searchParams.get('code');
  const state = url.searchParams.get('state');
  const error = url.searchParams.get('error');

  if (error) {
    limpiarUrl();
    throw new Error(`${error}: ${url.searchParams.get('error_description') ?? ''}`);
  }
  if (!code) return null;

  if (state !== sessionStorage.getItem(CLAVE_STATE)) {
    limpiarUrl();
    throw new Error('El parámetro state no coincide.');
  }

  const verifier = sessionStorage.getItem(CLAVE_VERIFIER);
  if (!verifier) {
    limpiarUrl();
    throw new Error('No hay code_verifier en esta pestaña.');
  }

  const respuesta = await fetch(`${CONFIG.cognitoDomain}/oauth2/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: CONFIG.clientId,
      code,
      redirect_uri: CONFIG.redirectUri,
      code_verifier: verifier,
    }),
  });

  const datos = await respuesta.json();
  limpiarUrl();
  sessionStorage.removeItem(CLAVE_VERIFIER);
  sessionStorage.removeItem(CLAVE_STATE);

  if (!respuesta.ok) {
    throw new Error(`/token respondió ${respuesta.status}: ${datos.error ?? 'desconocido'}`);
  }

  sessionStorage.setItem(CLAVE_TOKENS, JSON.stringify(datos));
  return datos;
}

export function decodificarJwt(token) {
  if (!token) return null;
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const bytes = Uint8Array.from(atob(payload), (c) => c.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(bytes));
  } catch {
    return null;
  }
}

export function logout() {
  sessionStorage.removeItem(CLAVE_TOKENS);
  const params = new URLSearchParams({
    client_id: CONFIG.clientId,
    logout_uri: CONFIG.redirectUri,
  });
  window.location.assign(`${CONFIG.cognitoDomain}/logout?${params}`);
}