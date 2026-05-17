import { readFile, readdir } from 'node:fs/promises';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const deniedPatterns = [/type=["']password["']/i, /plaintextCredential/i, /rawSecret/i];

/**
 * @param {string} directory
 * @returns {Promise<string[]>}
 */
async function collectFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = await Promise.all(
    entries.map((entry) => {
      const entryPath = path.join(directory, entry.name);
      return entry.isDirectory() ? collectFiles(entryPath) : [entryPath];
    }));
  return files.flat();
}

const files = await collectFiles(path.join(root, 'src'));
for (const file of files) {
  const content = await readFile(file, 'utf8');
  for (const pattern of deniedPatterns) {
    if (pattern.test(content)) {
      throw new Error(`Plaintext credential field found in ${path.relative(root, file)}`);
    }
  }
}

console.log('credential field lint passed');
