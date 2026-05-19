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

export interface CredentialRefPatch {
  type: 'Set' | 'Unbind';
  value?: string;
}

export interface MerchantPatchInput {
  display_name?: string;
  status?: string;
  credential_ref?: CredentialRefPatch;
}

export interface ChannelPatchInput {
  display_name?: string;
  status?: string;
  credential_ref?: CredentialRefPatch;
}

export interface VerifyBindingResult {
  status: string;
  stored_fingerprint: string;
  resolved_fingerprint: string;
}

export interface PaymentIntent {
  id: string;
  merchant_id: string;
  channel_id: string;
  provider_order_id: string | null;
  amount_minor: number;
  currency: string;
  order_ref: string;
  status: string;
  created_at: string;
  updated_at: string;
}

export interface PaymentIntentCreateInput {
  channel_id: string;
  amount_minor: number;
  currency: string;
  order_ref: string;
}

export interface Refund {
  id: string;
  payment_intent_id: string;
  merchant_id: string;
  amount_minor: number;
  currency: string;
  status: string;
  requested_at: string;
}

export interface RefundCreateInput {
  payment_intent_id: string;
  amount_minor: number;
}

export interface ReconciliationRun {
  id: number;
  run_date: string;
  provider_code: string;
  channel_id: string;
  ingest_count: number;
  matched_count: number;
  unmatched_count: number;
  outcome: string;
  ack_status: string;
  ack_at: string | null;
  ack_actor: string | null;
}

export interface WebhookIntegrationInput {
  id: string;
  display_name: string;
  target_url: string;
  secret_ref: string;
}

export interface BulkRedispatchInput {
  integration_id: string;
  event_count: number;
  rps: number;
}

export interface BulkRedispatchResult {
  integration_id: string;
  event_count: number;
  rps: number;
  estimated_drain_seconds: number;
}

export interface DeliveryLog {
  id: number;
  integration_id: string;
  payment_intent_id: string;
  event_id: string;
  attempt: number;
  scheduled_at: string;
  dispatched_at: string | null;
  response_status: number | null;
  next_retry_at: string | null;
  dead_letter: boolean;
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

export function updateMerchant(id: string, input: MerchantPatchInput) {
  return adminFetch<Merchant>(`/api/admin/merchants/${encodeURIComponent(id)}`, jsonOptions('PATCH', input));
}

export function archiveMerchant(id: string) {
  return adminFetch<void>(`/api/admin/merchants/${encodeURIComponent(id)}`, { method: 'DELETE' });
}

export function verifyMerchantBinding(id: string) {
  return adminFetch<VerifyBindingResult>(`/api/admin/merchants/${encodeURIComponent(id)}/verify-binding`, { method: 'POST' });
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

export function updateChannel(id: string, input: ChannelPatchInput) {
  return adminFetch<Channel>(`/api/admin/channels/${encodeURIComponent(id)}`, jsonOptions('PATCH', input));
}

export function archiveChannel(id: string) {
  return adminFetch<void>(`/api/admin/channels/${encodeURIComponent(id)}`, { method: 'DELETE' });
}

export function verifyChannelBinding(id: string) {
  return adminFetch<VerifyBindingResult>(`/api/admin/channels/${encodeURIComponent(id)}/verify-binding`, { method: 'POST' });
}

export function fetchPaymentIntents() {
  return adminFetch<AdminPage<PaymentIntent>>('/api/admin/payment-intents');
}

export function fetchPaymentIntent(id: string) {
  return adminFetch<PaymentIntent>(`/api/admin/payment-intents/${encodeURIComponent(id)}`);
}

export function createPaymentIntent(input: PaymentIntentCreateInput) {
  return adminFetch<PaymentIntent>('/api/admin/payment-intents', postOptions(input));
}

export function cancelPaymentIntent(id: string) {
  return adminFetch<PaymentIntent>(`/api/admin/payment-intents/${encodeURIComponent(id)}/cancel`, { method: 'POST' });
}

export function fetchRefunds() {
  return adminFetch<AdminPage<Refund>>('/api/admin/refunds');
}

export function fetchRefund(id: string) {
  return adminFetch<Refund>(`/api/admin/refunds/${encodeURIComponent(id)}`);
}

export function createRefund(input: RefundCreateInput) {
  return adminFetch<Refund>('/api/admin/refunds', postOptions(input));
}

export function fetchReconciliationRuns() {
  return adminFetch<AdminPage<ReconciliationRun>>('/api/admin/reconciliation/runs');
}

export function ackReconciliationRun(id: number) {
  return adminFetch<ReconciliationRun>(`/api/admin/reconciliation/runs/${id}/ack`, { method: 'POST' });
}

export function createWebhookIntegration(input: WebhookIntegrationInput) {
  return adminFetch('/api/admin/webhook-out/integrations', postOptions(input));
}

export function bulkRedispatch(input: BulkRedispatchInput) {
  return adminFetch<BulkRedispatchResult>('/api/admin/webhook-out/bulk-redispatch', jsonOptions('POST', input));
}

export function fetchDeliveryLogs() {
  return adminFetch<AdminPage<DeliveryLog>>('/api/admin/webhook-out/delivery-logs');
}

export function fetchDeliveryLog(id: string | number) {
  return adminFetch<DeliveryLog>(`/api/admin/webhook-out/delivery-logs/${id}`);
}

export function redispatchDeliveryLog(id: string | number) {
  return adminFetch<DeliveryLog>(`/api/admin/webhook-out/delivery-logs/${id}/redispatch`, { method: 'POST' });
}

async function adminFetch<T>(url: string, init: RequestInit = {}) {
  const response = await fetch(url, { ...init, headers: headers(init.headers) });
  if (!response.ok) {
    throw await problem(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

function postOptions(input: unknown): RequestInit {
  return jsonOptions('POST', input, { 'Idempotency-Key': crypto.randomUUID() });
}

function jsonOptions(method: string, input: unknown, extraHeaders: HeadersInit = {}): RequestInit {
  return { method, body: JSON.stringify(input), headers: { 'Content-Type': 'application/json', ...extraHeaders } };
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
