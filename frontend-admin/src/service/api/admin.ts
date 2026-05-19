import { getToken } from '@/store/modules/auth/shared';

export interface AdminPage<T> {
  items: T[];
  next_cursor: string | null;
  has_more: boolean;
}

export interface Merchant {
  id: string;
  display_name: string;
  credential_ref: string;
  credential_fingerprint: string;
  status: string;
  created_at: string;
  updated_at: string;
}

export interface Channel {
  id: string;
  merchant_id: string;
  display_name: string;
  provider_code: string;
  credential_ref: string;
  credential_fingerprint: string;
  status: string;
  created_at: string;
  updated_at: string;
}

export interface MerchantCreateInput {
  id: string;
  display_name: string;
  credential_ref: string;
}

export interface ChannelCreateInput {
  id: string;
  display_name: string;
  provider_code: string;
  credential_ref: string;
}

export function fetchMerchants() {
  return adminFetch<AdminPage<Merchant>>('/api/admin/merchants');
}

export function fetchMerchant(id: string) {
  return adminFetch<Merchant>(`/api/admin/merchants/${encodeURIComponent(id)}`);
}

export function createMerchant(input: MerchantCreateInput) {
  return adminFetch<Merchant>('/api/admin/merchants', postOptions(input));
}

export function fetchMerchantChannels(merchantId: string) {
  return adminFetch<AdminPage<Channel>>(`/api/admin/merchants/${encodeURIComponent(merchantId)}/channels`);
}

export function createChannel(merchantId: string, input: ChannelCreateInput) {
  return adminFetch<Channel>(`/api/admin/merchants/${encodeURIComponent(merchantId)}/channels`, postOptions(input));
}

export function fetchChannel(id: string) {
  return adminFetch<Channel>(`/api/admin/channels/${encodeURIComponent(id)}`);
}

async function adminFetch<T>(url: string, init: RequestInit = {}) {
  const response = await fetch(url, { ...init, headers: headers(init.headers) });
  if (!response.ok) {
    throw await problem(response);
  }
  return (await response.json()) as T;
}

function postOptions(input: unknown): RequestInit {
  return {
    method: 'POST',
    body: JSON.stringify(input),
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': crypto.randomUUID() }
  };
}

function headers(input?: HeadersInit): HeadersInit {
  const token = getToken();
  return {
    Accept: 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(input ?? {})
  };
}

async function problem(response: Response) {
  const body = await response.json().catch(() => null);
  return new Error(body?.detail || body?.title || `Admin API failed with HTTP ${response.status}`);
}
