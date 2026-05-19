import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '../..');
const files = [
  'backend/mmpay-license-relay/src/main/java/com/imgltd/mmpay/license/LicenseRelay.java',
  'backend/mmpay-license-relay/src/main/java/com/imgltd/mmpay/license/LicenseRelayReceipt.java',
  'backend/mmpay-license-relay/src/main/java/com/imgltd/mmpay/license/RelayConfig.java'
];
const forbidden = [
  /ObjectMapper|JsonNode|Gson|Yaml/,
  /Files\.write|System\.loadLibrary|Runtime\.getRuntime\(\)\.load/,
  /private[_-]?key|license[_-]?sign/i
];

for (const file of files) {
  const source = await readFile(path.join(root, file), 'utf8');
  for (const pattern of forbidden) {
    assert.doesNotMatch(source, pattern, `${file} violates opaque relay boundary`);
  }
}

console.log('license relay static scan passed');
