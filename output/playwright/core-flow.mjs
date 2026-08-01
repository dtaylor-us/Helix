import playwright from '../../apps/web/node_modules/playwright/index.js';
import fs from 'node:fs/promises';
const { chromium } = playwright;
const root = 'http://localhost:5173';
const out = new URL('./', import.meta.url);
const browser = await chromium.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', headless: true });
const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
const page = await context.newPage();
page.setDefaultTimeout(7000);
const log = { checks: [], events: [], inventories: {} };
page.on('console', m => { if (!['debug','info'].includes(m.type())) log.events.push({type:`console:${m.type()}`,text:m.text()}); });
page.on('requestfailed', r => log.events.push({type:'requestfailed',url:r.url(),failure:r.failure()}));
page.on('response', r => { if(r.status()>=400) log.events.push({type:'http',status:r.status(),url:r.url()}); });
const check = (name, pass, detail='') => log.checks.push({name,pass,detail});
const snap = async name => page.screenshot({path:new URL(name+'.png',out).pathname,fullPage:true});
const inventory = async name => log.inventories[name] = await page.locator('main').evaluate(m => ({text:m.innerText, controls:[...m.querySelectorAll('a,button,input,textarea,select')].map(e=>({tag:e.tagName,text:e.innerText||'',value:e.value||'',placeholder:e.getAttribute('placeholder')||'',disabled:e.disabled}))}));

await page.goto(root+'/transformations', {waitUntil:'networkidle'});
console.log('stage: journey loaded');
const saveTransformation = page.getByRole('button',{name:'Save transformation'});
check('blank transformation blocked', await saveTransformation.isDisabled(), 'Save button is disabled');
check('blank transformation has explanatory validation', /required|enter|provide|title/i.test(await page.locator('main').innerText()), (await page.locator('main').innerText()).slice(-300));
const inputs = page.locator('main input, main textarea');
await inputs.nth(0).fill('Respond to feedback with calm curiosity');
await inputs.nth(1).fill('I want criticism to become useful information instead of a verdict.');
await inputs.nth(2).fill('Someone who listens, pauses, and chooses a thoughtful response.');
await inputs.nth(3).fill('I react quickly when feedback feels personal.');
console.log('stage: transformation filled');
await saveTransformation.click();
await page.waitForTimeout(1000);
console.log('stage: transformation submitted', page.url());
check('transformation saved', (await page.locator('main').innerText()).includes('Respond to feedback with calm curiosity'), page.url());
await snap('transformation-created');
await inventory('transformation-detail');

// Draft should prefill but not persist when abandoned.
const draft = page.getByRole('button',{name:/Draft this for me/i});
check('draft control exists', await draft.count() > 0);
if (await draft.count()) {
  await draft.click(); await page.waitForTimeout(500);
  console.log('stage: draft requested');
  const text = await page.locator('main').innerText();
  check('draft status/provenance visible', /fallback|provider|draft|AI/i.test(text), text.slice(-500));
  await inventory('experiment-drafted');
  await snap('experiment-draft-fallback');
  await page.goto(root+'/today',{waitUntil:'networkidle'});
  check('abandoned draft not persisted', /Welcome to Helix|No active experiment|Start|Begin/i.test(await page.locator('main').innerText()), await page.locator('main').innerText());
  await page.goBack({waitUntil:'networkidle'}); await page.waitForTimeout(300);
}
await inventory('detail-before-manual-experiment');
await fs.writeFile(new URL('core-flow.json',out),JSON.stringify(log,null,2));
console.log(JSON.stringify(log,null,2));
await browser.close();
