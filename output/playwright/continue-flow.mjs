import playwright from '../../apps/web/node_modules/playwright/index.js';
import fs from 'node:fs/promises';
const {chromium}=playwright; const root='http://localhost:5173'; const out=new URL('./',import.meta.url);
const browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
const context=await browser.newContext({viewport:{width:1440,height:1000}}); const page=await context.newPage(); page.setDefaultTimeout(8000);
const log={checks:[],events:[],states:{}}; const check=(name,pass,detail='')=>log.checks.push({name,pass,detail});
page.on('console',m=>{if(!['debug','info'].includes(m.type()))log.events.push({type:`console:${m.type()}`,text:m.text()})});
page.on('requestfailed',r=>log.events.push({type:'requestfailed',url:r.url(),failure:r.failure()})); page.on('response',r=>{if(r.status()>=400)log.events.push({type:'http',status:r.status(),url:r.url()})});
const snap=n=>page.screenshot({path:new URL(n+'.png',out).pathname,fullPage:true});
const state=async n=>log.states[n]=await page.locator('main').evaluate(m=>({text:m.innerText,controls:[...m.querySelectorAll('button,input,textarea,select,a')].map(e=>({tag:e.tagName,text:e.innerText||'',value:e.value||'',type:e.type||'',placeholder:e.placeholder||'',disabled:e.disabled,href:e.getAttribute('href')||''}))}));
await page.goto(root+'/transformations',{waitUntil:'networkidle'}); await page.getByRole('link',{name:'Respond to feedback with calm curiosity'}).click(); await page.waitForTimeout(500); await state('detail'); console.log('detail',page.url());
const draft=page.getByRole('button',{name:/Draft this for me/i}); check('draft exists',await draft.count()>0);
if(await draft.count()){await draft.click();await page.waitForTimeout(1000);await state('drafted');await snap('experiment-draft-fallback');const vals=await page.locator('main input,main textarea').evaluateAll(es=>es.map(e=>e.value));check('draft prefills fields',vals.filter(Boolean).length>=3,JSON.stringify(vals));check('fallback provenance visible',/fallback|provider unavailable|AI unavailable/i.test(await page.locator('main').innerText()),await page.locator('main').innerText());await page.goto(root+'/today',{waitUntil:'networkidle'});check('abandoned draft not on Today',!/Pause before responding/.test(await page.locator('main').innerText()),await page.locator('main').innerText());await page.goBack();await page.waitForTimeout(500)}
await state('manual-form');
const fields=page.locator('main input,main textarea,main select'); const count=await fields.count(); console.log('field count',count);
// Locate by associated labels when possible.
const labels=await page.locator('main label').allTextContents(); log.states.labels=labels;
const title=page.getByLabel(/Experiment title|What will you try|Title/i).first(); const hypothesis=page.getByLabel(/hypothesis/i).first(); const action=page.getByLabel(/next action/i).first();
if(await title.count()) await title.fill('Pause before responding to feedback');
if(await hypothesis.count()) await hypothesis.fill('If I take one breath, I can respond with curiosity.');
if(await action.count()) await action.fill('Ask one clarifying question before defending myself.');
await state('manual-filled');
const save=page.getByRole('button',{name:/Save experiment/i}); check('save experiment enabled',await save.count()>0 && await save.isEnabled(),JSON.stringify(labels));
if(await save.count()&&await save.isEnabled()){await save.click();await page.waitForTimeout(1000)}
await state('after-save');await snap('experiment-saved');
await page.goto(root+'/today',{waitUntil:'networkidle'});await page.waitForTimeout(700);await state('today-active');await snap('today-active-experiment');check('active experiment on Today',(await page.locator('main').innerText()).includes('Pause before responding'),await page.locator('main').innerText());
await fs.writeFile(new URL('continue-flow.json',out),JSON.stringify(log,null,2));console.log(JSON.stringify(log,null,2));await browser.close();
