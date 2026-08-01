import playwright from '../../apps/web/node_modules/playwright/index.js';
import fs from 'node:fs/promises';
const { chromium } = playwright;
const root = 'http://localhost:5173';
const out = new URL('./', import.meta.url);
const browser = await chromium.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
page.setDefaultTimeout(45000);
const log = { checks: [], events: [], states: {} };
const check = (name, pass, detail = '') => log.checks.push({ name, pass, detail });
const state = async name => log.states[name] = await page.locator('main').evaluate(m => ({ text: m.innerText, labels: [...m.querySelectorAll('label')].map(e => e.innerText), controls: [...m.querySelectorAll('button,input,textarea,select')].map(e => ({ tag: e.tagName, text: e.innerText || '', value: e.value || '', type: e.type || '', disabled: e.disabled })) }));
const snap = name => page.screenshot({ path: new URL(name + '.png', out).pathname, fullPage: true });
page.on('console', m => { if (!['debug', 'info'].includes(m.type())) log.events.push({ type: `console:${m.type()}`, text: m.text() }); });
page.on('requestfailed', r => log.events.push({ type: 'failed', url: r.url(), failure: r.failure() }));
page.on('response', r => { if (r.status() >= 400) log.events.push({ type: 'http', status: r.status(), url: r.url() }); });
await page.goto(root + '/today', { waitUntil: 'networkidle' });
const box = page.getByLabel('Your message');
await box.fill('I paused, asked what specific change would help, and noticed my shoulders relax.');
await page.getByRole('button', { name: 'Send' }).click();
await page.waitForTimeout(8000);
await state('after-send');
check('AI follow-up appears', log.states['after-send'].text.includes('I paused') && /what|how|notice|surpris|matter|feel/i.test(log.states['after-send'].text), log.states['after-send'].text);
await snap('reflection-conversation');
await box.fill('I was surprised that curiosity made the feedback feel specific rather than personal.');
if (await page.getByRole('button', { name: 'Send' }).isEnabled()) { await page.getByRole('button', { name: 'Send' }).click(); await page.waitForTimeout(7000); }
await state('after-followup');
const done = page.getByRole('button', { name: /done.*review my reflection/i });
check('done enabled', await done.isEnabled(), log.states['after-followup'].text);
if (await done.isEnabled()) {
  await done.click(); await page.waitForTimeout(8000); await state('review'); await snap('reflection-review');
  const save = page.getByRole('button', { name: /Save reflection/i });
  if (await save.count()) { check('structured review offered', true, log.states.review.text); await save.click(); await page.waitForTimeout(10000); await state('saved'); await snap('today-reflection-saved'); }
}
await fs.writeFile(new URL('ai-reflection.json', out), JSON.stringify(log, null, 2));
console.log(JSON.stringify(log, null, 2));
await browser.close();
