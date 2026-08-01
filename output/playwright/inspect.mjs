import playwright from '../../apps/web/node_modules/playwright/index.js';
import fs from 'node:fs/promises';

const { chromium } = playwright;

const out = new URL('./', import.meta.url);
const browser = await chromium.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', headless: true });
const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
const page = await context.newPage();
const events = [];
page.on('console', m => events.push({ type: `console:${m.type()}`, text: m.text() }));
page.on('requestfailed', r => events.push({ type: 'requestfailed', url: r.url(), failure: r.failure() }));
page.on('response', r => { if (r.status() >= 400) events.push({ type: 'http', status: r.status(), url: r.url() }); });
await page.goto('http://localhost:5173/today', { waitUntil: 'networkidle' });
await page.screenshot({ path: new URL('welcome.png', out).pathname, fullPage: true });
const inventory = await page.locator('body').evaluate(body => ({
  text: body.innerText,
  controls: [...body.querySelectorAll('a,button,input,textarea,select')].map((e, i) => ({
    i, tag: e.tagName, text: e.innerText || '', name: e.getAttribute('aria-label') || e.getAttribute('name') || '',
    type: e.getAttribute('type') || '', href: e.getAttribute('href') || '', placeholder: e.getAttribute('placeholder') || ''
  })),
  labels: [...body.querySelectorAll('label')].map(e => e.innerText),
  live: [...body.querySelectorAll('[aria-live]')].map(e => ({ live: e.getAttribute('aria-live'), text: e.innerText }))
}));
await fs.writeFile(new URL('initial-inventory.json', out), JSON.stringify({ inventory, events }, null, 2));
console.log(JSON.stringify({ inventory, events }, null, 2));
await browser.close();
